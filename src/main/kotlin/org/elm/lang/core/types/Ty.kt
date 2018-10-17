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

/**
 * @property fields map of field name to ty
 * @property baseName The name of the base record identifier, if there is one. Non-null for field
 *   accessors, record with base identifiers etc. that match a subset of record fields
 * @property alias The alias for this record, if there is one. Used for rendering and tracking record constructors
 */
data class TyRecord(
        val fields: Map<String, Ty>,
        val baseName: String? = null,
        val alias: TyUnion? = null
) : Ty() {
    /** true if this record has a base name, and will match a subset of a record's fields */
    val isSubset: Boolean get() = baseName != null
}

/** A type like `String` or `Maybe a` */
data class TyUnion(val module: String, val name: String, val parameters: List<Ty>) : Ty()

val TyInt = TyUnion("Basics", "Int", emptyList())
val TyFloat = TyUnion("Basics", "Float", emptyList())
val TyBool = TyUnion("Basics", "Bool", emptyList())
val TyString = TyUnion("String", "String", emptyList())
val TyChar = TyUnion("Char", "Char", emptyList())

/** WebGL GLSL shader */
// The actual type is `Shader attributes uniforms varyings`, but we would have to parse the
// GLSL code to infer this.
val TyShader = TyUnion("WebGL", "Shader", listOf(TyUnknown, TyUnknown, TyUnknown))

fun TyList(elementTy: Ty) = TyUnion("List", "List", listOf(elementTy))
val TyUnion.isTyList: Boolean get() = module == "List" && name == "List"

data class TyFunction(val parameters: List<Ty>, val ret: Ty) : Ty() {
    init {
        require(parameters.isNotEmpty()) { "can't create a function with no parameters" }
    }

    val allTys get() = parameters + ret
}

object TyUnit : Ty() {
    override fun toString(): String = javaClass.simpleName
}

object TyUnknown : Ty() {
    override fun toString(): String = javaClass.simpleName
}

/** Not a real ty, but used to diagnose cyclic values in parameter bindings */
object TyInProgressBinding : Ty() {
    override fun toString(): String = javaClass.simpleName
}
