/*
The MIT License (MIT)

Derived from intellij-rust
Copyright (c) 2015 Aleksey Kladov, Evgeny Kurbatsky, Alexey Kudinkin and contributors
Copyright (c) 2016 JetBrains

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */

package org.elm.lang.core.psi

import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.PsiTreeUtil


val PsiElement.ancestors: Sequence<PsiElement> get() = generateSequence(this) { it.parent }


inline fun <reified T : PsiElement> PsiElement.parentOfType(strict: Boolean = true, minStartOffset: Int = -1): T? =
        PsiTreeUtil.getParentOfType(this, T::class.java, strict, minStartOffset)

inline fun <reified T : PsiElement> PsiElement.parentOfType(strict: Boolean = true, stopAt: Class<out PsiElement>): T? =
        PsiTreeUtil.getParentOfType(this, T::class.java, strict, stopAt)

inline fun <reified T : PsiElement> PsiElement.contextOfType(strict: Boolean = true): T? =
        PsiTreeUtil.getContextOfType(this, T::class.java, strict)

inline fun <reified T : PsiElement> PsiElement.childOfType(strict: Boolean = true): T? =
        PsiTreeUtil.findChildOfType(this, T::class.java, strict)

inline fun <reified T : PsiElement> PsiElement.descendantsOfType(): Collection<T> =
        PsiTreeUtil.findChildrenOfType(this, T::class.java)

/**
 * Finds first sibling that is neither comment, nor whitespace before given element.
 */
fun PsiElement?.getPrevNonCommentSibling(): PsiElement? =
        PsiTreeUtil.skipSiblingsBackward(this, PsiWhiteSpace::class.java, PsiComment::class.java)

/**
 * Finds first sibling that is neither comment, nor whitespace after given element.
 */
fun PsiElement?.getNextNonCommentSibling(): PsiElement? =
        PsiTreeUtil.skipSiblingsForward(this, PsiWhiteSpace::class.java, PsiComment::class.java)
