package org.elm.lang.core.psi.impl;

import com.intellij.lang.ASTNode;
import org.elm.lang.core.psi.ElmPsiElement;
import org.elm.lang.core.psi.interfaces.ElmLowerCasePath;


public class ElmLowerCasePathImpl extends ElmPsiElement implements ElmLowerCasePath {
    public ElmLowerCasePathImpl(ASTNode node) {
        super(node);
    }
}
