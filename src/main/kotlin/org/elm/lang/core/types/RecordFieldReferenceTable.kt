package org.elm.lang.core.types

import org.elm.lang.core.psi.ElmNamedElement

/**
 * A table that tracks references for [TyRecord] fields. Can be [frozen] to prevent updates.
 */
data class RecordFieldReferenceTable(
        private var refsByField: MutableMap<String, MutableSet<ElmNamedElement>> = mutableMapOf(),
        val frozen: Boolean = false
) {
    /** Get all references for a [field], or an empty list if there are none. */
    fun get(field: String): List<ElmNamedElement> {
        return refsByField[field]?.toList().orEmpty()
    }

    /** Add all references from [other] to this table. Has no effect if [frozen]. */
    fun addAll(other: RecordFieldReferenceTable) {
        if (frozen || other.refsByField === this.refsByField) return
        for ((field, refs) in other.refsByField) {
            refsByField.getOrPut(field) { mutableSetOf() } += refs
        }
    }

    /** Return true if this table contains no references */
    fun isEmpty() = refsByField.isEmpty()

    /** Create a new table with references from this table and [other] */
    operator fun plus(other: RecordFieldReferenceTable): RecordFieldReferenceTable {
        return copy(frozen = false).apply { addAll(other) }
    }

    fun copy(frozen: Boolean = this.frozen): RecordFieldReferenceTable {
        return RecordFieldReferenceTable(
                refsByField.mapValuesTo(mutableMapOf()) { (_, v) -> v.toMutableSet() },
                frozen
        )
    }

    override fun toString(): String {
        return "<RecordFieldReferenceTable frozen=$frozen, refs=${refsByField.keys}>"
    }
}
