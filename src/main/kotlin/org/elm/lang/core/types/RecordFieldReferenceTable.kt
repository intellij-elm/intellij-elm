package org.elm.lang.core.types

import com.intellij.util.containers.SmartHashSet
import org.elm.lang.core.psi.ElmNamedElement
import org.elm.lang.core.psi.elements.ElmFieldType

/**
 * A table that tracks references for [TyRecord] fields. Can be [frozen] to prevent updates.
 */
class RecordFieldReferenceTable private constructor(
        private var refsByField: MutableMap<String, MutableSet<ElmNamedElement>>
) {
    constructor() : this(HashMap(4, 1f))

    companion object {
        fun fromElements(fieldElements: Collection<ElmFieldType>): RecordFieldReferenceTable {
            return RecordFieldReferenceTable(fieldElements.associateTo(HashMap(fieldElements.size, 1f)) {
                it.name to newSet(1).apply { add(it) }
            })
        }
    }

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
            val set = refsByField.getOrPut(field) { newSet(refs.size) }
            set.addAll(refs)
        }
    }

    /** Return true if this table contains no references */
    fun isEmpty() = refsByField.isEmpty()

    /** Create a new table with references from this table and [other] */
    operator fun plus(other: RecordFieldReferenceTable): RecordFieldReferenceTable {
        val initialCapacity = refsByField.size + other.refsByField.keys.count { it !in refsByField }
        val newRefs = HashMap<String, MutableSet<ElmNamedElement>>(initialCapacity, 1f)
        refsByField.mapValuesTo(newRefs) { (field, set) ->
            val otherSet = other.refsByField[field].orEmpty()
            val initialSetCapacity = set.size + otherSet.count { it !in set }
            newSet(initialSetCapacity).apply { addAll(otherSet) }
        }
        other.refsByField.forEach { (k, s) ->
            newRefs.getOrPut(k) { newSet(s.size).apply { addAll(s) } }
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

private fun newSet(initialCapacity: Int): MutableSet<ElmNamedElement> {
    // > 99% of these sets have 1 element, so we use a set optimized for that use case
    return  SmartHashSet(initialCapacity, 1f)
}
