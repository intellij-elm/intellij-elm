/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 *
 * Originally from intellij-rust
 */

package org.elm.workspace

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.builders.ModuleFixtureBuilder
import com.intellij.testFramework.fixtures.CodeInsightFixtureTestCase
import org.elm.FileTree
import org.elm.TestProject
import java.util.concurrent.CompletableFuture

/**
 * Base class for "heavy" integration tests such as those that depend on the Elm toolchain
 * or read Elm project files in order to create a "workspace".
 *
 * For normal tests, see [org.elm.lang.ElmTestBase]
 */
abstract class ElmWorkspaceTestBase : CodeInsightFixtureTestCase<ModuleFixtureBuilder<*>>() {


    private var toolchain: ElmToolchain? = null
    private var originalToolchain: ElmToolchain? = null


    protected val elmWorkspaceDirectory: VirtualFile
        get() = myFixture.findFileInTempDir(".")

    protected fun FileTree.asyncCreateWithAutoDiscover(): CompletableFuture<TestProject> {
        val testProject = create(project, elmWorkspaceDirectory)
        return project.elmWorkspace.asyncDiscoverAndRefresh().thenApply { testProject }
    }


    override fun setUp() {
        super.setUp()
        originalToolchain = project.elmToolchain
        toolchain = ElmToolchain.suggest(project)
        if (toolchain != null) {
            project.elmWorkspace.useToolchain(toolchain)
        }
    }


    override fun runTest() {
        if (toolchain == null) {
            System.err.println("SKIP $name: no Elm toolchain found")
            return
        }
        super.runTest()
    }


    override fun tearDown() {
        project.elmWorkspace.useToolchain(originalToolchain)
        super.tearDown()
    }


    fun checkEquals(expected: Any, actual: Any) {
        if (expected != actual)
            failure(expected.toString(), actual.toString())
    }


    fun failure(expected: String, actual: String): AssertionError {
        // IntelliJ will handle this output specially by showing a diff.
        throw AssertionError("\nExpected: $expected\n     but: was $actual")
    }
}