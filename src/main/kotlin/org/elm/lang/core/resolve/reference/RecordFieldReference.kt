package org.elm.lang.core.resolve.reference

import org.elm.lang.core.psi.ElmNamedElement
import org.elm.lang.core.psi.ElmPsiElement
import org.elm.lang.core.resolve.ElmReferenceElement
import org.elm.lang.core.types.Ty
import org.elm.lang.core.types.TyRecord
import org.elm.lang.core.types.findTy

/**
 * A reference to a field in a record.
 *
 * May resolve to multiple locations in the case of functions that are called multiple times with
 * different records.
 *
 * This reference isn't cached because it depends on type inference, which has different
 * invalidation triggers than the reference cache.
 */
abstract class RecordFieldReference<T : ElmReferenceElement>(
        element: T
) : ElmReferenceBase<T>(element) {
    companion object {
        inline fun <reified T : ElmReferenceElement> fromElement(e: T, crossinline f: (T) -> ElmPsiElement?) =
                object : RecordFieldReference<T>(e) {
                    override fun getTy(): Ty? {
                        return f(element)?.findTy()
                    }
                }
    }
    // Errors for these references are handled during type inference
    override fun isSoft(): Boolean = true

    final override fun multiResolve(): List<ElmNamedElement> {
        val ty = getTy() as? TyRecord ?: return emptyList()
        return ty.fieldReferences.get(element.referenceName)
    }

    abstract fun getTy(): Ty?
}
