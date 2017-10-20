package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.util.PsiTreeUtil
import org.elm.lang.core.psi.ElmPsiElement




class ElmMixedCasePath(node: ASTNode) : ElmPsiElement(node) {

    val upperCaseIdList: List<ElmUpperCaseId>
        get() = PsiTreeUtil.getChildrenOfTypeAsList(this, ElmUpperCaseId::class.java)

}
