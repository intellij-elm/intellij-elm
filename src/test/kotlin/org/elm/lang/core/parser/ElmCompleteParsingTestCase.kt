/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 *
 * Originally from IntelliJ-Rust plugin.
 */

package org.elm.lang.core.parser

import com.intellij.psi.PsiFile

class ElmCompleteParsingTestCase : ElmParsingTestCaseBase("complete") {

    fun testComments() = doTest(true)
    fun testImports() = doTest(true)
    fun testLiterals() = doTest(true)
    fun testModulesExposingAll() = doTest(true)
    fun testModulesExposingSome() = doTest(true)
    fun testModulesQualifiedName() = doTest(true)

    override fun checkResult(targetDataName: String?, file: PsiFile?) {
        super.checkResult(targetDataName, file)
        check(!hasError(file!!)){
            "Error in well formed file ${file.name}"
        }
    }

}
