package org.elm.ide.folding

import org.elm.lang.ElmTestBase

class ElmFoldingBuilderTest: ElmTestBase() {
    override val dataPath = "org/elm/ide/folding/fixtures"
    private fun doTest() = myFixture.testFolding("$testDataPath/${fileName.trimStart('_')}")

    fun `test imports`() = doTest()
    fun `test module`() = doTest()
    fun `test doc comment`() = doTest()
    fun `test record`() = doTest()
    fun `test record_type`() = doTest()
    fun `test value declaration`() = doTest()
    fun `test type declaration`() = doTest()
    fun `test type alias`() = doTest()
    fun `test let in`() = doTest()
    fun `test case of`() = doTest()
}
