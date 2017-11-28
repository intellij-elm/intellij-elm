package org.elm.lang.core.resolve.reference

import com.intellij.psi.PsiReference
import org.elm.lang.core.psi.ElmNamedElement
import org.elm.lang.core.psi.ElmPsiElement

interface ElmReference: PsiReference {
    override fun resolve(): ElmPsiElement?
    override fun getVariants(): Array<ElmNamedElement>
}
