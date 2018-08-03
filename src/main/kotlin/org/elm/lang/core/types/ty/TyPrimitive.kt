package org.elm.lang.core.types.ty

/** A type like `String` or `Nothing` that has no type parameters */
data class TyPrimitive(val name: String) : Ty
