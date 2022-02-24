package org.elm.lang.core.resolve.reference

import com.intellij.psi.PsiPolyVariantReference
import org.elm.lang.core.psi.ElmNamedElement

interface ElmReference : PsiPolyVariantReference {
    override fun resolve(): ElmNamedElement?
    fun multiResolve(): List<ElmNamedElement>
}
