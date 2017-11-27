package org.elm.lang.core.resolve

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import org.elm.lang.core.psi.ElmPsiElement


interface ElmReferenceElement: ElmPsiElement {
    val referenceNameElement: PsiElement

    val referenceName: String

    override fun getReference(): PsiReference
}
