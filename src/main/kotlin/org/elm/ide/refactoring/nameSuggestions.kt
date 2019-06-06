package org.elm.ide.refactoring

import com.intellij.psi.codeStyle.NameUtil
import org.elm.lang.core.psi.ElmExpressionTag
import org.elm.lang.core.psi.elements.ElmFieldAccessExpr
import org.elm.lang.core.psi.elements.ElmFunctionCallExpr
import org.elm.lang.core.psi.elements.ElmValueExpr
import org.elm.lang.core.resolve.scope.ExpressionScope
import org.elm.lang.core.toElmLowerId
import org.elm.lang.core.types.TyFloat
import org.elm.lang.core.types.TyFunction
import org.elm.lang.core.types.TyInt
import org.elm.lang.core.types.findTy

data class SuggestedNames(val default: String, val all: LinkedHashSet<String>)

/**
 * Return at least one suggestion for a name that describes the receiver expression.
 */
fun ElmExpressionTag.suggestedNames(): SuggestedNames {
    val names = LinkedHashSet<String>()
    val ty = findTy()

    when {
        this is ElmFunctionCallExpr -> {
            // suggest based on function call (e.g. suggest "books" for expr: `getBooks "shakespeare" library`)
            val target = target
            if (target is ElmValueExpr) {
                names.addName(target.referenceName)
            }
        }
        this is ElmFieldAccessExpr -> {
            // suggest the last field in a record field access chain (e.g. "title" in expr `model.currentPage.title`)
            names.addName(this.lowerCaseIdentifier?.text)
        }
    }

    // if present, suggest type alias (useful for records which often have a descriptive alias name)
    ty?.alias?.name?.let { names.addName(it) }

    // suggest based on common types
    when (ty) {
        TyInt -> names.addName("i")
        TyFloat -> names.addName("x")
        is TyFunction -> names.addName("f")
    }

    // filter-out any suggestion which would conflict with a name that is already in lexical scope
    val usedNames = ExpressionScope(this).getVisibleValues().mapNotNull { it.name }.toSet()
    val defaultName = suggestDefault(names, usedNames)
    names.removeAll(usedNames)

    return SuggestedNames(defaultName, names)
}

fun suggestDefault(names: LinkedHashSet<String>, usedNames: Set<String>): String {
    val default = names.firstOrNull() ?: "x"
    if (default !in usedNames) return default
    return (1..100).asSequence()
            .map { "$default$it" }
            .first { it !in usedNames }
}


private fun LinkedHashSet<String>.addName(name: String?) {
    if (name == null) return
    // Generate variants on compound words (e.g. "selectedItem" -> ["item", "selectedItem"])
    NameUtil.getSuggestionsByName(name, "", "", false, false, false)
            .mapTo(this) { it.toElmLowerId() }
}