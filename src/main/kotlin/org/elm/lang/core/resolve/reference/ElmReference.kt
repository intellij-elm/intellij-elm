package org.elm.lang.core.resolve.reference

import com.intellij.psi.PsiReference
import org.elm.lang.core.psi.ElmNamedElement

interface ElmReference: PsiReference {
    override fun resolve(): ElmNamedElement?
    override fun getVariants(): Array<ElmNamedElement>
}
