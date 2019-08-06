package org.elm.lang.core.resolve.reference

import org.elm.lang.core.psi.ElmNamedElement
import org.elm.lang.core.psi.elements.ElmFieldAccessExpr
import org.elm.lang.core.types.TyRecord
import org.elm.lang.core.types.findTy

// This reference isn't cached because it depends on type inference, which has different
// invalidation triggers than the reference cache.
class FieldAccessReference(
        element: ElmFieldAccessExpr
) : ElmReferenceBase<ElmFieldAccessExpr>(element) {
    // Unresolved reference errors are handled during type inference
    override fun isSoft(): Boolean = true

    override fun multiResolve(): List<ElmNamedElement> {
        val ty = element.targetExpr.findTy() as? TyRecord ?: return emptyList()
        return ty.fieldReferences.get(element.referenceName)
    }
}
