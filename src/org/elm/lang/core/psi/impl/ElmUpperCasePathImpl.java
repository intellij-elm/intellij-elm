package org.elm.lang.core.psi.impl;

import com.intellij.lang.ASTNode;
import com.intellij.psi.util.PsiTreeUtil;
import org.elm.lang.core.psi.ElmPsiElement;
import org.elm.lang.core.psi.interfaces.ElmUpperCaseId;
import org.elm.lang.core.psi.interfaces.ElmUpperCasePath;

import java.util.List;

public class ElmUpperCasePathImpl extends ElmPsiElement implements ElmUpperCasePath {
    public ElmUpperCasePathImpl(ASTNode node) {
        super(node);
    }

    public List<ElmUpperCaseId> getUpperCaseIdList() {
        return PsiTreeUtil.getChildrenOfTypeAsList(this, ElmUpperCaseId.class);
    }
}
