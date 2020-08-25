/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 *
 * From intellij-rust
 */

package org.elm.openapiext

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import com.intellij.openapi.util.JDOMUtil
import com.intellij.openapi.util.RecursionManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.ex.temp.TempFileSystem
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StubIndex
import com.intellij.psi.stubs.StubIndexKey
import org.jdom.Element
import org.jdom.input.SAXBuilder
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.atomic.AtomicReference
import kotlin.reflect.KProperty


fun <T> Project.runWriteCommandAction(command: () -> T): T {
    return WriteCommandAction.runWriteCommandAction(this, Computable<T> { command() })
}

val Project.modules: Collection<Module>
    get() = ModuleManager.getInstance(this).modules.toList()


fun <T> recursionGuard(key: Any, block: Computable<T>, memoize: Boolean = true): T? =
        RecursionManager.doPreventingRecursion(key, memoize, block)


fun checkWriteAccessAllowed() {
    check(ApplicationManager.getApplication().isWriteAccessAllowed) {
        "Needs write action"
    }
}

fun checkReadAccessAllowed() {
    check(ApplicationManager.getApplication().isReadAccessAllowed) {
        "Needs read action"
    }
}

fun checkIsEventDispatchThread() {
    check(ApplicationManager.getApplication().isDispatchThread) {
        "Needs to be on the Event Dispatch Thread (EDT)"
    }
}

fun checkIsBackgroundThread() {
    check(!ApplicationManager.getApplication().isDispatchThread) {
        "Needs to be on a background thread"
    }
}

fun fullyRefreshDirectory(directory: VirtualFile) {
    VfsUtil.markDirtyAndRefresh(/* async = */ false, /* recursive = */ true, /* reloadChildren = */ true, directory)
}

fun VirtualFile.findFileBreadthFirst(predicate: (VirtualFile) -> Boolean): VirtualFile? {
    val queue = LinkedList<VirtualFile>().also { it.push(this) }
    while (queue.isNotEmpty()) {
        val candidate = queue.pop()
        if (predicate(candidate)) {
            return candidate
        } else {
            for (child in candidate.children) {
                queue.add(child)
            }
        }
    }
    return null
}

val VirtualFile.pathAsPath: Path get() = Paths.get(path)

fun VirtualFile.toPsiFile(project: Project): PsiFile? =
        PsiManager.getInstance(project).findFile(this)

fun Editor.toPsiFile(project: Project): PsiFile? =
        PsiDocumentManager.getInstance(project).getPsiFile(document)



inline fun <Key, reified Psi : PsiElement> getElements(
        indexKey: StubIndexKey<Key, Psi>,
        key: Key, project: Project,
        scope: GlobalSearchScope?
): Collection<Psi> =
        StubIndex.getElements(indexKey, key, project, scope, Psi::class.java)


fun Element.toXmlString() =
        JDOMUtil.writeElement(this)

fun elementFromXmlString(xml: String): org.jdom.Element =
        SAXBuilder().build(xml.byteInputStream()).rootElement


class CachedVirtualFile(private val url: String?) {
    private val cache = AtomicReference<VirtualFile>()

    operator fun getValue(thisRef: Any?, property: KProperty<*>): VirtualFile? {
        if (url == null) return null
        val cached = cache.get()
        if (cached != null && cached.isValid) return cached
        val file = VirtualFileManager.getInstance().findFileByUrl(url)
        cache.set(file)
        return file
    }
}

/**
 * Unless you are absolutely certain that the file will only ever exist
 * on disk (and not in-memory when running tests), you should use [findFileByPathTestAware]
 * instead.
 */
fun LocalFileSystem.findFileByPath(path: Path): VirtualFile? {
    return findFileByPath(path.toString())
}

/**
 * Attempt to find a [VirtualFile] for [path].
 *
 * If running in unit test mode, try the in-memory VFS first.
 *
 * Background: most of our unit tests run in the "light" mode which uses in-memory VFS
 * (http://www.jetbrains.org/intellij/sdk/docs/basics/testing_plugins/light_and_heavy_tests.html).
 * But some things like `elm/core` and other package dependencies exist in a real filesystem
 * on disk, [LocalFileSystem].
 *
 * Whenever you find yourself calling [LocalFileSystem.findFileByPath], consider using this
 * function instead.
 */
fun findFileByPathTestAware(path: Path): VirtualFile? {
    if (isUnitTestMode) {
        val vFile = TempFileSystem.getInstance().findFileByPath(path)
        if (vFile != null) {
            return vFile
        }
    }

    return LocalFileSystem.getInstance().findFileByPath(path)
}

fun refreshAndFindFileByPathTestAware(path: Path): VirtualFile? {
    if (isUnitTestMode) {
        val vFile = TempFileSystem.getInstance().refreshAndFindFileByPath(path.toString())
        if (vFile != null) {
            return vFile
        }
    }

    return LocalFileSystem.getInstance().refreshAndFindFileByPath(path.toString())
}

val isUnitTestMode: Boolean get() = ApplicationManager.getApplication().isUnitTestMode

fun saveAllDocuments() = FileDocumentManager.getInstance().saveAllDocuments()
