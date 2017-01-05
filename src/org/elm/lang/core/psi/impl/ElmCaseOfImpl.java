package org.elm.lang.core.psi.impl;

import org.elm.lang.core.psi.ElmPsiElement;
import org.elm.lang.core.psi.interfaces.ElmCaseOf;
import org.elm.lang.core.psi.interfaces.ElmVisitor;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;

public class ElmCaseOfImpl extends ElmPsiElement implements ElmCaseOf {
    public ElmCaseOfImpl(ASTNode node) {
        super(node);
    }

    public void accept(@NotNull PsiElementVisitor visitor) {
        if (visitor instanceof ElmVisitor) {
            ((ElmVisitor)visitor).visitPsiElement(this);
        }
        else super.accept(visitor);
    }
}
