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
        private val freeze: Boolean,
        private val keepRecordsMutable: Boolean
) {
    companion object {
        /**
         * Replace vars in [ty] according to [replacements].
         *
         * @param varsToRemainRigid If given, all [TyVar]s will be replaced with flexible copies,
         *  except for vars that occur in this collection, which will be left unchanged.
         * @param freeze If true, make all record field reference tables frozen
         * @param keepRecordsMutable If false, [MutableTyRecord]s will be replaces with [TyRecord]s
         */
        fun replace(
                ty: Ty,
                replacements: Map<TyVar, Ty>,
                varsToRemainRigid: Collection<TyVar>? = null,
                freeze: Boolean = false,
                keepRecordsMutable: Boolean = false
        ): Ty {
            if (varsToRemainRigid == null && replacements.isEmpty()) return ty
            return TypeReplacement(
                    replacements,
                    freshen = false,
                    varsToRemainRigid = varsToRemainRigid,
                    freeze = freeze,
                    keepRecordsMutable = keepRecordsMutable
            ).replace(ty)
        }

        fun replace(
                ty: Ty,
                replacements: DisjointSet,
                varsToRemainRigid: Collection<TyVar>? = null,
                freeze: Boolean = false,
                keepRecordsMutable: Boolean = false
        ): Ty = replace(ty, replacements.asMap(), varsToRemainRigid, freeze, keepRecordsMutable)

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
        fun freshenVars(ty: Ty, freeze: Boolean = false): Ty {
            return TypeReplacement(emptyMap(), freshen = true, varsToRemainRigid = null, freeze = freeze, keepRecordsMutable = false).replace(ty)
        }

        /**
         * Make all type variables in a [ty] flexible.
         *
         * Type variables in function annotations are rigid when bound to parameters; in all other
         * cases vars are flexible. This function is used to make vars inferred as rigid into flexible
         * when calling functions.
         */
        fun flexify(ty: Ty): Ty {
            return TypeReplacement(emptyMap(), freshen = false, varsToRemainRigid = emptyList(), freeze = false, keepRecordsMutable = false).replace(ty)
        }

        /**
         * Freeze any fields references present in records in a [ty].
         *
         * Use this to prevent cached values from being altered.
         */
        fun freeze(ty: Ty): Ty {
            return TypeReplacement(emptyMap(), freshen = false, varsToRemainRigid = null, freeze = true, keepRecordsMutable = false).replace(ty)
        }
    }

    private fun Ty.wouldChange(): Boolean {
        val changeAllVars = freshen || varsToRemainRigid != null
        return anyVar(orIsUnfrozenRecord = freeze) { changeAllVars || it in replacements }
    }

    /** A map of var to (has been accessed, ty) */
    private val replacements = replacements.mapValuesTo(mutableMapOf()) { (_, v) -> false to v }

    fun replace(ty: Ty): Ty {
        // If we wouldn't change anything, return the original ty to avoid duplicating the object
        if (!ty.wouldChange()) return ty
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
        val replacedBase = if (baseTy == null || baseTy !is TyVar) null else getReplacement(baseTy)
        val newBaseTy = when (replacedBase) {
            // If the base ty of the argument is a record, use its base ty, which might be null.
            is TyRecord -> replacedBase.baseTy
            // If it wasn't substituted, leave it as-is
            null -> baseTy
            // If it's another variable, use it
            else -> replacedBase
        }

        val baseFields = (replacedBase as? TyRecord)?.fields.orEmpty()
        val baseFieldRefs = (replacedBase as? TyRecord)?.fieldReferences

        val newFields = LinkedHashMap<String, Ty>(baseFields.size + fields.size, 1f)
        newFields.putAll(baseFields)
        fields.mapValuesTo(newFields) { (_, it) -> replace(it) }

        var newFieldReferences = when {
            baseFieldRefs == null || baseFieldRefs.isEmpty() -> fieldReferences
            fieldReferences.frozen -> fieldReferences + baseFieldRefs
            else -> {
                // The new record shares its references table with the old record. That allows us to track
                // references back to expressions inside nested declarations even when the record has been
                // freshened or replaced.
                fieldReferences.apply { addAll(baseFieldRefs) }
            }
        }

        if (freeze) {
            newFieldReferences = newFieldReferences.freeze()
        }
        val newAlias = replace(alias)
        return if (wasMutable && keepRecordsMutable) MutableTyRecord(newFields, newBaseTy, newFieldReferences)
        else TyRecord(newFields, newBaseTy, newAlias, newFieldReferences)
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
