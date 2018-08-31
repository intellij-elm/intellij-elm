package org.elm.lang.core.types

import com.intellij.codeInsight.documentation.DocumentationManagerUtil

fun Ty.renderedText(linkify: Boolean): String = when (this) {
    is TyFunction -> renderedText(linkify)
    is TyParametric -> renderedText(linkify)
    is TyPrimitive -> buildString { renderLink(name, name, linkify) }
    is TyRecord -> renderedText(linkify)
    is TyTuple -> renderedText(linkify)
    is TyUnknown -> "unknown"
    is TyVar -> name
    is TyUnit -> "()"
}

fun TyFunction.renderedText(linkify: Boolean): String {
    return (parameters + ret).joinToString(" -> ") {
        if (it is TyFunction) "(${it.renderedText(linkify)})" else it.renderedText(linkify)
    }
}

fun TyParametric.renderedText(linkify: Boolean): String {
    val name = buildString { renderLink(name, name, linkify) }
    return parameters.joinToString(" ", prefix = "$name ") {
        if (it is TyParametric) "(${it.renderedText(linkify)})" else it.renderedText(linkify)
    }
}

fun TyRecord.renderedText(linkify: Boolean): String {
    return fields.entries.joinToString(", ", prefix = "{", postfix = "}") { (name, ty) ->
        "$name: ${ty.renderedText(linkify)}"
    } // TODO we probably don't want to render all fields
}

fun TyTuple.renderedText(linkify: Boolean): String {
    return types.joinToString(", ", prefix = "(", postfix = ")") { it.renderedText(linkify) }
}

private fun StringBuilder.renderLink(refText: String, text: String, linkify: Boolean) {
    if (linkify) DocumentationManagerUtil.createHyperlink(this, refText, text, true)
    else append(text)
}
