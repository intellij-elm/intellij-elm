package org.elm.lang.core.types

/**
 * This class performs deep replacement of a set of [TyVar]s in a [Ty] with a set of new types,
 * which could also be [TyVar]s.
 *
 * It relies on the fact that [TyVar]s can be compared by identity. Vars in different scopes must
 * compare unequal, even if they have the same name.
 */
class TypeReplacement(
        // A map of variables that should be replaced to the ty to replace them with
        private val replacements: Map<TyVar, Ty>
) {
    companion object {
        fun replace(ty: Ty, replacements: Map<TyVar, Ty>): Ty {
            return TypeReplacement(replacements).replace(ty)
        }

        fun replace(ty: Ty, params: List<TyVar>, args: List<Ty>): Ty {
            require(params.size == args.size) { "params and args size differ: ${params.size}, ${args.size}" }
            return replace(ty, params.zip(args).toMap())
        }
    }

    private fun replace(ty: Ty): Ty = when (ty) {
        is TyVar -> replacements[ty] ?: ty
        is TyTuple -> TyTuple(ty.types.map { replace(it) }, replace(ty.alias))
        is TyFunction -> TyFunction(ty.parameters.map { replace(it) }, replace(ty.ret), replace(ty.alias))
        is TyUnknown -> TyUnknown(replace(ty.alias))
        is TyUnion -> replaceUnion(ty)
        is TyRecord -> replaceRecord(ty)
        is TyUnit, TyInProgressBinding, is TyInfer -> ty
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

    private fun replaceUnion(ty: TyUnion): TyUnion {
        val parameters = ty.parameters.map { replace(it) }
        return TyUnion(ty.module, ty.name, parameters, replace(ty.alias))
    }

    private fun replaceRecord(ty: TyRecord): Ty {
        val baseTy = replacements[ty.baseTy]
        val baseFields = (baseTy as? TyRecord)?.fields.orEmpty()

        val declaredFields = ty.fields.mapValues { (_, it) -> replace(it) }

        val newBaseTy = when (baseTy) {
            // If the base ty of the argument is a record, use it's base ty, which might be null.
            is TyRecord -> baseTy.baseTy
            // If it's another variable, use it as-is
            else -> baseTy
        }
        return TyRecord(baseFields + declaredFields, newBaseTy, replace(ty.alias))
    }
}

