package org.elm.lang.core.types

import org.elm.lang.core.psi.ElmNamedElement

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
/**
 * A type variable, either rigid or flexible.
 *
 * e.g. Given the following declaration:
 *
 * ```
 * foo : a -> ()
 * foo x =
 *    ...
 * ```
 *
 * While inferring the body of the declaration, the value `x` is a rigid variable (meaning it can't
 * be assigned to anything expecting a concrete type, so an expression like `x + 1` is invalid).
 * When calling `foo`, the parameter is a flexible variable (meaning any type can be passed as an
 * argument).
 */
class TyVar(val name: String, val rigid: Boolean = false) : Ty() {
    override val alias: AliasInfo? get() = null
    override fun withAlias(alias: AliasInfo): TyVar = this
    override fun toString(): String {
        return "<${if (rigid) "!" else ""}$name@${(System.identityHashCode(this)).toString(16).take(3)}>"
    }
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
        override val alias: AliasInfo? = null,
        val fieldReferences: Map<String, ElmNamedElement> = emptyMap()
) : Ty() {
    /** true if this record has a base name, and will match a subset of a record's fields */
    val isSubset: Boolean get() = baseTy != null

    override fun withAlias(alias: AliasInfo): TyRecord = copy(alias = alias)
    override fun toString(): String {
        val f = fields.toString().let { it.substring(1, it.lastIndex) }
        return alias?.let {
            "{${it.name}${if (it.parameters.isEmpty()) "" else " ${it.parameters.joinToString(" ")}"}}"
        } ?: baseTy?.let { "{$baseTy | $f}" } ?: "{$f}"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TyRecord) return false
        return fields == other.fields && baseTy == other.baseTy
    }

    override fun hashCode(): Int {
        var result = fields.hashCode()
        result = 31 * result + (baseTy?.hashCode() ?: 0)
        return result
    }
}

/**
 * A [TyRecord] with mutable fields, only used internally by type inference. These are never cached.
 *
 * This is used to track multiple constraints on record types. Only records can have more than one
 * constraint, so other types don't need a mutable version.
 */
data class MutableTyRecord(
        val fields: MutableMap<String, Ty>,
        val baseTy: Ty? = null
) : Ty() {
    fun toRecord() = TyRecord(fields.toMap(), baseTy)

    override val alias: AliasInfo? get() = null
    override fun withAlias(alias: AliasInfo) = error("MutableTyRecord cannot have aliases")
    override fun toString() = toRecord().toString()
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
        return "(${listOf(module, name).joinToString(".")} ${parameters.joinToString(" ")})"
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
val TyUnion.isTyInt: Boolean get() = module == TyInt.module && name == TyInt.name
val TyUnion.isTyFloat: Boolean get() = module == TyFloat.module && name == TyFloat.name
val TyUnion.isTyBool: Boolean get() = module == TyBool.module && name == TyBool.name
val TyUnion.isTyString: Boolean get() = module == TyString.module && name == TyString.name
val TyUnion.isTyChar: Boolean get() = module == TyChar.module && name == TyChar.name

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

    fun uncurry(): TyFunction = when (ret) {
        is TyFunction -> TyFunction(parameters + ret.parameters, ret.ret)
        else -> this
    }

    override fun withAlias(alias: AliasInfo): TyFunction = copy(alias = alias)

    override fun toString(): String {
        return allTys.joinToString(" → ", prefix = "(", postfix = ")")
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

/** Create a lazy sequence of all [TyVar]s referenced within this ty. */
fun Ty.allVars(includeAlias: Boolean = false): Sequence<TyVar> = sequence<TyVar> {
    when (this@allVars) {
        is TyVar -> yield(this@allVars)
        is TyTuple -> types.forEach { yieldAll(it.allVars(includeAlias)) }
        is TyRecord -> {
            fields.values.forEach { yieldAll(it.allVars(includeAlias)) }
            if (baseTy != null) yieldAll(baseTy.allVars(includeAlias))
        }
        is MutableTyRecord -> {
            fields.values.forEach { yieldAll(it.allVars(includeAlias)) }
            if (baseTy != null) yieldAll(baseTy.allVars(includeAlias))
        }
        is TyFunction -> {
            yieldAll(ret.allVars(includeAlias))
            parameters.forEach { yieldAll(it.allVars(includeAlias)) }
        }
        is TyUnion -> {
            parameters.forEach { yieldAll(it.allVars(includeAlias)) }
        }
        is TyUnit, is TyUnknown, TyInProgressBinding -> {
        }
    }
    if (includeAlias) {
        alias?.parameters?.forEach { yieldAll(it.allVars(includeAlias)) }
    }
}

data class DeclarationInTy(val module: String, val name: String, val isUnion: Boolean)

/** Create a lazy sequence of all union and alias instances within a [Ty]. */
fun Ty.allDeclarations(
        includeFunctions: Boolean = false,
        includeUnionsWithAliases: Boolean = false
): Sequence<DeclarationInTy> = sequence {
    when (this@allDeclarations) {
        is TyVar -> {
        }
        is TyTuple -> types.forEach { yieldAll(it.allDeclarations()) }
        is TyRecord -> {
            fields.values.forEach { yieldAll(it.allDeclarations()) }
            if (baseTy != null) yieldAll(baseTy.allDeclarations())
        }
        is MutableTyRecord -> {
            fields.values.forEach { yieldAll(it.allDeclarations()) }
            if (baseTy != null) yieldAll(baseTy.allDeclarations())
        }
        is TyFunction -> {
            if (includeFunctions) {
                yieldAll(ret.allDeclarations())
                parameters.forEach { yieldAll(it.allDeclarations()) }
            }
        }
        is TyUnion -> {
            if (includeUnionsWithAliases || alias == null) {
                yield(DeclarationInTy(module, name, isUnion = true))
            }
            parameters.forEach { yieldAll(it.allDeclarations()) }
        }
        is TyUnit, is TyUnknown, TyInProgressBinding -> {
        }
    }
    alias?.let {
        yield(DeclarationInTy(it.module, it.name, isUnion = false))
        it.parameters.forEach { p -> yieldAll(p.allDeclarations()) }
    }
}
