package org.elm.ide.hints

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.lang.parameterInfo.*
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.TokenType
import com.intellij.psi.util.PsiTreeUtil
import org.elm.lang.core.psi.ElmFunctionCallTargetTag
import org.elm.lang.core.psi.ElmTypes.*
import org.elm.lang.core.psi.ancestorsStrict
import org.elm.lang.core.psi.elementType
import org.elm.lang.core.psi.elements.ElmFunctionCallExpr
import org.elm.lang.core.psi.elements.ElmValueExpr
import org.elm.lang.core.types.Ty
import org.elm.lang.core.types.TyUnknown
import org.elm.lang.core.types.findTy
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

    override fun getParametersForLookup(item: LookupElement?, context: ParameterInfoContext?): Array<Any>? =
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

    private fun findFuncCall(caretElement: PsiElement): ElmFunctionCallExpr? {
        // NOTE: this logic is not restrictive enough. It accepts some things as a blank line
        //       following a function call expr as being part of the function call.
        // TODO this could be made cleaner if we could figure out how to get GrammarKit to parse
        //      trailing whitespace after a FunctionCallExpr as being part of the Expr itself.
        val element = when (caretElement.elementType) {
            TokenType.WHITE_SPACE,
            VIRTUAL_END_DECL,
            VIRTUAL_END_SECTION,
            RIGHT_BRACE,
            RIGHT_SQUARE_BRACKET,
            RIGHT_PARENTHESIS -> PsiTreeUtil.prevVisibleLeaf(caretElement) ?: return null
            else -> caretElement
        }

        val ancestorsStrict = element.ancestorsStrict
        log.debug("findFuncCall for $element (${element.text}) ancestorsStrict=${ancestorsStrict.toList()}")
        return ancestorsStrict.filterIsInstance<ElmFunctionCallExpr>().firstOrNull()
                ?.takeIf { it.target is ElmFunctionCallTargetTag }
    }

    // receives the element as returned by findElementForParameterInfo
    override fun showParameterInfo(element: PsiElement, context: CreateParameterInfoContext) {
        if (element !is ElmFunctionCallExpr) return

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

class ElmParametersDescription(private val name: String?, private val ty: Ty) {
    val presentText: String
        get() =
            if (name == null) ty.renderedText()
            else "$name : ${ty.renderedText()}"

    val rangeToHighlight: TextRange
        get() = TextRange(0, name?.length ?: 0)

    companion object {
        fun fromCall(functionCall: ElmFunctionCallExpr): ElmParametersDescription? {
            // If calling a declared function or constructor, show its unqualified name
            val name = (functionCall.target as? ElmValueExpr)?.referenceName
            val ty = functionCall.target.findTy()

            if (ty != null && ty !is TyUnknown) {
                return ElmParametersDescription(name, ty)
            }

            return null
        }
    }
}
