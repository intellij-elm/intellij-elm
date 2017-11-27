package org.elm.lang.core.psi

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement


interface ElmPsiElement : PsiElement {
    /**
     * Get the file containing this element as an [ElmFile]
     */
    val elmFile: ElmFile
}


abstract class ElmPsiElementImpl(node: ASTNode) : ASTWrapperPsiElement(node), ElmPsiElement {

    override val elmFile: ElmFile
        get() = containingFile as ElmFile
}
