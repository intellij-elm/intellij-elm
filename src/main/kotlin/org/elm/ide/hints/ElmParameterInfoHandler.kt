package org.elm.ide.hints

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.lang.parameterInfo.*
import com.intellij.openapi.diagnostic.logger
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.elm.lang.core.psi.ElmTypes.RIGHT_PARENTHESIS
import org.elm.lang.core.psi.ElmTypes.VIRTUAL_END_DECL
import org.elm.lang.core.psi.ancestorsStrict
import org.elm.lang.core.psi.elementType
import org.elm.lang.core.psi.elements.CallInfo
import org.elm.lang.core.psi.elements.ElmFunctionCall
import org.elm.lang.core.types.renderedText

private val log = logger<ElmParameterInfoHandler>()

class ElmParameterInfoHandler : ParameterInfoHandler<PsiElement, ElmParametersDescription> {

    var hintText: String = ""

    override fun couldShowInLookup() = false

    override fun getParametersForLookup(item: LookupElement?, context: ParameterInfoContext?) =
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

        context.setupUIComponentPresentation(
                hintText,
                0, // no highlighting
                0,
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
                    .joinToString(" → ") { it.renderedText(linkify = false, withModule = false) }
            return "${callInfo.functionName} : $signature"
        }

    companion object {
        fun fromCall(functionCall: ElmFunctionCall): ElmParametersDescription? {
            val info = functionCall.resolveCallInfo() ?: return null
            return ElmParametersDescription(info)
        }
    }
}
