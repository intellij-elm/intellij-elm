package org.elm.lang.core.psi.impl;

import com.intellij.lang.ASTNode;
import com.intellij.psi.util.PsiTreeUtil;
import org.elm.lang.core.psi.ElmPsiElement;
import org.elm.lang.core.psi.interfaces.ElmMixedCasePath;
import org.elm.lang.core.psi.interfaces.ElmUpperCaseId;

import java.util.List;


public class ElmMixedCasePathImpl extends ElmPsiElement implements ElmMixedCasePath {
    public ElmMixedCasePathImpl(ASTNode node) {
        super(node);
    }



    public List<ElmUpperCaseId> getUpperCaseIdList() {
        return PsiTreeUtil.getChildrenOfTypeAsList(this, ElmUpperCaseId.class);
    }

}
