package org.elm.lang.core.types

import com.intellij.codeInsight.documentation.DocumentationManagerUtil

fun Ty.renderedText(linkify: Boolean, withModule: Boolean): String {
    return alias?.renderedText(linkify, withModule) ?: when (this) {
        is TyFunction -> renderedText(linkify, withModule)
        is TyUnion -> renderedText(linkify, withModule)
        is TyRecord -> renderedText(linkify, withModule)
        is TyTuple -> renderedText(linkify, withModule)
        is TyVar -> name
        is TyInfer -> name
        is TyUnit -> "()"
        is TyUnknown, TyInProgressBinding -> "unknown"
        TyShader -> "shader"
    }
}

fun TyFunction.renderedText(linkify: Boolean, withModule: Boolean): String {
    return (parameters + ret).joinToString(" → ") {
        if (it is TyFunction) "(${it.renderedText(linkify, withModule)})" else it.renderedText(linkify, withModule)
    }
}

fun TyUnion.renderedText(linkify: Boolean, withModule: Boolean): String {
    val name = buildString { renderLink(name, name, linkify) }
    val type = when {
        parameters.isEmpty() -> name
        else -> parameters.joinToString(" ", prefix = "$name ") {
            if (it is TyUnion && it.parameters.isNotEmpty()) {
                "(${it.renderedText(linkify, withModule)})"
            } else {
                it.renderedText(linkify, withModule)
            }
        }
    }
    return if (withModule && module.isNotBlank()) "$module.$type" else type
}

fun TyRecord.renderedText(linkify: Boolean, withModule: Boolean): String {
    val prefix = if (baseTy != null) "{ ${baseTy.renderedText(linkify, withModule)} | " else "{ "
    return fields.entries.joinToString(", ", prefix = prefix, postfix = " }") { (name, ty) ->
        "$name : ${ty.renderedText(linkify, withModule)}"
    }
}

fun TyTuple.renderedText(linkify: Boolean, withModule: Boolean): String {
    return types.joinToString(", ", prefix = "(", postfix = ")") { it.renderedText(linkify, withModule) }
}

fun AliasInfo.renderedText(linkify: Boolean, withModule: Boolean): String {
    return TyUnion(module, name, parameters).renderedText(linkify, withModule)
}

private fun StringBuilder.renderLink(refText: String, text: String, linkify: Boolean) {
    if (linkify) DocumentationManagerUtil.createHyperlink(this, refText, text, true)
    else append(text)
}

/** Render a name or destructuring pattern to use as a parameter for a function or case branch */
fun Ty.renderParam(): String {
    return alias?.name?.toFirstCharLowerCased() ?: when (this) {
        is TyFunction -> "function"
        is TyUnion -> name.toFirstCharLowerCased()
        is TyRecord -> "record"
        is TyTuple -> renderParam()
        is TyVar -> name
        is TyInfer -> name
        is TyUnit -> "()"
        is TyUnknown, TyInProgressBinding -> "unknown"
        TyShader -> "shader"
    }
}

fun TyTuple.renderParam(): String {
    return types.joinToString(", ", prefix = "(", postfix = ")") { it.renderParam() }
}

private fun String.toFirstCharLowerCased(): String =
        if (isEmpty()) ""
        else first().toLowerCase() + substring(1)
