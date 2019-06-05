package org.elm.ide.refactoring

import com.intellij.psi.codeStyle.NameUtil
import org.elm.lang.core.psi.ElmExpressionTag
import org.elm.lang.core.psi.elements.ElmFunctionCallExpr
import org.elm.lang.core.psi.elements.ElmValueExpr
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

    // suggest based on function call (e.g. suggest "books" for expr: `getBooks "shakespeare" library`)
    if (this is ElmFunctionCallExpr) {
        val target = target
        if (target is ElmValueExpr) {
            names.addName(target.referenceName)
        }
    }

    // TODO suggest names on based on ElmFieldAccessExpr (e.g. suggest "title" for `model.currentPage.title`)

    // if present, suggest type alias (useful for records which often have a descriptive alias name)
    ty?.alias?.name?.let { names.addName(it) }

    // suggest based on common types
    when (ty) {
        TyInt -> names.addName("i")
        TyFloat -> names.addName("x")
        is TyFunction -> names.addName("f")
    }

    // TODO do not suggest names which are already bound in the current lexical scope

    val defaultName = names.firstOrNull() ?: "x"
    return SuggestedNames(defaultName, names)
}

private fun LinkedHashSet<String>.addName(name: String) {
    // Generate variants on compound words (e.g. "selectedItem" -> ["item", "selectedItem"])
    NameUtil.getSuggestionsByName(name, "", "", false, false, false)
            .mapTo(this) { it.toElmLowerId() }
}