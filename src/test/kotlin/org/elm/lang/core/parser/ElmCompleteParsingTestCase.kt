/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 *
 * Originally from IntelliJ-Rust plugin.
 */

package org.elm.lang.core.parser

import com.intellij.psi.PsiFile
import org.junit.Test

class ElmCompleteParsingTestCase : ElmParsingTestCaseBase("complete") {

    @Test
    fun testCaseOf() = doTest(true)

    @Test
    fun testCaseOfComments() = doTest(true)

    @Test
    fun testComments() = doTest(true)

    @Test
    fun testDottedExpressions() = doTest(true)

    @Test
    fun testFieldAccessors() = doTest(true)

    @Test
    fun testImports() = doTest(true)

    @Test
    fun testFieldAccessorFunction() = doTest(true)

    @Test
    fun testFunctionCall() = doTest(true)

    @Test
    fun testFunctionMatchSingularUnion() = doTest(true)

    @Test
    fun testIfElse() = doTest(true)

    @Test
    fun testLetIn() = doTest(true)

    @Test
    fun testLiterals() = doTest(true)

    @Test
    fun testModulesEffect() = doTest(true)

    @Test
    fun testModulesExposingAll() = doTest(true)

    @Test
    fun testModulesExposingSome() = doTest(true)

    @Test
    fun testModulesQualifiedName() = doTest(true)

    @Test
    fun testOperators() = doTest(true)

    @Test
    fun testRecords() = doTest(true)

    @Test
    fun testTuples() = doTest(true)

    @Test
    fun testTypeAlias() = doTest(true)

    @Test
    fun testTypeAnnotation() = doTest(true)

    @Test
    fun testUnionTypeDeclaration() = doTest(true)

    @Test
    fun testValueQID() = doTest(true)

    @Test
    fun testShaders() = doTest(true)


    override fun checkResult(targetDataName: String, file: PsiFile) {
        super.checkResult(targetDataName, file)
        check(!hasError(file)){
            "Error in well formed file ${file.name}"
        }
    }
}
