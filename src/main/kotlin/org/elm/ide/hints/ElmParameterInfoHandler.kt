package org.elm.ide.hints

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.lang.parameterInfo.*
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.elm.lang.core.psi.ancestorsStrict
import org.elm.lang.core.psi.elements.ElmFunctionCall
import org.elm.lang.core.psi.elements.ElmFunctionDeclarationLeft
import org.elm.lang.core.psi.elements.ElmValueExpr

class ElmParameterInfoHandler : ParameterInfoHandler<PsiElement, ElmParametersDescription> {

    var hintText: String = ""

    override fun couldShowInLookup() = true

    override fun getParametersForLookup(item: LookupElement?, context: ParameterInfoContext?) =
            null

    override fun findElementForParameterInfo(context: CreateParameterInfoContext): PsiElement? {
        val caretElement = context.file.findElementAt(context.editor.caretModel.offset) ?: return null
        val element = findFuncCall(caretElement)
        println("findElementForParameterInfo() caret on $caretElement returning $element")
        return element
    }

    override fun findElementForUpdatingParameterInfo(context: UpdateParameterInfoContext): PsiElement? {
        val caretElement = context.file.findElementAt(context.editor.caretModel.offset) ?: return null
        val element = findFuncCall(caretElement)
        println("findElementForUpdatingParameterInfo() caret on $caretElement returning $element")
        return element
    }

    private fun findFuncCall(element: PsiElement): ElmFunctionCall? {
        val ancestorsStrict = element.ancestorsStrict
        println("findFuncCall for $element (${element.text}) ancestorsStrict=${ancestorsStrict.toList()}")
        return ancestorsStrict.filterIsInstance<ElmFunctionCall>().firstOrNull()
    }

    // receives the element as returned by findElementForParameterInfo
    override fun showParameterInfo(element: PsiElement, context: CreateParameterInfoContext) {
        if (element !is ElmFunctionCall) return

        val paramsDescription = describeParametersOf(element)
        if (paramsDescription == null) {
            println("showParameterInfo() for ${element.text} FAILED to produce a description of the func parameters")
            return
        }

        println("showParameterInfo() for '${element.text}', itemsToShow='${paramsDescription.presentText}'")

        context.itemsToShow = arrayOf(paramsDescription)
        context.showHint(
                element,                        // TODO re-consider this
                element.textRange.startOffset,  // TODO re-consider this
                this)
    }

    override fun updateParameterInfo(parameterOwner: PsiElement, context: UpdateParameterInfoContext) {
        println("updateParameterInfo() called with parameterOwner=$parameterOwner")

        context.setCurrentParameter(0) // TODO implement me for real
    }

    override fun updateUI(p: ElmParametersDescription?, context: ParameterInfoUIContext) {
        println("updateUI() called for $p")
        if (p == null) {
            context.isUIComponentEnabled = false
            return
        }

        hintText = p.presentText

        val range = p.getHighlightRange(context.currentParameterIndex)

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

    private fun describeParametersOf(funcCall: ElmFunctionCall): ElmParametersDescription? {
        val target = funcCall.target as? ElmValueExpr ?: return null
        val resolved = target.reference.resolve() ?: return null
        if (resolved !is ElmFunctionDeclarationLeft) return null
        return ElmParametersDescription.fromFuncCall(resolved)
    }
}

class ElmParametersDescription(val parameters: List<String>) {
    val presentText: String
        get() = parameters.joinToString(separator)

    fun getHighlightRange(index: Int): TextRange {
        if (index < 0 || index >= parameters.size) return TextRange.EMPTY_RANGE
        val start = parameters.take(index).sumBy { it.length + separator.length }
        return TextRange(start, start + parameters[index].length)
    }

    companion object {
        private val separator = ", "

        fun fromFuncCall(funcDecl: ElmFunctionDeclarationLeft): ElmParametersDescription {
            // TODO handle destructured parameter names
            // TODO add type information for each parameter
            val params = funcDecl.namedParameters
            return ElmParametersDescription(params.map { it.name ?: "???" })
        }
    }
}
