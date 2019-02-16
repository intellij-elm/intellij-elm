/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 *
 * Originally from IntelliJ-Rust plugin.
 */

package org.elm.lang.core.parser

import com.intellij.psi.PsiFile

/*
Tests parser recovery (`pin` and `recoverWhile` attributes from Elm parser BNF)
by constructing PSI trees from syntactically invalid files.
*/
class ElmPartialParsingTestCase : ElmParsingTestCaseBase("partial") {

    fun testFieldAccessors() = doTest(true)
    fun testIfElse() = doTest(true)
    fun testModuleDecl0() = doTest(true)
    fun testModuleDecl1() = doTest(true)
    fun testModuleDecl2() = doTest(true)
    fun testModuleDecl3() = doTest(true)
    fun testModuleDecl4() = doTest(true)
    fun testImport() = doTest(true)
    fun testValueDecl() = doTest(true)
    fun testTypeDecl() = doTest(true)
    fun testTypeAliasDecl() = doTest(true)
    fun testTypeAnnotations() = doTest(true)
    fun testListLiterals() = doTest(true)
    fun testRecords() = doTest(true)
    fun testTuples() = doTest(true)
    fun testNegateExpression() = doTest(true)
    fun testChars() = doTest(true)
    fun testStrings() = doTest(true)
    fun testOperators() = doTest(true)
    fun testShaders() = doTest(true)

    // The parse error recovery for case/of and let/in expressions is hard to get right
    // due to the parse rules depending on indentation. In a partial program that the
    // user is actively editing, the indentation can be screwed up. So we must test these
    // partial expressions extensively.
    fun testCaseOf() = doTest(true)
    fun testLetIn() = doTest(true)


    override fun checkResult(targetDataName: String, file: PsiFile) {
        check(hasError(file)) {
            "Invalid file was parsed successfully: ${file.name}"
        }
        super.checkResult(targetDataName, file)
    }
}
