package org.elm.lang.core.types

sealed class Ty

data class TyVar(val name: String, val origin: Ty?) : Ty()

object TyUnit : Ty() {
    override fun toString(): String = javaClass.simpleName
}

data class TyTuple(val types: List<Ty>) : Ty()

data class TyRecord(val fields: List<Pair<String, Ty>>) : Ty()

/** A type like `String` or `Nothing` that has no type parameters */
data class TyPrimitive(val name: String) : Ty()

data class TyParametric(val name: String, val parameters: List<Ty>) : Ty()

data class TyFunction(val parameters: List<Ty>, val ret: Ty) : Ty()

object TyUnknown : Ty() {
    override fun toString(): String = javaClass.simpleName
}
