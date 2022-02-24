package org.elm.ide.folding

import org.elm.lang.ElmTestBase
import org.junit.Test

class ElmFoldingBuilderTest: ElmTestBase() {
    override val dataPath = "org/elm/ide/folding/fixtures"
    private fun doTest() = myFixture.testFolding("$testDataPath/${fileName.trimStart('_')}")

    @Test
    fun `test imports`() = doTest()
    @Test
    fun `test module`() = doTest()
    @Test
    fun `test doc comment`() = doTest()
    @Test
    fun `test record`() = doTest()
    @Test
    fun `test record_type`() = doTest()
    @Test
    fun `test value declaration`() = doTest()
    @Test
    fun `test type declaration`() = doTest()
    @Test
    fun `test type alias`() = doTest()
    @Test
    fun `test let in`() = doTest()
    @Test
    fun `test case of`() = doTest()

    // Partial input test. It's ok if this doesn't produce any folds. We just want to
    // make sure that it doesn't throw any exceptions.
    @Test
    fun `test incomplete module decl`() = doTest()
}
