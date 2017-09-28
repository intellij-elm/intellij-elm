package org.elm.lang.core.psi.ext

import com.intellij.psi.PsiElement

interface ElmReferenceElement {
    val referenceNameElement: PsiElement

    val referenceName: String
}
