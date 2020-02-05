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

    val exposesAll: Boolean
        get() = stub?.exposesAll ?: (doubleDot != null)

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


    /**
     * Returns the module which the exposing list acts upon.
     *
     * An exposing list can occur in 2 different contexts:
     *  1. as part of an import, in which case the target module is given by the import
     *  2. as part of a module declaration at the top of a file, in which case the
     *     target module is the module itself
     */
    val targetModule: ElmModuleDeclaration?
        get() {
            val importClause = this.parentOfType<ElmImportClause>()
            return if (importClause != null) {
                importClause.reference.resolve() as? ElmModuleDeclaration
            } else {
                this.parentOfType<ElmModuleDeclaration>()
            }
        }
}


/**
 * Attempt to find an [exposed item][ElmExposedItemTag] that refers to [decl].
 *
 * Note that this will not find [union variants][ElmUnionVariant] since, as of Elm 0.19,
 * they are no longer exposed individually.
 *
 * If this function returns null, it doesn't mean that [decl] is not exposed; it just means
 * that it is not explicitly exposed by name (as opposed to `..` syntax)
 */
fun ElmExposingList.findMatchingItemFor(decl: ElmExposableTag): ElmExposedItemTag? =
        allExposedItems.find { it.reference?.isReferenceTo(decl) ?: false }


/**
 * Returns true if [element] is exposed by the receiver, either directly by name or
 * indirectly by the `..` "expose-all" syntax.
 */
fun ElmExposingList.exposes(element: ElmExposableTag): Boolean {
    val module = targetModule ?: return false
    if (element.elmFile.getModuleDecl() != module) {
        // this element does not belong to the target module
        return false
    }

    return when {
        module.exposesAll -> true
        element is ElmUnionVariant -> exposedTypeList.any { it.exposes(element) }
        else -> findMatchingItemFor(element) != null
    }
}


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
