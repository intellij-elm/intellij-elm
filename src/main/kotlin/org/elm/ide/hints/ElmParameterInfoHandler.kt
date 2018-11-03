package org.elm.ide.hints

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.lang.parameterInfo.*
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.elm.lang.core.psi.ElmTypes.RIGHT_PARENTHESIS
import org.elm.lang.core.psi.ElmTypes.VIRTUAL_END_DECL
import org.elm.lang.core.psi.ancestorsStrict
import org.elm.lang.core.psi.elementType
import org.elm.lang.core.psi.elements.CallInfo
import org.elm.lang.core.psi.elements.ElmFunctionCall
import org.elm.lang.core.types.Ty
import org.elm.lang.core.types.TyFunction
import org.elm.lang.core.types.renderedText

private val log = logger<ElmParameterInfoHandler>()

/**
 * Provide information about the parameters expected by a function at a call-site.
 * In a normal programming language, this would just show the parameter names/types
 * and highlight the parameter which the caret currently corresponds to. But that
 * doesn't work well with ML-style languages where functions are partially applied,
 * application order can be inverted, and where there are no delimiting parentheses
 * around the function's arguments.
 *
 * So instead we will just show the function call target's type annotation.
 */
class ElmParameterInfoHandler : ParameterInfoHandler<PsiElement, ElmParametersDescription> {

    var hintText: String = ""

    override fun couldShowInLookup() = false

    override fun getParametersForLookup(item: LookupElement?, context: ParameterInfoContext?) =
    // TODO maybe we should implement this. I'm not sure what it does, though.
            null

    override fun findElementForParameterInfo(context: CreateParameterInfoContext): PsiElement? {
        val caretElement = context.file.findElementAt(context.editor.caretModel.offset) ?: return null
        val element = findFuncCall(caretElement)
        log.debug("findElementForParameterInfo() caret on $caretElement returning $element")
        return element
    }

    override fun findElementForUpdatingParameterInfo(context: UpdateParameterInfoContext): PsiElement? {
        val caretElement = context.file.findElementAt(context.editor.caretModel.offset) ?: return null
        return findFuncCall(caretElement)
    }

    private fun findFuncCall(caretElement: PsiElement): ElmFunctionCall? {
        val element = when (caretElement.elementType) {
            VIRTUAL_END_DECL, RIGHT_PARENTHESIS -> PsiTreeUtil.prevVisibleLeaf(caretElement) ?: return null
            else -> caretElement
        }

        val ancestorsStrict = element.ancestorsStrict
        log.debug("findFuncCall for $element (${element.text}) ancestorsStrict=${ancestorsStrict.toList()}")
        return ancestorsStrict.filterIsInstance<ElmFunctionCall>().firstOrNull()
    }

    // receives the element as returned by findElementForParameterInfo
    override fun showParameterInfo(element: PsiElement, context: CreateParameterInfoContext) {
        if (element !is ElmFunctionCall) return

        val paramsDescription = ElmParametersDescription.fromCall(element)
                ?: return

        // Each "item" in `itemsToShow` is a function overload set. Elm does not support function overloading,
        // so this array will never be larger than length 1.
        context.itemsToShow = arrayOf(paramsDescription)

        context.showHint(element, element.textRange.startOffset, this)
    }

    override fun updateParameterInfo(parameterOwner: PsiElement, context: UpdateParameterInfoContext) {
        // normally you would call context.setCurrentParameter() here, but we are not going
        // to try to highlight the current parameter
    }

    override fun updateUI(p: ElmParametersDescription?, context: ParameterInfoUIContext) {
        if (p == null) {
            context.isUIComponentEnabled = false
            return
        }

        hintText = p.presentText
        val range = p.rangeToHighlight

        context.setupUIComponentPresentation(
                hintText,
                range.startOffset,
                range.endOffset,
                !context.isUIComponentEnabled,
                false,
                false,
                context.defaultParameterColor
        )
    }
}

class ElmParametersDescription(val callInfo: CallInfo) {
    val presentText: String
        get() {
            val signature = (callInfo.parameters.map { it.ty } + callInfo.returnType)
                    .joinToString(" → ") { it.pretty() }
            return "${callInfo.functionName} : $signature"
        }
    val rangeToHighlight: TextRange
        get() = TextRange(0, callInfo.functionName.length)

    companion object {
        fun fromCall(functionCall: ElmFunctionCall): ElmParametersDescription? {
            val info = functionCall.resolveCallInfo() ?: return null
            return ElmParametersDescription(info)
        }
    }
}

private fun Ty.pretty(): String {
    val s = this.renderedText(linkify = false, withModule = false)
    return when (this) {
        is TyFunction -> "($s)"
        else -> s
    }
}