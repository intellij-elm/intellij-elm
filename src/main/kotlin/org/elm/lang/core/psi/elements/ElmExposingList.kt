package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.util.PsiTreeUtil
import org.elm.lang.core.psi.*
import org.elm.lang.core.psi.ElmTypes.*
import org.elm.lang.core.stubs.ElmExposingListStub


class ElmExposingList : ElmStubbedElement<ElmExposingListStub> {

    constructor(node: ASTNode) :
            super(node)

    constructor(stub: ElmExposingListStub, stubType: IStubElementType<*, *>) :
            super(stub, stubType)


    /**
     * If present, indicates that all names are exposed
     */
    val doubleDot: PsiElement?
        get() = findChildByType(DOUBLE_DOT)


    /**
     * Returns the opening parenthesis element.
     *
     * This will be non-null in a well-formed program.
     */
    val openParen: PsiElement?
        get() = findChildByType(LEFT_PARENTHESIS)


    /**
     * Returns the closing parenthesis element.
     *
     * This will be non-null in a well-formed program.
     */
    val closeParen: PsiElement?
        get() = findChildByType(RIGHT_PARENTHESIS)


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
 * Attempt to find an exposed item that refers to [decl]
 */
fun ElmExposingList.findMatchingItemFor(decl: ElmNameIdentifierOwner): ElmExposedItemTag? =
        allExposedItems.find { it.reference?.isReferenceTo(decl) ?: false }

/**
 * Returns true if [decl] is explicitly exposed by the receiver
 *
 * NOTE: This only handles the case where it is exposed directly by name.
 *       You must check separately to see if the receiver uses Elm's `..` syntax
 *       to expose *all* names.
 *
 * @see exposes
 */
fun ElmExposingList.explicitlyExposes(decl: ElmNameIdentifierOwner): Boolean =
        findMatchingItemFor(decl) != null


/**
 * Returns true if [decl] is exposed by the receiver, either directly by name or
 * indirectly by the `..` (expose-all) syntax.
 *
 * @see explicitlyExposes
 */
fun ElmExposingList.exposes(decl: ElmNameIdentifierOwner): Boolean =
        doubleDot != null || findMatchingItemFor(decl) != null


/**
 * Add a function/type to the exposing list, while ensuring that the necessary comma and whitespace are also added.
 *
 * TODO does this function really belong here in this file? Or should it be moved closer to intention actions?
 */
fun ElmExposingList.addItem(itemName: String) {
    // create a dummy import with multiple exposed values so that we can also extract the preceding comma and whitespace
    val import = ElmPsiFactory(project).createImportExposing("FooBar", listOf("foobar", itemName))
    val item = import.exposingList!!.allExposedItems.single { it.text == itemName }
    val prevComma = item.prevSiblings.first { it.elementType == ElmTypes.COMMA }
    addRangeBefore(prevComma, item, closeParen)
}


/**
 * Remove the item from the exposing list and make sure that whitespace and commas are correctly preserved.
 *
 * TODO does this function really belong here in this file? Or should it be moved closer to intention actions?
 */
fun ElmExposingList.removeItem(item: ElmExposedItemTag) {
    val nextVisibleLeaf = PsiTreeUtil.nextVisibleLeaf(item) ?: error("incomplete exposing list")
    require(allExposedItems.size > 1) { "Elm's parser requires that the exposing list between the parens is never empty" }

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