package org.elm.lang.core.resolve

import com.intellij.psi.PsiElement
import org.elm.lang.core.psi.ElmPsiElement
import org.elm.lang.core.resolve.reference.ElmReference


interface ElmReferenceElement : ElmPsiElement {
    val referenceNameElement: PsiElement

    val referenceName: String

    override fun getReference(): ElmReference
    override fun getReferences(): Array<ElmReference>
}
