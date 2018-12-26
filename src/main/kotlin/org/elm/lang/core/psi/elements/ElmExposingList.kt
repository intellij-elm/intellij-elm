package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.util.PsiTreeUtil
import org.elm.lang.core.psi.*
import org.elm.lang.core.psi.ElmTypes.DOUBLE_DOT
import org.elm.lang.core.psi.ElmTypes.RIGHT_PARENTHESIS
import org.elm.lang.core.stubs.ElmExposingListStub


class ElmExposingList : ElmStubbedElement<ElmExposingListStub> {

    constructor(node: ASTNode) :
            super(node)

    constructor(stub: ElmExposingListStub, stubType: IStubElementType<*, *>) :
            super(stub, stubType)


    val doubleDot: PsiElement?
        get() = findChildByType(DOUBLE_DOT)


    /* TODO consider getting rid of the individual [exposedValueList], [exposedTypeList],
            and [exposedOperatorList] functions in favor of [allExposedItems]
    */

    val exposedValueList: List<ElmExposedValue>
        get() = PsiTreeUtil.getStubChildrenOfTypeAsList(this, ElmExposedValue::class.java)

    val exposedTypeList: List<ElmExposedType>
        get() = PsiTreeUtil.getStubChildrenOfTypeAsList(this, ElmExposedType::class.java)

    val exposedOperatorList: List<ElmExposedOperator>
        get() = PsiTreeUtil.getStubChildrenOfTypeAsList(this, ElmExposedOperator::class.java)


    /**
     * Returns the list of explicitly exposed items.
     *
     * e.g. `a` and `b` in `module Foo exposing (a, b)
     *
     * In the case where the module/import exposes *all* names (e.g. `module Foo exposing (..)`)
     * the returned list will be empty.
     *
     * This is the superset of [exposedValueList], [exposedTypeList], and [exposedOperatorList]
     */
    val allExposedItems: List<ElmExposedItemTag>
        get() = PsiTreeUtil.getStubChildrenOfTypeAsList(this, ElmExposedItemTag::class.java)

}


/**
 * Remove the item from the exposing list and make sure that whitespace and commas are correctly preserved.
 *
 * TODO does this function really belong here in this file? Or should it be moved closer to intention actions?
 */
fun ElmExposingList.removeItem(item: ElmExposedItemTag) {
    val nextVisibleLeaf = PsiTreeUtil.nextVisibleLeaf(item) ?: error("incomplete exposing list")

    if (nextVisibleLeaf.elementType == RIGHT_PARENTHESIS) {
        // delete any list junk that precedes the item to remove
        val junk = item.prevSiblings.adjacentJunk()
        deleteChildRange(junk.last(), junk.first())
    } else {
        // delete any list junk that follows the item to remove
        val junk = item.nextSiblings.adjacentJunk()
        deleteChildRange(junk.first(), junk.last())
    }

    // delete the exposed item itself
    item.delete()
}

// When removing an exposed item from the list, adjacent whitespace and comma should also be removed.
private fun Sequence<PsiElement>.adjacentJunk(): Sequence<PsiElement> =
        takeWhile { it is PsiWhiteSpace || it.elementType == ElmTypes.COMMA }