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

package org.elmPerformanceTests

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.vfs.*
import com.intellij.util.ui.UIUtil
import org.elm.openapiext.fullyRefreshDirectory
import org.elm.workspace.ElmWorkspaceTestBase
import org.elm.workspace.elmWorkspace

abstract class ElmRealProjectTestBase : ElmWorkspaceTestBase() {

    protected fun openRealProject(info: RealProjectInfo): VirtualFile? {
        val base = openRealProject("testData/${info.path}", info.exclude)
        if (base == null) {
            val name = info.name
            println("SKIP $name: git clone ${info.gitUrl} testData/$name")
            return null
        }
        return base
    }

    private fun openRealProject(path: String, exclude: List<String> = emptyList()): VirtualFile? {
        val projectDir = LocalFileSystem.getInstance().refreshAndFindFileByPath(path)
                ?: return null

        fun isAppropriate(file: VirtualFile): Boolean {
            val relativePath = file.path.substring(projectDir.path.length + 1)
            // 1. Ignore excluded files
            if (exclude.any { relativePath.startsWith(it) }) return false
            // 2. Ignore hidden files
            if (file.name.startsWith(".")) return false

            // Otherwise, analyse it
            return true
        }

        runWriteAction {
            fullyRefreshDirectoryInUnitTests(projectDir)
            VfsUtil.copyDirectory(this, projectDir, elmWorkspaceDirectory, ::isAppropriate)
            fullyRefreshDirectoryInUnitTests(elmWorkspaceDirectory)
        }

        project.elmWorkspace.asyncDiscoverAndRefresh()
        UIUtil.dispatchAllInvocationEvents()
        return elmWorkspaceDirectory
    }

    class RealProjectInfo(
            val name: String,
            val path: String,
            val gitUrl: String,
            val exclude: List<String> = emptyList()
    )

    companion object {
        val SPA = RealProjectInfo("elm-spa-example", "elm-spa-example", "https://github.com/rtfeldman/elm-spa-example")
        val ELM_CSS = RealProjectInfo("elm-css", "elm-css", "https://github.com/rtfeldman/elm-css", exclude = listOf("src/DEPRECATED", "tests"))
        val ELM_PHYSICS = RealProjectInfo("elm-physics", "elm-physics", "https://github.com/w0rm/elm-physics")
        val LIST_EXTRA = RealProjectInfo("elm-list-extra", "elm-list-extra", "https://github.com/elm-community/list-extra")
        val JSON_TREE_VIEW = RealProjectInfo("elm-json-tree-view", "elm-json-tree-view", "https://github.com/klazuka/elm-json-tree-view")
        val DEV_TOOLS = RealProjectInfo("elm-dev-tools", "elm-dev-tools", "https://github.com/opvasger/elm-dev-tools")
    }
}

fun VirtualFile.findDescendants(filter: (VirtualFile) -> Boolean): ArrayList<VirtualFile> {
    val result = ArrayList<VirtualFile>()
    VfsUtilCore.visitChildrenRecursively(this,
            object : VirtualFileVisitor<ArrayList<VirtualFile>>() {
                override fun visitFile(file: VirtualFile): Boolean {
                    if (!file.isDirectory && filter(file)) result.add(file)
                    return true
                }
            })
    return result
}

fun fullyRefreshDirectoryInUnitTests(directory: VirtualFile) {
    // It's very weird, but real refresh occurs only if
    // we touch file names. At least in the test environment
    VfsUtilCore.iterateChildrenRecursively(directory, null) { it.name; true }
    fullyRefreshDirectory(directory)
}
