package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.elm.lang.core.psi.ElmPsiElementImpl
import org.elm.lang.core.psi.ElmTypes.DOUBLE_DOT

class ElmExposingList(node: ASTNode) : ElmPsiElementImpl(node) {

    val doubleDot: PsiElement?
        get() = findChildByType(DOUBLE_DOT)

    val exposedValueList: List<ElmExposedValue>
        get() = PsiTreeUtil.getChildrenOfTypeAsList(this, ElmExposedValue::class.java)

    val exposedTypeList: List<ElmExposedType>
        get() = PsiTreeUtil.getChildrenOfTypeAsList(this, ElmExposedType::class.java)

    val operatorAsFunctionList: List<ElmOperatorAsFunction>
        get() = PsiTreeUtil.getChildrenOfTypeAsList(this, ElmOperatorAsFunction::class.java)
}