package org.elm.ide.refactoring

import org.elm.lang.core.psi.ElmExpressionTag
import org.elm.lang.core.resolve.scope.ExpressionScope

/** Returns a derivation of [originalName] which is unique in the current lexical value scope. */
fun uniqueValueName(expr: ElmExpressionTag, originalName: String): String {
    require(originalName.isNotBlank())
    val visibleNames = ExpressionScope(expr).getVisibleValues().mapNotNullTo(HashSet()) { it.name }
    return sequence {
        yield(originalName)
        yieldAll((2..Int.MAX_VALUE).asSequence().map { originalName + it })
    }.first { it !in visibleNames }
}