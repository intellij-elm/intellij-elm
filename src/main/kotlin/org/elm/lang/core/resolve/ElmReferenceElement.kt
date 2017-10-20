package org.elm.lang.core.resolve

import com.intellij.psi.PsiElement


// TODO [kl] make a base interface for Elm Psi elements and have this extend that interface
interface ElmReferenceElement: PsiElement {
    val referenceNameElement: PsiElement

    val referenceName: String

    override fun getReference(): ElmReference
}
