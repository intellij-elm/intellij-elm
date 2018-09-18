package org.elm.lang.core.types

import com.intellij.codeInsight.documentation.DocumentationManagerUtil

fun Ty.renderedText(linkify: Boolean, withModule: Boolean): String = when (this) {
    is TyFunction -> renderedText(linkify, withModule)
    is TyUnion -> renderedText(linkify, withModule)
    is TyRecord -> renderedText(linkify, withModule)
    is TyTuple -> renderedText(linkify, withModule)
    is TyVar -> name
    TyUnknown -> "unknown"
    TyUnit -> "()"
    TyShader -> "shader"
}

fun TyFunction.renderedText(linkify: Boolean, withModule: Boolean): String {
    return (parameters + ret).joinToString(" -> ") {
        if (it is TyFunction) "(${it.renderedText(linkify, withModule)})" else it.renderedText(linkify, withModule)
    }
}

fun TyUnion.renderedText(linkify: Boolean, withModule: Boolean): String {
    val name = buildString { renderLink(name, name, linkify) }
    val type = when {
        parameters.isEmpty() -> name
        else -> parameters.joinToString(" ", prefix = "$name ") {
            if (it is TyUnion) "(${it.renderedText(linkify, withModule)})" else it.renderedText(linkify, withModule)
        }
    }
    return if (withModule && module.isNotBlank()) "$module.$type" else type
}

fun TyRecord.renderedText(linkify: Boolean, withModule: Boolean): String {
    return fields.entries.joinToString(",<br>", prefix = "{", postfix = "}") { (name, ty) ->
        "$name: ${ty.renderedText(linkify, withModule)}"
    } // TODO we probably don't want to render all fields
}

fun TyTuple.renderedText(linkify: Boolean, withModule: Boolean): String {
    return types.joinToString(", ", prefix = "(", postfix = ")") { it.renderedText(linkify, withModule) }
}

private fun StringBuilder.renderLink(refText: String, text: String, linkify: Boolean) {
    if (linkify) DocumentationManagerUtil.createHyperlink(this, refText, text, true)
    else append(text)
}
