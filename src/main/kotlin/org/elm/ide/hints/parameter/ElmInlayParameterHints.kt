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

        return buildHints(hints)
    }

    private fun buildHints(hints: List<Pair<ElmFunctionParamOrPatternChildTag, ElmExpressionTag>>): List<InlayInfo> {
        return hints
                .flatMap { (param, arg) ->
                    val unwrapped = if (param is ElmPattern) {
                        param.unwrapped
                    } else {
                        param
                    }

                    val asText = (param as? ElmPattern)?.patternAs?.text
                    if (asText != null) {
                        return@flatMap listOf(InlayInfo("$asText:", arg.startOffset))
                    }
                    when (unwrapped) {
                        is ElmAnythingPattern -> emptyList()
                        is ElmRecordPattern -> emptyList()
                        is ElmTuplePattern -> {
                            if (arg is ElmTupleExpr) {
                                buildHints(unwrapped.patternList.zip(arg.expressionList))
                            } else {
                                emptyList()
                            }
                        }
                        is ElmUnitExpr -> {
                            emptyList()
                        }
                        is ElmUnionPattern -> {
//                                listOf(InlayInfo(unwrapped.upperCaseQID.fullName + ":", arg.startOffset))
                            emptyList()
                        }
                        else -> {
                            listOf(InlayInfo(unwrapped.text + ":", arg.startOffset))
                        }
                    }
                }
                .filterNotNull()
    }
}