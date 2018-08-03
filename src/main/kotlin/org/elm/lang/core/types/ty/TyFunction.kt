package org.elm.lang.core.types.ty

data class TyFunction(val parameters: List<Ty>, val ret: Ty) : Ty
