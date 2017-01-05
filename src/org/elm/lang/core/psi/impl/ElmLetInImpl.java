package org.elm.lang.core.psi.impl;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import org.elm.lang.core.psi.ElmPsiElement;
import org.elm.lang.core.psi.interfaces.ElmInnerValueDeclaration;
import org.elm.lang.core.psi.interfaces.ElmLetIn;
import org.elm.lang.core.psi.interfaces.ElmVisitor;
import org.jetbrains.annotations.NotNull;

import java.util.List;


public class ElmLetInImpl extends ElmPsiElement implements ElmLetIn {
    public ElmLetInImpl(ASTNode node) {
        super(node);
    }

    public void accept(@NotNull PsiElementVisitor visitor) {
        if (visitor instanceof ElmVisitor) {
            ((ElmVisitor)visitor).visitPsiElement(this);
        }
        else super.accept(visitor);
    }

    @Override
    @NotNull
    public List<ElmInnerValueDeclaration> getInnerValuesList() {
        return PsiTreeUtil.getChildrenOfTypeAsList(this, ElmInnerValueDeclaration.class);
    }
}
