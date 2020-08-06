/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.elm.ide.hints.parameter

import com.intellij.codeInsight.hints.InlayInfo
import com.intellij.codeInsight.hints.Option
import com.intellij.psi.PsiElement
import org.elm.lang.core.psi.*
import org.elm.lang.core.psi.elements.*
import org.elm.lang.core.types.TyUnknown
import org.elm.lang.core.types.findTy
import org.elm.lang.core.types.renderedText
import org.elm.utils.getIndent

@Suppress("UnstableApiUsage")
object ElmInlayParameterHints {
    // BACKCOMPAT: 2020.1
    @Suppress("DEPRECATION")
    val enabledOption: Option = Option("SHOW_PARAMETER_HINT", "Show argument name hints", true)
    val enabled: Boolean get() = enabledOption.get()

    @ExperimentalStdlibApi
    fun provideHints(elem: PsiElement): List<InlayInfo> {
        return when (elem) {
            is ElmNameDeclarationPatternTag -> {
                listOf(InlayInfo(": " + elem.findTy()?.renderedText(), elem.endOffset))
            }
            is ElmValueDeclaration -> {
                return if (elem.typeAnnotation == null) {
                    val findTy = elem.findTy()
                    if (findTy is TyUnknown || findTy == null) {
                        emptyList()
                    } else {
                        listOf(InlayInfo(" -- " + findTy.renderedText(), elem.eqElement?.endOffset!!))
                    }
                } else {
                    emptyList()
                }
            }
            else -> {
                emptyList()
            }
        }
    }
}
