package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.util.PsiTreeUtil
import org.elm.lang.core.psi.ElmPsiElementImpl


class ElmPattern(node: ASTNode) : ElmPsiElementImpl(node) {

    val recordPatternList: List<ElmRecordPattern>
        get() = PsiTreeUtil.getChildrenOfTypeAsList(this, ElmRecordPattern::class.java)

    val patternList: List<ElmPattern>
        get() = PsiTreeUtil.getChildrenOfTypeAsList(this, ElmPattern::class.java)

    val unionPatternList: List<ElmUnionPattern>
        get() = PsiTreeUtil.getChildrenOfTypeAsList(this, ElmUnionPattern::class.java)

    val unitList: List<ElmUnit>
        get() = PsiTreeUtil.getChildrenOfTypeAsList(this, ElmUnit::class.java)

    val lowerPatternList: List<ElmLowerPattern>
        get() = PsiTreeUtil.getChildrenOfTypeAsList(this, ElmLowerPattern::class.java)

    val patternAs: ElmPatternAs?
        get() = findChildByClass(ElmPatternAs::class.java)
}
