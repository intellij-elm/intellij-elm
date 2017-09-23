package org.elm.lang.core.psi.interfaces;

import com.intellij.psi.PsiElement;

import java.util.List;

public interface ElmMixedCasePath extends PsiElement {
    List<ElmUpperCaseId> getUpperCaseIdList();
}
