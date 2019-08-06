package org.elm.lang.core.resolve.reference

import org.elm.lang.core.psi.ElmNamedElement
import org.elm.lang.core.psi.ElmPsiElement
import org.elm.lang.core.resolve.ElmReferenceElement
import org.elm.lang.core.types.TyRecord
import org.elm.lang.core.types.findTy

// This reference isn't cached because it depends on type inference, which has different
// invalidation triggers than the reference cache.
abstract class RecordFieldReferenceBase<T : ElmReferenceElement>(
        element: T
) : ElmReferenceBase<T>(element) {
    companion object {
        inline fun <reified T : ElmReferenceElement> create(
                elem: T,
                crossinline findRecord: (T) -> ElmPsiElement?
        ) = object : RecordFieldReferenceBase<T>(elem) {
            override fun recordElement(): ElmPsiElement? {
                return findRecord(element)
            }
        }
    }
    // Unresolved reference errors are handled during type inference
    override fun isSoft(): Boolean = true

    final override fun multiResolve(): List<ElmNamedElement> {
        val ty = recordElement()?.findTy() as? TyRecord ?: return emptyList()
        return ty.fieldReferences.get(element.referenceName)
    }

    abstract fun recordElement(): ElmPsiElement?
}
