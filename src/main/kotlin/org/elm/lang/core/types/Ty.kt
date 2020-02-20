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
 * @property fieldReferences A map of field name to the psi element that defines them; used for
 *   reference resolve, only the frozen status affects equality
 */
data class TyRecord(
        val fields: Map<String, Ty>,
        val baseTy: Ty? = null,
        override val alias: AliasInfo? = null,
        val fieldReferences: RecordFieldReferenceTable = RecordFieldReferenceTable()
) : Ty() {
    companion object {
        // Empty records occur in code like `foo : BaseRecord {}`, where they create a type from an
        // extension record with no extra fields.
        val emptyRecord = TyRecord(emptyMap(), null, null, RecordFieldReferenceTable().apply { freeze() })
    }

    /** true if this record has a base name, and will match a subset of a record's fields */
    val isSubset: Boolean get() = baseTy != null

    override fun withAlias(alias: AliasInfo): TyRecord = copy(alias = alias)
    override fun toString(): String {
        val f = fields.toString().let { it.substring(1, it.lastIndex) }
        return alias?.let {
            val prefix = if (fieldReferences.isEmpty()) "" else "+"
            val params = if (it.parameters.isEmpty()) "" else " ${it.parameters.joinToString(" ")}"
            "{$prefix${it.name}$params}"
        } ?: baseTy?.let { "{$baseTy | $f}" } ?: "{$f}"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TyRecord) return false
        if (fields != other.fields) return false
        if (baseTy != other.baseTy) return false
        if (alias != other.alias) return false
        if (fieldReferences.frozen != other.fieldReferences.frozen) return false
        return true
    }

    override fun hashCode(): Int {
        var result = fields.hashCode()
        result = 31 * result + (baseTy?.hashCode() ?: 0)
        result = 31 * result + (alias?.hashCode() ?: 0)
        result = 31 * result + fieldReferences.frozen.hashCode()
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
        val baseTy: Ty? = null,
        val fieldReferences: RecordFieldReferenceTable = RecordFieldReferenceTable()
) : Ty() {
    fun toRecord() = TyRecord(fields.toMap(), baseTy, alias, fieldReferences)

    override val alias: AliasInfo? get() = null
    override fun withAlias(alias: AliasInfo) = error("MutableTyRecord cannot have aliases")
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MutableTyRecord) return false
        if (fields != other.fields) return false
        if (baseTy != other.baseTy) return false
        if (alias != other.alias) return false
        if (fieldReferences.frozen != other.fieldReferences.frozen) return false
        return true
    }
    override fun hashCode(): Int {
        var result = fields.hashCode()
        result = 31 * result + (baseTy?.hashCode() ?: 0)
        result = 31 * result + (alias?.hashCode() ?: 0)
        result = 31 * result + fieldReferences.frozen.hashCode()
        return result
    }
    override fun toString() = "{~${toRecord().toString().drop(1)}"
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
        return "[${(listOf("$module.$name") + parameters).joinToString(" ")}]"
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


/** Return a list of all [TyVar]s in this ty, including itself */
fun Ty.allVars(): List<TyVar> = mutableListOf<TyVar>().also { allVars(it) }

private fun Ty.allVars(result: MutableList<TyVar>) {
    when (this) {
        is TyVar -> result.add(this)
        is TyTuple -> types.forEach { it.allVars(result) }
        is TyRecord -> {
            fields.values.forEach { it.allVars(result) }
            baseTy?.allVars(result)
        }
        is MutableTyRecord -> {
            fields.values.forEach { it.allVars(result) }
            baseTy?.allVars(result)
        }
        is TyUnion -> parameters.forEach { it.allVars(result) }
        is TyFunction -> {
            ret.allVars(result)
            parameters.forEach { it.allVars(result) }
        }
        is TyUnit, is TyUnknown, TyInProgressBinding -> {
        }
    }
}

/** Return `true` if this [Ty] or any of its children match the [predicate] */
fun Ty.anyVar(predicate: (TyVar) -> Boolean): Boolean {
    return when (this) {
        is TyVar -> predicate(this)
        is TyTuple -> types.any { it.anyVar(predicate) }
        is TyRecord -> fields.values.any { it.anyVar(predicate) } || baseTy?.anyVar(predicate) == true
        is MutableTyRecord -> fields.values.any { it.anyVar(predicate) } || baseTy?.anyVar(predicate) == true
        is TyUnion -> parameters.any { it.anyVar(predicate) }
        is TyFunction -> ret.anyVar(predicate) || parameters.any { it.anyVar(predicate) }
        is TyUnit, is TyUnknown, TyInProgressBinding -> false
    } || alias?.parameters?.any { it.anyVar(predicate) } == true
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
        is TyTuple -> types.forEach { yieldAll(it.allDeclarations(includeFunctions, includeUnionsWithAliases)) }
        is TyRecord -> {
            fields.values.forEach { yieldAll(it.allDeclarations(includeFunctions, includeUnionsWithAliases)) }
            if (baseTy != null) yieldAll(baseTy.allDeclarations(includeFunctions, includeUnionsWithAliases))
        }
        is MutableTyRecord -> {
            fields.values.forEach { yieldAll(it.allDeclarations(includeFunctions, includeUnionsWithAliases)) }
            if (baseTy != null) yieldAll(baseTy.allDeclarations(includeFunctions, includeUnionsWithAliases))
        }
        is TyFunction -> {
            if (includeFunctions) {
                yieldAll(ret.allDeclarations(includeFunctions, includeUnionsWithAliases))
                parameters.forEach { yieldAll(it.allDeclarations(includeFunctions, includeUnionsWithAliases)) }
            }
        }
        is TyUnion -> {
            if (includeUnionsWithAliases || alias == null) {
                yield(DeclarationInTy(module, name, isUnion = true))
            }
            parameters.forEach { yieldAll(it.allDeclarations(includeFunctions, includeUnionsWithAliases)) }
        }
        is TyUnit, is TyUnknown, TyInProgressBinding -> {
        }
    }
    alias?.let {
        yield(DeclarationInTy(it.module, it.name, isUnion = false))
        it.parameters.forEach { p -> yieldAll(p.allDeclarations()) }
    }
}

/** Extract the typeclass for a var name if it is one, or null if it's a normal var */
fun TyVar.typeclassName(): String? = when {
    name.length < 6 -> null // "number".length == 6
    name.startsWith("number") -> "number"
    name.startsWith("appendable") -> "appendable"
    name.startsWith("comparable") -> "comparable"
    name.startsWith("compappend") -> "compappend"
    else -> null
}
