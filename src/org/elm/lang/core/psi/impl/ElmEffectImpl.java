package org.elm.lang.core.psi.impl;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import org.elm.lang.core.psi.ElmPsiElement;
import org.elm.lang.core.psi.interfaces.ElmEffect;
import org.elm.lang.core.psi.interfaces.ElmVisitor;
import org.jetbrains.annotations.NotNull;

public class ElmEffectImpl extends ElmPsiElement implements ElmEffect {
    public ElmEffectImpl(ASTNode node) {
        super(node);
    }

    public void accept(@NotNull PsiElementVisitor visitor) {
        if (visitor instanceof ElmVisitor) {
            ((ElmVisitor)visitor).visitPsiElement(this);
        }
        else super.accept(visitor);
    }
}
