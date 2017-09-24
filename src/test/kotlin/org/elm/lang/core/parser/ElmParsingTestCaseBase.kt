/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 *
 * Originally from IntelliJ-Rust plugin.
 */

package org.elm.lang.core.parser

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiFile
import com.intellij.testFramework.ParsingTestCase
import org.elm.lang.ElmTestCase
import org.elm.lang.core.ElmFileType
import org.jetbrains.annotations.NonNls


val relativeFixtures = "org/elm/lang/core/parser/fixtures/"

abstract class ElmParsingTestCaseBase(@NonNls dataPath: String)
    : ParsingTestCase(relativeFixtures + dataPath, ElmFileType.EXTENSION, false, ElmParserDefinition())
    , ElmTestCase {

    override fun getTestDataPath() =
            ElmTestCase.resourcesPath

    protected fun hasError(file: PsiFile): Boolean {
        var hasErrors = false
        file.accept(object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement?) {
                if (element is PsiErrorElement) {
                    hasErrors = true
                    return
                }
                element!!.acceptChildren(this)
            }
        })
        return hasErrors
    }

    /* TODO [kl] why was the IntelliJ Rust plugin registering their BraceMatcher
     * as an explicit extension in the `setUp` method.
     */
}
