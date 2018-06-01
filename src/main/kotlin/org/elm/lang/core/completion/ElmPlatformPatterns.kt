package org.elm.lang.core.completion

import com.intellij.patterns.*
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext

inline fun <reified I : PsiElement> psiElement(): PsiElementPattern.Capture<I> {
    return PlatformPatterns.psiElement(I::class.java)
}

/**
 * Similar with [TreeElementPattern.afterSiblingSkipping]
 * but it uses [PsiElement.getPrevSibling] to get previous sibling elements
 * instead of [PsiElement.getChildren].
 */
fun <T : PsiElement, Self : PsiElementPattern<T, Self>> PsiElementPattern<T, Self>.withPrevSiblingSkipping(
        skip: ElementPattern<out T>,
        pattern: ElementPattern<out T>
): Self = with("withPrevSiblingSkipping") {
    val sibling = it.leftSiblings.dropWhile { skip.accepts(it) }
            .firstOrNull() ?: return@with false
    pattern.accepts(sibling)
}

private fun <T, Self : ObjectPattern<T, Self>> ObjectPattern<T, Self>.with(name: String, cond: (T) -> Boolean): Self =
        with(object : PatternCondition<T>(name) {
            override fun accepts(t: T, context: ProcessingContext?): Boolean = cond(t)
        })

val PsiElement.leftLeaves: Sequence<PsiElement> get() = generateSequence(this, PsiTreeUtil::prevLeaf).drop(1)

val PsiElement.rightSiblings: Sequence<PsiElement> get() = generateSequence(this.nextSibling) { it.nextSibling }

val PsiElement.leftSiblings: Sequence<PsiElement> get() = generateSequence(this.prevSibling) { it.prevSibling }
