package org.elm.lang.core.resolve.reference

import org.elm.lang.core.lookup.ElmLookup
import org.elm.lang.core.psi.ElmNamedElement
import org.elm.lang.core.psi.elements.ElmFieldAccessExpr
import org.elm.lang.core.psi.elements.ElmTypeAliasDeclaration
import org.elm.lang.core.types.findTy

// This reference isn't cached because it depends on type inference, which has different
// invalidation triggers than the reference cache. But the inference result and ElmLookup are both
// cached, so there are no expensive uncached operations.
class FieldAccessReference(
        element: ElmFieldAccessExpr
) : ElmReferenceBase<ElmFieldAccessExpr>(element) {
    // Unresolved reference errors are handled during type inference
    override fun isSoft(): Boolean = true
    override fun getVariants(): Array<ElmNamedElement> = emptyArray()

    override fun resolve(): ElmNamedElement? {
        val alias = element.targetExpr.findTy()?.alias ?: return null

        return ElmLookup.findFirstByNameAndModule<ElmTypeAliasDeclaration>(alias.name, alias.module, element.elmFile)
                ?.aliasedRecord
                ?.fieldTypeList
                ?.find { it.name == element.referenceName }
    }
}
