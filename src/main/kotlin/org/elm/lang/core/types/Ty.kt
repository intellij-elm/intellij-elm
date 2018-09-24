package org.elm.lang.core.types

/**
 * A type in the inference system.
 *
 * The name "Ty" is used to differentiate it from the PsiElements with "Type" in their name.
 */
sealed class Ty

data class TyVar(val name: String) : Ty()

data class TyTuple(val types: List<Ty>) : Ty() {
    init {
        require(types.isNotEmpty()) { "can't create a tuple with no types. Use TyUnit." }
    }
}

data class TyRecord(val fields: Map<String, Ty>) : Ty()

/** A type like `String` or `Maybe a` */
data class TyUnion(val module: String, val name: String, val parameters: List<Ty>) : Ty()

val TyInt = TyUnion("Basics", "Int", emptyList())
val TyFloat = TyUnion("Basics", "Float", emptyList())
val TyString = TyUnion("String", "String", emptyList())
val TyChar = TyUnion("Char", "Char", emptyList())

fun TyList(parameters: Ty) = TyUnion("List", "List", listOf(parameters))

data class TyFunction(val parameters: List<Ty>, val ret: Ty) : Ty() {
    init {
        require(parameters.isNotEmpty()) { "can't create a function with no parameters" }
    }

    val allTys get() = parameters + ret
}

/** WebGL GLSL shader */
object TyShader : Ty() {
    override fun toString(): String = javaClass.simpleName
}

object TyUnit : Ty() {
    override fun toString(): String = javaClass.simpleName
}

object TyUnknown : Ty() {
    override fun toString(): String = javaClass.simpleName
}
