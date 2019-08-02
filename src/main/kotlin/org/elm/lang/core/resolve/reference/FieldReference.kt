package org.elm.lang.core.resolve.reference

import org.elm.lang.core.psi.ElmNamedElement
import org.elm.lang.core.psi.elements.ElmField
import org.elm.lang.core.psi.elements.ElmRecordExpr
import org.elm.lang.core.psi.parentOfType
import org.elm.lang.core.types.TyRecord
import org.elm.lang.core.types.findTy

// This reference isn't cached because it depends on type inference, which has different
// invalidation triggers than the reference cache.
class FieldReference(
        element: ElmField
) : ElmReferenceBase<ElmField>(element) {
    // Unresolved reference errors are handled during type inference
    override fun isSoft(): Boolean = true
    override fun getVariants(): Array<ElmNamedElement> = emptyArray()

    // TODO[aj] : This doesn't seem to work for |>. Maybe replacement is different somehow, or maybe
    // its from the chained vars?
    override fun resolve(): ElmNamedElement? {
        val recordExpr = element.parentOfType<ElmRecordExpr>() ?: return null
        val ty = recordExpr.findTy() as? TyRecord ?: return null
        return ty.fieldReferences[element.referenceName]
    }
}
