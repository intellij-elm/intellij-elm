package org.elm.lang.core.types

/**
 * A type in the inference system.
 *
 * The name "Ty" is used to differentiate it from the PsiElements with "Type" in their name.
 */
sealed class Ty {
    /**
     * The type of the alias that this ty was referenced through, if there is one.
     *
     * If the type was inferred from a literal or referenced directly, this will be null. Types that
     * cannot be aliased like [TyVar] will always return null.
     */
    abstract val alias: AliasInfo?

    /** Make a copy of this ty with the given [alias] as its alias */
    abstract fun withAlias(alias: AliasInfo): Ty
}

// vars are not a data class because they need to be compared by identity
/** A declared ("rigid") type variable (e.g. `a` in `Maybe a`) */
class TyVar(val name: String) : Ty() {
    override val alias: AliasInfo? get() = null
    override fun withAlias(alias: AliasInfo): TyVar = this
    override fun toString(): String = "<TyVar $name>"
}

/**
 * An implicit ("flexible") type variable (i.e. the type of parameters in unannotated functions)
 *
 * These types must be unified.
 */
data class TyInfer(val name: String) : Ty() {
    override val alias: AliasInfo? get() = null
    override fun withAlias(alias: AliasInfo): TyInfer = this
    override fun toString(): String = "<TyInfer $name>"
}

/** A tuple type like `(Int, String)` */
data class TyTuple(val types: List<Ty>, override val alias: AliasInfo? = null) : Ty() {
    init {
        require(types.isNotEmpty()) { "can't create a tuple with no types. Use TyUnit." }
    }

    override fun withAlias(alias: AliasInfo): TyTuple = copy(alias = alias)
}

/**
 * A record type like `{x: Int, y: Float}` or `{a | x: Int}`
 *
 * @property fields map of field name to ty
 * @property baseTy The type of the base record identifier, if there is one. Non-null for field
 *   accessors, record with base identifiers etc. that match a subset of record fields
 * @property alias The alias for this record, if there is one. Used for rendering and tracking record constructors
 */
data class TyRecord(
        val fields: Map<String, Ty>,
        val baseTy: Ty? = null,
        override val alias: AliasInfo? = null
) : Ty() {
    /** true if this record has a base name, and will match a subset of a record's fields */
    val isSubset: Boolean get() = baseTy != null

    override fun withAlias(alias: AliasInfo): TyRecord = copy(alias = alias)
}

/** A type like `String` or `Maybe a` */
data class TyUnion(
        val module: String,
        val name: String,
        val parameters: List<Ty>,
        override val alias: AliasInfo? = null
) : Ty() {
    override fun withAlias(alias: AliasInfo): TyUnion = copy(alias = alias)

    override fun toString(): String {
        return "<TyUnion ${listOf(module, name).joinToString(".")} ${parameters.joinToString(" ")}>"
    }
}

val TyInt = TyUnion("Basics", "Int", emptyList())
val TyFloat = TyUnion("Basics", "Float", emptyList())
val TyBool = TyUnion("Basics", "Bool", emptyList())
val TyString = TyUnion("String", "String", emptyList())
val TyChar = TyUnion("Char", "Char", emptyList())

/** WebGL GLSL shader */
// The actual type is `Shader attributes uniforms varyings`, but we would have to parse the
// GLSL code to infer the type variables, so we just don't report diagnostics on shader types.
val TyShader = TyUnion("WebGL", "Shader", listOf(TyUnknown(), TyUnknown(), TyUnknown()))

@Suppress("FunctionName")
fun TyList(elementTy: Ty) = TyUnion("List", "List", listOf(elementTy))

val TyUnion.isTyList: Boolean get() = module == "List" && name == "List"

data class TyFunction(
        val parameters: List<Ty>,
        val ret: Ty,
        override val alias: AliasInfo? = null
) : Ty() {
    init {
        require(parameters.isNotEmpty()) { "can't create a function with no parameters" }
    }

    val allTys get() = parameters + ret

    fun partiallyApply(count: Int): Ty = when {
        count < parameters.size -> TyFunction(parameters.drop(count), ret)
        else -> ret
    }

    override fun withAlias(alias: AliasInfo): TyFunction = copy(alias = alias)

    override fun toString(): String {
        return allTys.joinToString(" → ", prefix = "<TyFunction ", postfix = ">")
    }
}

/** The [Ty] representing `()` */
data class TyUnit(override val alias: AliasInfo? = null) : Ty() {
    override fun withAlias(alias: AliasInfo): TyUnit = copy(alias = alias)
    override fun toString(): String = javaClass.simpleName
}

/**
 *  A [Ty] that can be assigned to and from any [Ty].
 *
 * Used for unimplemented functionality and in partial programs where it's not possible to infer a type.
 */
data class TyUnknown(override val alias: AliasInfo? = null) : Ty() {
    override fun withAlias(alias: AliasInfo): TyUnknown = copy(alias = alias)
    override fun toString(): String = javaClass.simpleName
}

/** Not a real ty, but used to diagnose cyclic values in parameter bindings */
object TyInProgressBinding : Ty() {
    override val alias: AliasInfo? get() = null
    override fun withAlias(alias: AliasInfo): TyInProgressBinding = this
    override fun toString(): String = javaClass.simpleName
}

/** Information about a type alias. This is not a [Ty]. */
data class AliasInfo(val module: String, val name: String, val parameters: List<Ty>)
