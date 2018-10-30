package org.elm.ide.hints

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.lang.parameterInfo.*
import com.intellij.psi.PsiElement
import org.elm.lang.core.psi.ElmOperandTag
import org.elm.lang.core.psi.elements.ElmFunctionCall
import org.elm.lang.core.psi.parentOfType

class ElmParameterInfoHandler : ParameterInfoHandler<PsiElement, ElmParametersDescription> {

    var hintText: String = ""

    override fun couldShowInLookup() = true

    override fun getParametersForLookup(item: LookupElement?, context: ParameterInfoContext?): Array<Any>? {
        println("getParametersForLookup() called for item $item")
        val elem = item?.`object` as? PsiElement ?: return null
        if (elem !is ElmOperandTag) {
            println("Skipping $elem because not an ElmOperand")
            return null
        }

        val funcCall = elem.parentOfType<ElmFunctionCall>()
        return if (funcCall == null) {
            println("Skipping $elem because parent is not an ElmFunctionCall")
            emptyArray()
        } else {
            // TODO verify that the function call target is actually a function
            println("FOUND $elem")
            arrayOf(funcCall)
        }
    }

    override fun findElementForParameterInfo(context: CreateParameterInfoContext): PsiElement? {
        val element = context.file.findElementAt(context.editor.caretModel.offset)
        println("findElementForParameterInfo() returning $element")
        return element
    }

    override fun findElementForUpdatingParameterInfo(context: UpdateParameterInfoContext): PsiElement? {
        val element = context.file.findElementAt(context.editor.caretModel.offset)
        println("findElementForUpdatingParameterInfo() returning $element")
        return element
    }

    override fun showParameterInfo(element: PsiElement, context: CreateParameterInfoContext) {
        println("showParameterInfo() for $element")
        // `element` as returned by [findElementForParameterInfo]
        context.itemsToShow = emptyArray()      // TODO implement me (should be ElmParametersDescription)
        context.showHint(
                element,                        // TODO re-consider this
                element.textRange.startOffset,  // TODO re-consider this
                this)
    }

    override fun updateParameterInfo(parameterOwner: PsiElement, context: UpdateParameterInfoContext) {
        println("updateParameterInfo() called with parameterOwner=$parameterOwner")
//
//        val argIndex = findArgumentIndex(place)
//        if (argIndex == INVALID_INDEX) {
//            context.removeHint()
//            return
//        }
        val argIndex = 0

        context.setCurrentParameter(argIndex) // TODO implement me for real

        // TODO intellij-rust did some stuff to context.parameterOwner that I don't understand
//        when {
//            context.parameterOwner == null -> context.parameterOwner = place
//            context.parameterOwner != findElementForParameterInfo(place) -> {
//                context.removeHint()
//                return
//            }
//        }

        // TODO is this needed?
        context.objectsToView.indices.map { context.setUIComponentEnabled(it, true) }
    }

    override fun updateUI(p: ElmParametersDescription?, context: ParameterInfoUIContext) {
        println("updateUI() called for $p")
        if (p == null) {
            context.isUIComponentEnabled = false
            return
        }

        hintText = p.presentText

        // update the UI to highlight the currently selected element
//        val range = p.getArgumentRange(context.currentParameterIndex)
//        context.setupUIComponentPresentation(
//                hintText,
//                range.startOffset,
//                range.endOffset,
//                !context.isUIComponentEnabled,
//                false,
//                false,
//                context.defaultParameterColor)
    }
}

class ElmParametersDescription(val parameters: List<String>) {
    val presentText: String
        get() = parameters.joinToString(", ")

}
