package org.elm.lang.core.types

/**
 * This class performs deep replacement of a set of [TyVar]s in a [Ty] with a set of new types,
 * which could also be [TyVar]s.
 *
 * It relies on the fact that [TyVar]s can be compared by identity. Vars in different scopes must
 * compare unequal, even if they have the same name.
 *
 * The constructor takes a map of vars to the ty to replace them with.
 */
class TypeReplacement(
        // A map of variables that should be replaced to the ty to replace them with
        replacements: Map<TyVar, Ty>,
        private val freshen: Boolean,
        private val varsToRemainRigid: Collection<TyVar>?,
        private val keepRecordsMutable: Boolean
) {
    companion object {
        /**
         * Replace vars in [ty] according to [replacements].
         *
         * @param varsToRemainRigid If given, all [TyVar]s will be replaced with flexible copies,
         *  except for vars that occur in this collection, which will be left unchanged.
         * @param keepRecordsMutable If false, [MutableTyRecord]s will be replaces with [TyRecord]s
         */
        fun replace(
                ty: Ty,
                replacements: Map<TyVar, Ty>,
                varsToRemainRigid: Collection<TyVar>? = null,
                keepRecordsMutable: Boolean = false
        ): Ty {
            if (varsToRemainRigid == null && replacements.isEmpty()) return ty
            return TypeReplacement(
                    replacements,
                    freshen = false,
                    varsToRemainRigid = varsToRemainRigid,
                    keepRecordsMutable = keepRecordsMutable
            ).replace(ty)
        }

        fun replace(
                ty: Ty,
                replacements: DisjointSet,
                varsToRemainRigid: Collection<TyVar>? = null,
                keepRecordsMutable: Boolean = false
        ): Ty = replace(ty, replacements.asMap(), varsToRemainRigid, keepRecordsMutable)

        /**
         * Replace all [TyVar]s in a [ty] with new copies with the same names.
         *
         * This is important because tys are cached, and [TyVar]s are compared by instance to be
         * able to differentiate them by scope. Each time you call a function or reference a type,
         * its vars are distinct, even though they share a name with vars from the previous
         * reference. If we use the cached tys, there's no way to distinguish between vars in one
         * call from another.
         *
         * Note that rigid vars are never freshened. In a function with an annotation like
         * `f : (a -> b) -> a -> b`, every time you call the `(a -> b)` parameter within the body of
         * `f`, the ty of `a` and `b` have to be the same.
         */
        fun freshenVars(ty: Ty): Ty {
            return TypeReplacement(emptyMap(), freshen = true, varsToRemainRigid = null, keepRecordsMutable = false).replace(ty)
        }

        /**
         * Make all type variables in a [ty] flexible.
         *
         * Type variables in function annotations are rigid when bound to parameters; in all other
         * cases vars are flexible. This function is used to make vars inferred as rigid into flexible
         * when calling functions.
         */
        fun flexify(ty: Ty): Ty {
            return TypeReplacement(emptyMap(), freshen = false, varsToRemainRigid = emptyList(), keepRecordsMutable = false).replace(ty)
        }

        /**
         * Freeze any fields references present in records in a [ty].
         *
         * Use this to prevent cached values from being altered. Note that this is an in-place
         * change rather than a copy.
         */
        fun freeze(ty: Ty) {
            when (ty) {
                is TyVar -> Unit
                is TyTuple -> ty.types.forEach { freeze(it) }
                is TyRecord -> {
                    (ty.baseTy as? TyRecord)?.fieldReferences?.freeze()
                    ty.fields.values.forEach { freeze(it) }
                    ty.fieldReferences.freeze()
                }
                is MutableTyRecord -> {
                    (ty.baseTy as? TyRecord)?.fieldReferences?.freeze()
                    ty.fields.values.forEach { freeze(it) }
                    ty.fieldReferences.freeze()
                }
                is TyUnion -> ty.parameters.forEach { freeze(it) }
                is TyFunction -> {
                    freeze(ty.ret)
                    ty.parameters.forEach { freeze(it) }
                }
                is TyUnit -> Unit
                is TyUnknown -> Unit
                TyInProgressBinding -> Unit
            }
            ty.alias?.parameters?.forEach { freeze(it) }
        }
    }

    private fun wouldChange(ty: Ty): Boolean {
        val changeAllVars = freshen || varsToRemainRigid != null
        fun Ty.f(): Boolean = when (this) {
            is TyVar -> changeAllVars || this in replacements
            is TyTuple -> types.any { it.f() }
            is TyRecord -> fields.values.any { it.f() } || baseTy?.f() == true
            is MutableTyRecord -> !keepRecordsMutable || fields.values.any { it.f() } || baseTy?.f() == true
            is TyUnion -> parameters.any { it.f() }
            is TyFunction -> ret.f() || parameters.any { it.f() }
            is TyUnit, is TyUnknown, TyInProgressBinding -> false
        } || alias?.parameters?.any { it.f() } == true
        return ty.f()
    }

    /** A map of var to (has been accessed, ty) */
    private val replacements = replacements.mapValuesTo(mutableMapOf()) { (_, v) -> false to v }

    fun replace(ty: Ty): Ty {
        // If we wouldn't change anything, return the original ty to avoid duplicating the object
        if (!wouldChange(ty)) return ty
        return when (ty) {
            is TyVar -> getReplacement(ty) ?: ty
            is TyTuple -> replaceTuple(ty)
            is TyFunction -> replaceFunction(ty)
            is TyUnknown -> TyUnknown(replace(ty.alias))
            is TyUnion -> replaceUnion(ty)
            is TyRecord -> replaceRecord(ty.fields, ty.baseTy, ty.alias, ty.fieldReferences, wasMutable = false)
            is TyUnit, TyInProgressBinding -> ty
            is MutableTyRecord -> replaceRecord(ty.fields, ty.baseTy, null, ty.fieldReferences, wasMutable = true)
        }
    }

    /*
     * Although aliases are not used in ty comparisons, we still need to do replacement on them to
     * render their call sites correctly.
     * e.g.
     *     type alias A a = ...
     *     main : A ()
     * We replace the parameter of the AliasInfo in the return value of the main function with
     * TyUnit so that it renders as `A ()` rather than `A a`.
     */
    private fun replace(aliasInfo: AliasInfo?) = aliasInfo?.let { info ->
        info.copy(parameters = info.parameters.map { replace(it) }.optimizeReadOnlyList())
    }

    private fun replaceTuple(ty: TyTuple): TyTuple {
        return TyTuple(ty.types.map { replace(it) }.optimizeReadOnlyList(), replace(ty.alias))
    }

    private fun replaceFunction(ty: TyFunction): TyFunction {
        val parameters = ty.parameters.map { replace(it) }.optimizeReadOnlyList()
        return TyFunction(parameters, replace(ty.ret), replace(ty.alias)).uncurry()
    }

    private fun replaceUnion(ty: TyUnion): TyUnion {
        // fast path for common cases like Basics.Int
        if (ty.parameters.isEmpty() && ty.alias == null) return ty

        val parameters = ty.parameters.map { replace(it) }.optimizeReadOnlyList()
        return TyUnion(ty.module, ty.name, parameters, replace(ty.alias))
    }

    private fun replaceRecord(
            fields: Map<String, Ty>,
            baseTy: Ty?,
            alias: AliasInfo?,
            fieldReferences: RecordFieldReferenceTable,
            wasMutable: Boolean
    ): Ty {
        val oldBase = if (baseTy == null || baseTy !is TyVar) null else getReplacement(baseTy)
        val newBase = when (oldBase) {
            // If the base ty of the argument is a record, use its base ty, which might be null.
            is TyRecord -> oldBase.baseTy
            // If it wasn't substituted, leave it as-is
            null -> when {
                // Although if it's a record, we might need to make it immutable
                baseTy is MutableTyRecord && !keepRecordsMutable -> replace(baseTy)
                else -> baseTy
            }
            // If it's another variable, use it
            else -> oldBase
        }

        val baseFields = (oldBase as? TyRecord)?.fields.orEmpty()
        val baseFieldRefs = (oldBase as? TyRecord)?.fieldReferences

        // Make the new map as small as we can for these fields. HashMap always uses a power of two
        // element array, so we can't avoid all wasted memory.
        val initialCapacity = baseFields.size + fields.keys.count { it !in baseFields }
        val newFields = LinkedHashMap<String, Ty>(initialCapacity, 1f)

        // Don't use putAll here, since that function will resize the table to hold (size + 1) elements
        baseFields.forEach { (k, v) -> newFields[k] = v }
        fields.mapValuesTo(newFields) { (_, it) -> replace(it) }

        val newFieldReferences = when {
            baseFieldRefs == null || baseFieldRefs.isEmpty() -> fieldReferences
            fieldReferences.frozen -> fieldReferences + baseFieldRefs
            else -> {
                // The new record shares its references table with the old record. That allows us to track
                // references back to expressions inside nested declarations even when the record has been
                // freshened or replaced.
                fieldReferences.apply { addAll(baseFieldRefs) }
            }
        }

        return if (wasMutable && keepRecordsMutable) MutableTyRecord(newFields, newBase, newFieldReferences)
        else TyRecord(newFields, newBase, replace(alias), newFieldReferences)
    }

    // When we replace a var, the new ty may itself contain vars, and so we need to recursively
    // replace the ty before we can replace the var we were given as an argument.
    // After the recursive replacement, we avoid repeating work by storing the final ty and tracking
    // of the fact that its replacement is complete with the `hasBeenAccessed` flag.
    private fun getReplacement(key: TyVar): Ty? {
        val (hasBeenAccessed, storedTy) = replacements[key] ?: return when {
            freshen || varsToRemainRigid != null -> {
                if (key.rigid && (varsToRemainRigid == null || key in varsToRemainRigid)) null // never freshen rigid vars
                else TyVar(key.name, rigid = false).also { replacements[key] = true to it }
            }
            else -> null
        }

        if (hasBeenAccessed) return storedTy

        val replacedVal = replace(storedTy)
        replacements[key] = true to replacedVal
        return replacedVal
    }
}

// Copied from the stdlib, which doesn't apply this for `map` and `filter`
private fun <T> List<T>.optimizeReadOnlyList() = when (size) {
    0 -> emptyList()
    1 -> listOf(this[0])
    else -> this
}
