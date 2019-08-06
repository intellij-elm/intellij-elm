package org.elm.lang.core.types

import org.elm.lang.core.psi.ElmNamedElement

/**
 * A table that tracks references for [TyRecord] fields. Can be [frozen] to prevent updates.
 */
data class RecordFieldReferenceTable(
        private val refsByField: MutableMap<String, MutableSet<ElmNamedElement>> = mutableMapOf(),
        val frozen: Boolean = false
) {
    /** Get all references for a [field], or an empty list if there are none. */
    fun get(field: String): List<ElmNamedElement> {
        return refsByField[field]?.toList().orEmpty()
    }

    /** Add all references from [other] to this table. Has no effect if [frozen]. */
    fun addAll(other: RecordFieldReferenceTable) {
        if (frozen) return
        combine(other)
    }

    /** Return true if this table contains no references */
    fun isEmpty() = refsByField.isEmpty()

    /** Create a new table with references from this table and [other] */
    operator fun plus(other: RecordFieldReferenceTable): RecordFieldReferenceTable {
        return copy().apply { combine(other) }
    }

    private fun combine(other: RecordFieldReferenceTable) {
        for ((field, refs) in other.refsByField) {
            refsByField.getOrPut(field) { mutableSetOf() } += refs
        }
    }
}
