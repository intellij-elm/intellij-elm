package org.elm.lang.core.types

import org.elm.lang.core.psi.ElmNamedElement

/**
 * A table that tracks references for [TyRecord] fields. Can be [frozen] to prevent updates.
 */
class RecordFieldReferenceTable(
        private var refsByField: MutableMap<String, MutableSet<ElmNamedElement>> = LinkedHashMap(4, 1f)
) {
    var frozen = false
        private set

    /** Get all references for a [field], or an empty list if there are none. */
    fun get(field: String): List<ElmNamedElement> {
        return refsByField[field]?.toList().orEmpty()
    }

    /** Add all references from [other] to this table. Has no effect if [frozen]. */
    fun addAll(other: RecordFieldReferenceTable) {
        if (frozen || other.refsByField === this.refsByField) return
        for ((field, refs) in other.refsByField) {
            refsByField.getOrPut(field) { HashSet(refs.size, 1f) } += refs
        }
    }

    /** Return true if this table contains no references */
    fun isEmpty() = refsByField.isEmpty()

    /** Create a new table with references from this table and [other] */
    operator fun plus(other: RecordFieldReferenceTable): RecordFieldReferenceTable {
        val newRefs = LinkedHashMap<String, MutableSet<ElmNamedElement>>(refsByField.size + other.refsByField.size, 1f)
        refsByField.mapValuesTo(newRefs) { (field, set) ->
            val otherSet = other.refsByField[field].orEmpty()
            HashSet<ElmNamedElement>(set.size + (otherSet.size), 1f).apply { addAll(otherSet) }
        }
        other.refsByField.forEach { (k, s) ->
            newRefs.getOrPut(k) { HashSet<ElmNamedElement>(s.size, 1f).apply { addAll(s) } }
        }
        return RecordFieldReferenceTable(newRefs)
    }

    /** Prevent further modifications to this table. Tables cannot be unfrozen. */
    fun freeze() {
        frozen = true
    }

    override fun toString(): String {
        return "<RecordFieldReferenceTable frozen=$frozen, refs=${refsByField.keys}>"
    }
}
