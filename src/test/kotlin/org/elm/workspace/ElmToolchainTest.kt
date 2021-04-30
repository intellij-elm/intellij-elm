package org.elm.workspace

import org.elm.lang.ElmTestBase
import org.junit.Test
import java.nio.file.Paths
import kotlin.test.assertFalse


class ElmToolchainTest : ElmTestBase() {
    fun `test allows an executable file`() {
        val rawElmPath = Paths.get(ClassLoader.getSystemResource("org/elm/workspace/fixtures/elm-executable").toURI())
        val toolchain = ElmToolchain(rawElmPath, null, null, false)
        assertTrue(toolchain.looksLikeValidToolchain(project))
    }

    fun `test does not allow an empty path`() {
        val toolchain = ElmToolchain(null, null, null, false)
        assertFalse(toolchain.looksLikeValidToolchain(project))
    }

    fun `test does not allow a non-executable file`() {
        val rawElmPath = Paths.get(ClassLoader.getSystemResource("org/elm/workspace/fixtures/non-executable").toURI())
        val toolchain = ElmToolchain(rawElmPath, null, null, false)
        assertFalse(toolchain.looksLikeValidToolchain(project))
    }
}
