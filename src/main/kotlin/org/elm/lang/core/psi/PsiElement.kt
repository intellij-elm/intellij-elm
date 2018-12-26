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
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtilCore
import org.elm.lang.core.lexer.ElmLayoutLexer
import org.elm.lang.core.psi.ElmTypes.VIRTUAL_END_DECL
import org.elm.lang.core.stubs.ElmFileStub
import kotlin.reflect.KClass


val PsiElement.descendants: Sequence<PsiElement> get() = directChildren.flatMap { sequenceOf(it) + it.descendants }
val PsiElement.ancestors: Sequence<PsiElement> get() = generateSequence(this) { it.parent }
val PsiElement.ancestorsStrict: Sequence<PsiElement> get() = ancestors.drop(1)
val PsiElement.prevSiblings: Sequence<PsiElement> get() = generateSequence(prevSibling) { it.prevSibling }
val PsiElement.nextSiblings: Sequence<PsiElement> get() = generateSequence(nextSibling) { it.nextSibling }
val PsiElement.prevLeaves: Sequence<PsiElement> get() = generateSequence(PsiTreeUtil.prevLeaf(this)) { PsiTreeUtil.prevLeaf(it) }
val PsiElement.nextLeaves: Sequence<PsiElement> get() = generateSequence(PsiTreeUtil.nextLeaf(this)) { PsiTreeUtil.nextLeaf(it) }
val PsiElement.directChildren: Sequence<PsiElement> get() = generateSequence(firstChild) { it.nextSibling }
val Sequence<PsiElement>.withoutWs get() = filter { it !is PsiWhiteSpace && it.elementType != VIRTUAL_END_DECL }
val Sequence<PsiElement>.withoutWsOrComments get() = withoutWs.filter { it !is PsiComment }
val Sequence<PsiElement>.withoutErrors get() = filter { it !is PsiErrorElement }


/**
 * Extracts node's element type, being careful not to switch from stubs to AST inadvertently.
 */
val PsiElement.elementType: IElementType
    get() = if (this is ElmFile) ElmFileStub.Type else PsiUtilCore.getElementType(this)

inline fun <reified T : PsiElement> PsiElement.parentOfType(strict: Boolean = true, minStartOffset: Int = -1): T? =
        PsiTreeUtil.getParentOfType(this, T::class.java, strict, minStartOffset)

inline fun <reified T : PsiElement> PsiElement.parentOfType(strict: Boolean = true, stopAt: Class<out PsiElement>): T? =
        PsiTreeUtil.getParentOfType(this, T::class.java, strict, stopAt)

fun <T : PsiElement> PsiElement.parentOfType(vararg classes: KClass<out T>): T? {
    return PsiTreeUtil.getParentOfType(this, *classes.map { it.java }.toTypedArray())
}

inline fun <reified T : PsiElement> PsiElement.contextOfType(strict: Boolean = true): T? =
        PsiTreeUtil.getContextOfType(this, T::class.java, strict)

inline fun <reified T : PsiElement> PsiElement.childOfType(strict: Boolean = true): T? =
        PsiTreeUtil.findChildOfType(this, T::class.java, strict)

inline fun <reified T : PsiElement> PsiElement.descendantsOfType(): Collection<T> =
        PsiTreeUtil.findChildrenOfType(this, T::class.java)

/**
 * Computes the start offset of the receiver relative to [owner].
 *
 * The receiver must be a descendant of [owner]
 */
fun PsiElement.offsetIn(owner: PsiElement): Int =
        ancestors.takeWhile { it != owner }.sumBy { it.startOffsetInParent }


// Elm-specific helpers

/**
 * Returns true if this element is a virtual token synthesized by [ElmLayoutLexer]
 */
fun PsiElement.isVirtualLayoutToken(): Boolean =
        elementType in ELM_VIRTUAL_TOKENS
