package org.elm.lang.core.types

import org.elm.lang.core.types.ty.*

val Ty.renderedText: String
    get() = when (this) {
        is TyFunction -> (parameters + ret).joinToString(" -> ") { if (it is TyFunction) "(${it.renderedText})" else it.renderedText }
        is TyParametric -> parameters.joinToString(" ", prefix = "$name ") { it.renderedText } // TODO we probably need parenthesis in some cases
        is TyPrimitive -> name
        is TyRecord -> fields.joinToString(", ", prefix = "{", postfix = "}") { (name, ty) -> "$name: ${ty.renderedText}" } // TODO we probably don't want to render all fields
        is TyTuple -> types.joinToString(", ", prefix = "(", postfix = ")") { it.renderedText }
        is TyUnknown -> "_"
        is TyVar -> name
        is TyUnit -> "()"
        else -> error("unimplemented type $this")
    }
