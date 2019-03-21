package org.elm.lang.core.types

/**
 * A map of [TyVar] to [Ty] that optimizes retrieval of chains of references
 *
 * https://en.wikipedia.org/wiki/Disjoint-set_data_structure
 */
class DisjointSet {
    private val map = mutableMapOf<TyVar, Ty>()

    operator fun set(key: TyVar, value: Ty) {
        map[key] = value
    }

    operator fun get(ty: Ty): Ty {
        if (ty !is TyVar) return ty

        var node: TyVar = ty
        var parent = map[node]

        // use Tarjan's algorithm for path compression to keep access near constant time
        while (parent is TyVar) {
            val grandparent = map[parent] ?: return parent
            map[node] = grandparent
            node = parent
            parent = grandparent
        }

        return parent ?: node
    }

    operator fun contains(ty: TyVar): Boolean = ty in map
    fun isEmpty(): Boolean = map.isEmpty()
    fun asMap(): Map<TyVar, Ty> = map
}
