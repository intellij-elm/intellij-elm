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

    fun testIfElse() = doTest(true)
    fun testValueDecl() = doTest(true)
    fun testTypeDecl() = doTest(true)
    fun testTypeAliasDecl() = doTest(true)
    fun testTypeAnnotations() = doTest(true)

    // the case-of and let-in tests are broken. The parse error recovery for these
    // types of expressions works in most cases, but doesn't work in really malformed
    // source texts.
    // TODO [kl] level-up your GrammarKit-fu and improve the parse error recovery
    //    fun testCaseOf() = doTest(true)
    //    fun testLetIn() = doTest(true)


    override fun checkResult(targetDataName: String?, file: PsiFile) {
        check(hasError(file)) {
            "Invalid file was parsed successfully: ${file.name}"
        }
        super.checkResult(targetDataName, file)
    }
}
