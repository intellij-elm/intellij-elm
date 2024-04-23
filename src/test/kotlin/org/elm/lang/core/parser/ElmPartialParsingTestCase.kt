/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 *
 * Originally from IntelliJ-Rust plugin.
 */

package org.elm.lang.core.parser

import com.intellij.psi.PsiFile
import org.junit.Test

/*
Tests parser recovery (`pin` and `recoverWhile` attributes from Elm parser BNF)
by constructing PSI trees from syntactically invalid files.
*/
class ElmPartialParsingTestCase : ElmParsingTestCaseBase("partial") {

    @Test
    fun testFieldAccessors() = doTest(true)

    @Test
    fun testIfElse() = doTest(true)

    @Test
    fun testModuleDecl0() = doTest(true)

    @Test
    fun testModuleDecl1() = doTest(true)

    @Test
    fun testModuleDecl2() = doTest(true)

    @Test
    fun testModuleDecl3() = doTest(true)

    @Test
    fun testModuleDecl4() = doTest(true)

    @Test
    fun testImport() = doTest(true)

    @Test
    fun testValueDecl() = doTest(true)

    @Test
    fun testTypeDecl() = doTest(true)

    @Test
    fun testTypeAliasDecl() = doTest(true)

    @Test
    fun testTypeAnnotations() = doTest(true)

    @Test
    fun testListLiterals() = doTest(true)

    @Test
    fun testRecords() = doTest(true)

    @Test
    fun testTuples() = doTest(true)

    @Test
    fun testNegateExpression() = doTest(true)

    @Test
    fun testChars() = doTest(true)

    @Test
    fun testStrings() = doTest(true)

    @Test
    fun testOperators() = doTest(true)

    @Test
    fun testShaders() = doTest(true)

    // The parse error recovery for case/of and let/in expressions is hard to get right
    // due to the parse rules depending on indentation. In a partial program that the
    // user is actively editing, the indentation can be screwed up. So we must test these
    // partial expressions extensively.
    @Test
    fun testCaseOf() = doTest(true)


    @Test
    fun testLetIn() = doTest(true)


    override fun checkResult(targetDataName: String, file: PsiFile) {
        check(hasError(file)) {
            "Invalid file was parsed successfully: ${file.name}"
        }
        super.checkResult(targetDataName, file)
    }
}
