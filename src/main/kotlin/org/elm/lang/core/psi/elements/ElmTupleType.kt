package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.util.PsiTreeUtil
import org.elm.lang.core.psi.ElmParametricTypeRefParameterTag
import org.elm.lang.core.psi.ElmPsiElementImpl
import org.elm.lang.core.psi.ElmTypeRefSegmentTag
import org.elm.lang.core.psi.ElmUnionMemberParameterTag


class ElmTupleType(node: ASTNode) : ElmPsiElementImpl(node), ElmUnionMemberParameterTag, ElmParametricTypeRefParameterTag, ElmTypeRefSegmentTag {

    val typeRefList: List<ElmTypeRef>
        get() = PsiTreeUtil.getChildrenOfTypeAsList(this, ElmTypeRef::class.java)

    val unitExpr: ElmUnitExpr?
        get() = findChildByClass(ElmUnitExpr::class.java)

}
