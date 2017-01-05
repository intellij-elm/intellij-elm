package org.elm.lang.core.psi;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import org.elm.lang.core.psi.interfaces.ElmVisitor;
import org.jetbrains.annotations.NotNull;


public abstract class ElmPsiElement extends ASTWrapperPsiElement {
    public ElmPsiElement(@NotNull ASTNode node) {
        super(node);
    }

    public void accept(@NotNull PsiElementVisitor visitor) {
        if (visitor instanceof ElmVisitor) {
            ((ElmVisitor)visitor).visitPsiElement(this);
        }
        else super.accept(visitor);
    }

}
