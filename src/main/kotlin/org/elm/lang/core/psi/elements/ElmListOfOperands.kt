package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.util.PsiTreeUtil
import org.elm.lang.core.psi.ElmPsiElementImpl


class ElmListOfOperands(node: ASTNode) : ElmPsiElementImpl(node) {


    // TODO [kl] cleanup this mess. The caller has no idea which states are valid here.

    fun getAnonymousFunctionList(): List<ElmAnonymousFunction> {
        return PsiTreeUtil.getChildrenOfTypeAsList(this, ElmAnonymousFunction::class.java)
    }

    fun getCaseOfList(): List<ElmCaseOf> {
        return PsiTreeUtil.getChildrenOfTypeAsList(this, ElmCaseOf::class.java)
    }

    fun getGlslCodeList(): List<ElmGlslCode> {
        return PsiTreeUtil.getChildrenOfTypeAsList(this, ElmGlslCode::class.java)
    }

    fun getIfElseList(): List<ElmIfElse> {
        return PsiTreeUtil.getChildrenOfTypeAsList(this, ElmIfElse::class.java)
    }

    fun getLetInList(): List<ElmLetIn> {
        return PsiTreeUtil.getChildrenOfTypeAsList(this, ElmLetIn::class.java)
    }

    fun getListList(): List<ElmList> {
        return PsiTreeUtil.getChildrenOfTypeAsList(this, ElmList::class.java)
    }

    fun getNonEmptyTupleList(): List<ElmNonEmptyTuple> {
        return PsiTreeUtil.getChildrenOfTypeAsList(this, ElmNonEmptyTuple::class.java)
    }

    fun getOperatorAsFunctionList(): List<ElmOperatorAsFunction> {
        return PsiTreeUtil.getChildrenOfTypeAsList(this, ElmOperatorAsFunction::class.java)
    }

    fun getParenthesedExpressionList(): List<ElmParenthesedExpression> {
        return PsiTreeUtil.getChildrenOfTypeAsList(this, ElmParenthesedExpression::class.java)
    }

    fun getRecordList(): List<ElmRecord> {
        return PsiTreeUtil.getChildrenOfTypeAsList(this, ElmRecord::class.java)
    }

    fun getUnitList(): List<ElmUnit> {
        return PsiTreeUtil.getChildrenOfTypeAsList(this, ElmUnit::class.java)
    }

    fun getFieldAccessList(): List<ElmFieldAccess> {
        return PsiTreeUtil.getChildrenOfTypeAsList(this, ElmFieldAccess::class.java)
    }
}
