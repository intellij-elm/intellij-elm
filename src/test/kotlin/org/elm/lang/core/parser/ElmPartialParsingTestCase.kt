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

    fun testValueDecl() = doTest(true)
    fun testTypeDecl() = doTest(true)
    fun testTypeAliasDecl() = doTest(true)
    fun testTypeAnnotations() = doTest(true)

    override fun checkResult(targetDataName: String?, file: PsiFile) {
        check(hasError(file)) {
            "Invalid file was parsed successfully: ${file.name}"
        }
        super.checkResult(targetDataName, file)
    }
}
