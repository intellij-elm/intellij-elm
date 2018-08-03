package org.elm.lang.core.types.ty

data class TyRecord(val fields: List<Pair<String, Ty>>) : Ty
