package org.elm.lang.core.resolve

import com.intellij.psi.PsiReference
import org.elm.lang.core.psi.ElmPsiElement


interface ElmReference: PsiReference {

    override fun getElement(): ElmPsiElement

    override fun resolve(): ElmPsiElement?

    // resolve multiple variants so that we can also provide code completion
    fun multiResolve(): List<ElmPsiElement>
}
