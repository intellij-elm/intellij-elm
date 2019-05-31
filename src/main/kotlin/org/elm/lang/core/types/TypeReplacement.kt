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
        private val varsToRemainRigid: Collection<TyVar>?
) {
    companion object {
        /**
         * Replace vars in [ty] according to [replacements].
         *
         * @param varsToRemainRigid If given, all [TyVar]s will be replaced with flexible copies,
         *  except for vars that occur in this collection, which will be left unchanged.
         */
        fun replace(ty: Ty, replacements: Map<TyVar, Ty>, varsToRemainRigid: Collection<TyVar>? = null): Ty {
            if (varsToRemainRigid == null && replacements.isEmpty()) return ty
            return TypeReplacement(replacements, freshen = false, varsToRemainRigid = varsToRemainRigid).replace(ty)
        }

        fun replace(ty: Ty, replacements: DisjointSet, varsToRemainRigid: Collection<TyVar>? = null): Ty {
            return replace(ty, replacements.asMap(), varsToRemainRigid = varsToRemainRigid)
        }

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
            return TypeReplacement(emptyMap(), freshen = true, varsToRemainRigid = null).replace(ty)
        }

        /**
         * Make all type variables in a [ty] flexible.
         *
         * Type variables in function annotations are rigid when bound to parameters; in all other
         * cases vars are flexible. This function is used to make vars inferred as rigid into flexible
         * when calling functions.
         */
        fun flexify(ty: Ty): Ty {
            return TypeReplacement(emptyMap(), freshen = false, varsToRemainRigid = emptyList()).replace(ty)
        }
    }

    /** A map of var to (has been accessed, ty) */
    private val replacements = replacements.mapValuesTo(mutableMapOf()) { (_, v) -> false to v }

    fun replace(ty: Ty): Ty = when (ty) {
        is TyVar -> getReplacement(ty) ?: ty
        is TyTuple -> TyTuple(ty.types.map { replace(it) }, replace(ty.alias))
        is TyFunction -> replaceFunction(ty)
        is TyUnknown -> TyUnknown(replace(ty.alias))
        is TyUnion -> replaceUnion(ty)
        is TyRecord -> replaceRecord(ty)
        is TyUnit, TyInProgressBinding -> ty
        is MutableTyRecord -> replaceRecord(ty.toRecord())
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
        info.copy(parameters = info.parameters.map { replace(it) })
    }

    private fun replaceFunction(ty: TyFunction): TyFunction {
        val parameters = ty.parameters.map { replace(it) }
        return TyFunction(parameters, replace(ty.ret), replace(ty.alias)).uncurry()
    }

    private fun replaceUnion(ty: TyUnion): TyUnion {
        val parameters = ty.parameters.map { replace(it) }
        return TyUnion(ty.module, ty.name, parameters, replace(ty.alias))
    }

    private fun replaceRecord(ty: TyRecord): Ty {
        val replacedBase = if (ty.baseTy == null || ty.baseTy !is TyVar) null else getReplacement(ty.baseTy)
        val newBaseTy = when (replacedBase) {
            // If the base ty of the argument is a record, use it's base ty, which might be null.
            is TyRecord -> replacedBase.baseTy
            // If it wasn't substituted, leave it as-is
            null -> ty.baseTy
            // If it's another variable, use it
            else -> replacedBase
        }

        val declaredFields = ty.fields.mapValues { (_, it) -> replace(it) }
        val baseFields = (replacedBase as? TyRecord)?.fields.orEmpty()

        return TyRecord(baseFields + declaredFields, newBaseTy, replace(ty.alias))
    }

    // When we replace a var, the new ty may itself contain vars, and so we need to recursively
    // replace the ty before we can replace the var we were given as an argument.
    // After the recursive replacement, we avoid repeating work by storing the final ty and tracking
    // of the fact that it's replacement is complete with the `hasBeenAccessed` flag.
    private fun getReplacement(key: TyVar): Ty? {
        if (key !in replacements && (freshen || varsToRemainRigid != null)) {
            if (key.rigid && (varsToRemainRigid == null || key in varsToRemainRigid)) return null // never freshen rigid vars

            val ty = TyVar(key.name, rigid = false)
            replacements[key] = true to ty
            return ty
        }

        val v = replacements[key] ?: return null
        val (hasBeenAccessed, storedTy) = v
        if (hasBeenAccessed) return storedTy

        val replacedVal = replace(storedTy)
        replacements[key] = true to replacedVal
        return replacedVal
    }
}
