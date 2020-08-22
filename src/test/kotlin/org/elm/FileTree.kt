/*
The MIT License (MIT)

Derived from intellij-rust
Copyright (c) 2015 Aleksey Kladov, Evgeny Kurbatsky, Alexey Kudinkin and contributors
Copyright (c) 2016 JetBrains

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */


package org.elm

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import org.elm.lang.core.psi.parentOfType
import org.elm.lang.core.resolve.ElmReferenceElement
import org.elm.openapiext.fullyRefreshDirectory
import org.elm.workspace.ElmPackageProject
import org.intellij.lang.annotations.Language


fun fileTree(builder: FileTreeBuilder.() -> Unit): FileTree {
    return FileTree(FileTreeBuilderImpl().apply { builder() }.intoDirectory())
}

fun fileTreeFromText(@Language("Elm") text: String): FileTree {
    val fileSeparator = """^--@ (\S+)\s*$""".toRegex(RegexOption.MULTILINE)
    val fileNames = fileSeparator.findAll(text).map { it.groupValues[1] }.toList()
    val fileTexts = fileSeparator.split(text).filter(String::isNotBlank).map { it.trimIndent() }

    check(fileNames.size == fileTexts.size) {
        "Have you placed `--@ filename.elm` markers?"
    }

    fun fill(dir: Entry.Directory, path: List<String>, contents: String) {
        val name = path.first()
        if (path.size == 1) {
            dir.children[name] = Entry.File(contents)
        } else {
            val childDir = dir.children.getOrPut(name, { Entry.Directory(mutableMapOf()) }) as Entry.Directory
            fill(childDir, path.drop(1), contents)
        }
    }

    return FileTree(Entry.Directory(mutableMapOf()).apply {
        for ((path, contents) in fileNames.map { it.split("/") }.zip(fileTexts)) {
            fill(this, path, contents)
        }
    })
}

interface FileTreeBuilder {
    /* Creates a directory */
    fun dir(name: String, builder: FileTreeBuilder.() -> Unit)

    /* Creates a plain files */
    fun file(name: String, code: String)

    /* Creates an Elm source file */
    fun elm(name: String, @Language("Elm") code: String) = file(name, code)

    /* Creates an Elm source file where the content doesn't matter but it does compile */
    fun elm(name: String) = file(name, """
        module ${name.removeSuffix(".elm")} exposing (..)
        placeholderValue = 0
    """.trimIndent())

    /* Creates an `elm.json` project file */
    fun project(name: String, @Language("JSON") code: String) = file(name, code)
}

class FileTree(private val rootDirectory: Entry.Directory) {
    fun create(project: Project, directory: VirtualFile): TestProject {
        val filesWithCaret: MutableList<String> = mutableListOf()

        fun go(dir: Entry.Directory, root: VirtualFile, parentComponents: List<String> = emptyList()) {
            for ((name, entry) in dir.children) {
                val components = parentComponents + name
                when (entry) {
                    is Entry.File -> {
                        val vFile = root.findChild(name) ?: root.createChildData(root, name)
                        VfsUtil.saveText(vFile, replaceCaretMarker(entry.text))
                        if (hasCaretMarker(entry.text) || "--^" in entry.text) {
                            filesWithCaret += components.joinToString(separator = "/")
                        }
                    }
                    is Entry.Directory -> {
                        go(entry, root.createChildDirectory(root, name), components)
                    }
                }
            }
        }

        runWriteAction {
            go(rootDirectory, directory)
            fullyRefreshDirectory(directory)
        }

        return TestProject(project, directory, filesWithCaret)
    }

    fun assertEquals(baseDir: VirtualFile) {
        fun go(expected: Entry.Directory, actual: VirtualFile) {
            val actualChildren = actual.children.associateBy { it.name }
            check(expected.children.keys == actualChildren.keys) {
                "Mismatch in directory ${actual.path}\n" +
                        "Expected: ${expected.children.keys}\n" +
                        "Actual  : ${actualChildren.keys}"
            }

            for ((name, entry) in expected.children) {
                val a = actualChildren[name]!!
                when (entry) {
                    is Entry.File -> {
                        check(!a.isDirectory)
                        val actualText = String(a.contentsToByteArray(), Charsets.UTF_8)
                        check(entry.text == actualText) {
                            "Expected:\n${entry.text}\nGot:\n$actualText"
                        }
                    }
                    is Entry.Directory -> go(entry, a)
                }
            }
        }

        go(rootDirectory, baseDir)
    }
}

class TestProject(
        private val project: Project,
        val root: VirtualFile,
        val filesWithCaret: List<String>
) {

    val fileWithCaret: String get() = filesWithCaret.singleOrNull()!!

    inline fun <reified T : PsiElement> findElementInFile(path: String): T {
        val element = doFindElementInFile(path)
        return element.parentOfType<T>()
                ?: error("No parent of type ${T::class.java} for ${element.text}")
    }

    inline fun <reified T : ElmReferenceElement> checkReferenceIsResolved(
            path: String,
            shouldNotResolve: Boolean = false,
            toPackage: String? = null
    ) {
        val ref = findElementInFile<T>(path)
        val res = ref.reference.resolve()
        if (shouldNotResolve) {
            check(res == null) {
                "Reference ${ref.text} should be unresolved in `$path`"
            }
        } else {
            check(res != null) {
                "Failed to resolve the reference `${ref.text}` in `$path`."
            }
            if (toPackage != null) {
                val pkgProject = res.elmProject as? ElmPackageProject
                val pkg = pkgProject?.let { "${it.name} ${it.version}" } ?: "<package not found>"
                check(pkg == toPackage) {
                    "Expected to be resolved to $toPackage but actually resolved to $pkg"
                }
            }
        }
    }

    fun doFindElementInFile(path: String): PsiElement {
        val vFile = root.findFileByRelativePath(path)
                ?: error("No `$path` file in test project")
        val file = PsiManager.getInstance(project).findFile(vFile)!!
        return findElementInFile(file, "--^")
    }

    fun psiFile(path: String): PsiFileSystemItem {
        val vFile = root.findFileByRelativePath(path)
                ?: error("Can't find `$path`")
        val psiManager = PsiManager.getInstance(project)
        return if (vFile.isDirectory) psiManager.findDirectory(vFile)!! else psiManager.findFile(vFile)!!
    }
}


private class FileTreeBuilderImpl(val directory: MutableMap<String, Entry> = mutableMapOf()) : FileTreeBuilder {
    override fun dir(name: String, builder: FileTreeBuilder.() -> Unit) {
        check('/' !in name) { "Bad directory name `$name`" }
        directory[name] = FileTreeBuilderImpl().apply { builder() }.intoDirectory()
    }

    override fun file(name: String, code: String) {
        check('/' !in name && '.' in name) { "Bad file name `$name`" }
        directory[name] = Entry.File(code.trimIndent())
    }

    fun intoDirectory() = Entry.Directory(directory)
}

sealed class Entry {
    class File(val text: String) : Entry()
    class Directory(val children: MutableMap<String, Entry>) : Entry()
}

private fun findElementInFile(file: PsiFile, marker: String): PsiElement {
    val markerOffset = file.text.indexOf(marker)
    check(markerOffset != -1) { "No `$marker` in \n${file.text}" }

    val doc = PsiDocumentManager.getInstance(file.project).getDocument(file)!!
    val markerLine = doc.getLineNumber(markerOffset)
    val makerColumn = markerOffset - doc.getLineStartOffset(markerLine)
    val elementOffset = doc.getLineStartOffset(markerLine - 1) + makerColumn

    return file.findElementAt(elementOffset) ?:
            error { "No element found, offset = $elementOffset" }
}

fun replaceCaretMarker(text: String): String = text.replace("{-caret-}", "<caret>")
fun hasCaretMarker(text: String): Boolean = text.contains("{-caret-}")
