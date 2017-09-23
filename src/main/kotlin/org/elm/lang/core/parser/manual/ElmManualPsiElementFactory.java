package org.elm.lang.core.parser.manual;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import org.elm.lang.core.psi.impl.*;
import org.elm.lang.core.psi.ElmTypes;

public class ElmManualPsiElementFactory {

    public static PsiElement createElement(ASTNode node) {
        IElementType type = node.getElementType();
        if (type == ElmTypes.CASE_OF) {
            return new ElmCaseOfImpl(node);
        }
        if (type == ElmTypes.LET_IN) {
            return new ElmLetInImpl(node);
        }
        if (type == ElmTypes.UPPER_CASE_PATH) {
            return new ElmUpperCasePathImpl(node);
        }
        if (type == ElmTypes.LOWER_CASE_PATH) {
            return new ElmLowerCasePathImpl(node);
        }
        if (type == ElmTypes.MIXED_CASE_PATH) {
            return new ElmMixedCasePathImpl(node);
        }
        if (type == ElmTypes.FIELD_ACCESS) {
            return new ElmFieldAccessImpl(node);
        }
        if (type == ElmTypes.EFFECT) {
            return new ElmEffectImpl(node);
        }
        return null;
    }
}