package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import org.elm.lang.core.psi.*
import org.elm.lang.core.types.*


/**
 * A function call expression.
 *
 * e.g.
 *  - `toString 1`
 *  - `(+) 1 2`
 *  - `List.map toString [1, 2]`
 *  - `record.functionInField ()`
 *  - `(\x -> x) 1`
 *  - `(a << b) c
 */
class ElmFunctionCall(node: ASTNode) : ElmPsiElementImpl(node), ElmOperandTag {
    /** The function or operator being called */
    val target: ElmFunctionCallTargetTag get() = findNotNullChildByClass(ElmFunctionCallTargetTag::class.java)

    /** The arguments to the function. This will always have at least one element */
    val arguments: Sequence<ElmOperandTag> get() = directChildren.filterIsInstance<ElmOperandTag>().drop(1)

    /**
     * Returns a description of the function call's **declared** parameters.
     * Compare to [arguments], which is what the caller actually provided, and may differ.
     */
    fun resolveCallInfo(): CallInfo? {
        // TODO handle destructured parameter names
        // TODO generalize so that it works with other kinds of call targets (such as lambdas)

        val resolved = target.reference?.resolve() ?: return null
        if (resolved !is ElmFunctionDeclarationLeft) return null

        val inference = resolved.parentOfType<ElmValueDeclaration>()?.inference()
        val parameters: List<CallInfo.Parameter>
        if (inference != null && inference.ty is TyFunction) {
            parameters = resolved.namedParameters
                    .zip(inference.ty.parameters)
                    .map { (param, ty) -> CallInfo.Parameter(param.name ?: "?", ty) }
        } else {
            parameters = resolved.namedParameters
                    .map { param -> CallInfo.Parameter(param.name ?: "?", TyUnknown) }
        }

        return CallInfo(resolved.name, parameters)
    }
}


/**
 * An externally-focused description of a function and its expected parameters.
 *
 * This is specifically meant for **usage at the function call site**, that is,
 * the outside view of the function, ignoring any kind of internal destructuring that
 * may happen within the function body.
 *
 * With that being said, Elm's parameter destructuring can cause a problem here because
 * you may have, say, a single tuple argument which is destructured internally, deriving
 * the parameter of its rightful name. In such cases, the parameter will be given
 * an anonymous name.
 */
data class CallInfo(
        val functionName: String,
        val parameters: List<Parameter>
) {

    data class Parameter(val name: String, val ty: Ty) {
        override fun toString(): String {
            return "$name: ${ty.renderedText(linkify = false, withModule = false)}"
        }
    }
}
