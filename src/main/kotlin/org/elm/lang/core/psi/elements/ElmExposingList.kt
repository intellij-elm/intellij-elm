package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.util.PsiTreeUtil
import org.elm.lang.core.psi.ElmStubbedElement
import org.elm.lang.core.psi.ElmTypes.DOUBLE_DOT
import org.elm.lang.core.stubs.ElmExposingListStub


class ElmExposingList : ElmStubbedElement<ElmExposingListStub> {

    constructor(node: ASTNode) :
            super(node)

    constructor(stub: ElmExposingListStub, stubType: IStubElementType<*, *>) :
            super(stub, stubType)


    val doubleDot: PsiElement?
        get() = findChildByType(DOUBLE_DOT)

    val exposedValueList: List<ElmExposedValue>
        get() = PsiTreeUtil.getStubChildrenOfTypeAsList(this, ElmExposedValue::class.java)

    val exposedTypeList: List<ElmExposedType>
        get() = PsiTreeUtil.getStubChildrenOfTypeAsList(this, ElmExposedType::class.java)

    val exposedOperatorList: List<ElmExposedOperator>
        get() = PsiTreeUtil.getStubChildrenOfTypeAsList(this, ElmExposedOperator::class.java)
}