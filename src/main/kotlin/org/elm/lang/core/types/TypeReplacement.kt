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
        /**
         * Replace vars in [ty] according to [replacements].
         */
        fun replace(ty: Ty, replacements: Map<TyVar, Ty>): Ty {
            if (replacements.isEmpty()) return ty
            return TypeReplacement(replacements).replace(ty)
        }

        /**
         * Replace vars in [ty] according to [replacements].
         *
         * If the values of [replacements] can contain [TyVar]s that occur in the keys, use this
         * rather than [replace].
         *
         * This will perform replacement several times until there is nothing left to replace. Then
         * all remaining [TyVar]s in the type are replaced with new vars with the same name to
         * enforce scoping.
         */
        fun deepReplace(ty: Ty, replacements: Map<TyVar, Ty>): Ty {
            if (replacements.isEmpty()) return ty
            val tr = TypeReplacement(replacements)
            var newTy = ty
            repeat(5) {
                val next = tr.replace(newTy)
                if (next == newTy) {
                    return freshenVars(newTy)
                }
                newTy = next
            }
            // exceeded the recursion threshold, don't replace anything
            return ty
        }

        private fun freshenVars(ty: Ty): Ty {
            return TypeReplacement(MutableMapWithDefault { TyVar(it.name) }).replace(ty)
        }
    }

    private fun replace(ty: Ty): Ty = when (ty) {
        is TyVar -> replacements[ty] ?: ty
        is TyTuple -> TyTuple(ty.types.map { replace(it) }, replace(ty.alias))
        is TyFunction -> {
            val parameters = ty.parameters.map {
                replace(it)
            }
            val ret = replace(ty.ret)
            val alias = replace(ty.alias)
            TyFunction(parameters, ret, alias).uncurry()
        }
        is TyUnknown -> TyUnknown(replace(ty.alias))
        is TyUnion -> replaceUnion(ty)
        is TyRecord -> replaceRecord(ty)
        is TyUnit, TyInProgressBinding -> ty
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
        val replacedBase = if (ty.baseTy == null) null else replacements[ty.baseTy]
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
}


private class MutableMapWithDefault<K, V>(
        val map: MutableMap<K, V> = mutableMapOf(),
        private val default: (key: K) -> V
) : MutableMap<K, V> {
    override fun equals(other: Any?): Boolean = map.equals(other)
    override fun hashCode(): Int = map.hashCode()
    override fun toString(): String = map.toString()
    override val size: Int get() = map.size
    override fun isEmpty(): Boolean = map.isEmpty()
    override fun containsKey(key: K): Boolean = map.containsKey(key)
    override fun containsValue(value: @UnsafeVariance V): Boolean = map.containsValue(value)
    override fun get(key: K): V? = map.getOrPut(key) { default(key) }
    override val keys: MutableSet<K> get() = map.keys
    override val values: MutableCollection<V> get() = map.values
    override val entries: MutableSet<MutableMap.MutableEntry<K, V>> get() = map.entries

    override fun put(key: K, value: V): V? = map.put(key, value)
    override fun remove(key: K): V? = map.remove(key)
    override fun putAll(from: Map<out K, V>) = map.putAll(from)
    override fun clear() = map.clear()
}
