package org.elm.lang.core.types.ty

data class TyVar(val name: String, val origin: Ty?) : Ty
