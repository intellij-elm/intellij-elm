package org.elm.lang.core.completion

import com.intellij.patterns.*
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext

inline fun <reified I : PsiElement> psiElement(): PsiElementPattern.Capture<I> {
    return PlatformPatterns.psiElement(I::class.java)
}

val PsiElement.leftLeaves: Sequence<PsiElement> get() = generateSequence(this, PsiTreeUtil::prevLeaf).drop(1)

val PsiElement.rightSiblings: Sequence<PsiElement> get() = generateSequence(this.nextSibling) { it.nextSibling }

val PsiElement.leftSiblings: Sequence<PsiElement> get() = generateSequence(this.prevSibling) { it.prevSibling }
