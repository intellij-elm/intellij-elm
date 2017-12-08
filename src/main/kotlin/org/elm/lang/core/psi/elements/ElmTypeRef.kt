
package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.util.PsiTreeUtil
import org.elm.lang.core.psi.ElmPsiElementImpl


class ElmTypeRef(node: ASTNode) : ElmPsiElementImpl(node) {

    val upperPathTypeRefList: List<ElmUpperPathTypeRef>
        get() = PsiTreeUtil.getChildrenOfTypeAsList(this, ElmUpperPathTypeRef::class.java)

    val typeVariableRefList: List<ElmTypeVariableRef>
        get() = PsiTreeUtil.getChildrenOfTypeAsList(this, ElmTypeVariableRef::class.java)

    val recordTypeList: List<ElmRecordType>
        get() = PsiTreeUtil.getChildrenOfTypeAsList(this, ElmRecordType::class.java)

    val tupleTypeList: List<ElmTupleType>
        get() = PsiTreeUtil.getChildrenOfTypeAsList(this, ElmTupleType::class.java)

    val parametricTypeRefList: List<ElmParametricTypeRef>
        get() = PsiTreeUtil.getChildrenOfTypeAsList(this, ElmParametricTypeRef::class.java)

    val typeRefList: List<ElmTypeRef>
        get() = PsiTreeUtil.getChildrenOfTypeAsList(this, ElmTypeRef::class.java)
}
