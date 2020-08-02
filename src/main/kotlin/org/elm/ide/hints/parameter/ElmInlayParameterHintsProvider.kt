/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.elm.ide.hints.parameter

import com.intellij.codeInsight.hints.HintInfo
import com.intellij.codeInsight.hints.InlayInfo
import com.intellij.codeInsight.hints.InlayParameterHintsProvider
import com.intellij.codeInsight.hints.Option
import com.intellij.psi.PsiElement
import org.elm.lang.core.ElmLanguage
import org.elm.lang.core.psi.elements.ElmFunctionCallExpr
import org.elm.lang.core.psi.elements.ElmFunctionDeclarationLeft


@Suppress("UnstableApiUsage")
class ElmInlayParameterHintsProvider : InlayParameterHintsProvider {
    override fun getSupportedOptions(): List<Option> =
            listOf(ElmInlayParameterHints.enabledOption, ElmInlayParameterHints.smartOption)

    override fun getDefaultBlackList(): Set<String> = emptySet()

    override fun getHintInfo(element: PsiElement): HintInfo? {
        return when (element) {
            is ElmFunctionCallExpr -> {
                resolve(element)
            }
            else -> {
                null
            }
        }
    }

    @ExperimentalStdlibApi
    override fun getParameterHints(element: PsiElement): List<InlayInfo> {
        if (ElmInlayParameterHints.enabled) {
            return ElmInlayParameterHints.provideHints(element)
        }
        return emptyList()
    }

    override fun getInlayPresentation(inlayText: String): String = inlayText

    companion object {
        private fun resolve(call: ElmFunctionCallExpr): HintInfo.MethodInfo? {
            val fn = (call.target).reference?.resolve() as? ElmFunctionDeclarationLeft ?: return null
            val parameters = fn.namedParameters.map { it.text }
            return createMethodInfo(fn, parameters)
        }

        private fun createMethodInfo(function: ElmFunctionDeclarationLeft, parameters: List<String>): HintInfo.MethodInfo? {
            val path = function.name
            return HintInfo.MethodInfo(path, parameters, ElmLanguage)
        }
    }
}