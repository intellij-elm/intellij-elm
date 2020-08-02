/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.elm.ide.hints.parameter

import com.intellij.codeInsight.hints.InlayInfo
import com.intellij.codeInsight.hints.Option
import com.intellij.psi.PsiElement
import org.elm.lang.core.psi.elements.*
import org.elm.lang.core.psi.startOffset

@Suppress("UnstableApiUsage")
object ElmInlayParameterHints {
    // BACKCOMPAT: 2020.1
    @Suppress("DEPRECATION")
    val enabledOption: Option = Option("SHOW_PARAMETER_HINT", "Show argument name hints", true)
    val enabled: Boolean get() = enabledOption.get()

    // BACKCOMPAT: 2020.1
    @Suppress("DEPRECATION")
    val smartOption: Option = Option("SMART_HINTS", "Show only smart hints", true)
    val smart: Boolean get() = smartOption.get()

    @ExperimentalStdlibApi
    fun provideHints(elem: PsiElement): List<InlayInfo> {
        val (callInfo, valueArgumentList) = when (elem) {
            is ElmFunctionCallExpr -> ( elem.target.reference?.resolve() to elem)
            else -> return emptyList()
        }
        if (callInfo == null) return emptyList()
        val elements =
                if (callInfo is ElmFunctionDeclarationLeft) {
                    callInfo.patterns.toList()
                } else {
                    emptyList()
                }
        val hints = elements.zip(valueArgumentList.arguments.toList())

        return hints
                .map { (param, arg) ->
                    val asText = ( param as? ElmPattern )?.patternAs?.text
                    if (asText != null) {
                        return@map InlayInfo("$asText:", arg.startOffset)
                    }
                    when (param) {
                        is ElmRecordPattern -> null
                        is ElmAnythingPattern -> null
                        is ElmPattern -> {
                            val child = param.unwrapped
                            if (child is ElmUnionPattern) {
                                InlayInfo(child.upperCaseQID.fullName + ":", arg.startOffset)
                            } else {
                                InlayInfo(param.text + ":", arg.startOffset)
                            }
                        }
                        else -> {
                            InlayInfo(param.text + ":", arg.startOffset)
                        }
                    }
                }
                .filterNotNull()
    }
}