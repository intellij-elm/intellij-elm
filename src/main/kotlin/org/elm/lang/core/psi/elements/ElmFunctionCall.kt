package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import org.elm.lang.core.psi.*
import org.elm.lang.core.types.Ty
import org.elm.lang.core.types.TyFunction
import org.elm.lang.core.types.TyUnknown
import org.elm.lang.core.types.inference


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
        // TODO generalize so that it works with other kinds of call targets (such as lambdas)
        val resolved = target.reference?.resolve() as? ElmFunctionDeclarationLeft
                ?: return null

        val inference = resolved.parentOfType<ElmValueDeclaration>()?.inference()
        if (inference != null && inference.ty is TyFunction) {
            val parameters = inference.ty.parameters.map { CallInfo.Parameter(it) }
            return CallInfo(resolved.name, parameters, inference.ty.ret)
        } else {
            return CallInfo(resolved.name, emptyList(), TyUnknown)
        }
    }
}


/**
 * An externally-focused description of a function and its expected parameters.
 *
 * This is specifically meant for **usage at the function call site**, that is,
 * the outside view of the function, ignoring any kind of internal destructuring that
 * may happen within the function body.
 *
 * Hence, the parameters are given by their types only: the names are treated as an
 * internal detail. In the future we may want to give the choice to include parameter
 * names, but, if so, we will have to deal with destructured, anonymous parameters.
 */
data class CallInfo(
        val functionName: String,
        val parameters: List<Parameter>,
        val returnType: Ty
) {
    data class Parameter(val ty: Ty)
}
