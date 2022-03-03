/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 *
 * Originally from IntelliJ-Rust plugin.
 */

package org.elm.lang.core.parser

import com.intellij.psi.PsiFile

class ElmCompleteParsingTestCase : ElmParsingTestCaseBase("complete") {

    fun testCaseOf() = doTest(true)
    fun testCaseOfComments() = doTest(true)
    fun testComments() = doTest(true)
    fun testDottedExpressions() = doTest(true)
    fun testFieldAccessors() = doTest(true)
    fun testImports() = doTest(true)
    fun testFieldAccessorFunction() = doTest(true)
    fun testFunctionCall() = doTest(true)
    fun testIfElse() = doTest(true)
    fun testLetIn() = doTest(true)
    fun testLiterals() = doTest(true)
    fun testModulesEffect() = doTest(true)
    fun testModulesExposingAll() = doTest(true)
    fun testModulesExposingSome() = doTest(true)
    fun testModulesQualifiedName() = doTest(true)
    fun testOperators() = doTest(true)
    fun testRecords() = doTest(true)
    fun testTuples() = doTest(true)
    fun testTypeAlias() = doTest(true)
    fun testTypeAnnotation() = doTest(true)
    fun testUnionTypeDeclaration() = doTest(true)
    fun testValueQID() = doTest(true)
    fun testShaders() = doTest(true)


    override fun checkResult(targetDataName: String, file: PsiFile) {
        super.checkResult(targetDataName, file)
        check(!hasError(file)){
            "Error in well formed file ${file.name}"
        }
    }
}
