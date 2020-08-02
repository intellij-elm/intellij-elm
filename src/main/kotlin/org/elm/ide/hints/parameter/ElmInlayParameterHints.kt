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
        val hints = elements.map {
            if (it is ElmPattern ) {
                it.unwrapped
            } else {
                it
            }
        }.zip(valueArgumentList.arguments.toList())

        return hints
                .flatMap { (param, arg) ->
                    val asText = ( param.parent as? ElmPattern )?.patternAs?.text
                    if (asText != null) {
                        return listOf(InlayInfo("$asText:", arg.startOffset))
                    }
                    when (param) {
                        is ElmAnythingPattern -> emptyList()
                        is ElmTuplePattern -> {
                            if (arg is ElmTupleExpr) {
                                param.patternList.zip(arg.expressionList).map { (tupleParam, tupleArg) ->
                                    InlayInfo(tupleParam.text + ":", tupleArg.startOffset)
                                }

                            } else {
                                emptyList()
                            }
                        }
                        is ElmUnionPattern -> {
                                listOf(InlayInfo(param.upperCaseQID.fullName + ":", arg.startOffset))
                        }
                        else -> {
                            listOf(InlayInfo(param.text + ":", arg.startOffset))
                        }
                    }
                }
                .filterNotNull()
    }
}