package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.elm.lang.core.psi.ElmAtomTag
import org.elm.lang.core.psi.ElmFieldAccessPartTag
import org.elm.lang.core.psi.ElmFunctionCallTargetTag
import org.elm.lang.core.psi.ElmPsiElementImpl
import org.elm.lang.core.psi.ElmTypes.LOWER_CASE_IDENTIFIER

/**
 * Accessing one or more fields on a base record.
 *
 * e.g. `model.currentUser.name`
 */
class ElmFieldAccessExpr(node: ASTNode) : ElmPsiElementImpl(node), ElmAtomTag, ElmFunctionCallTargetTag, ElmFieldAccessPartTag {

    val leftExpr: ElmFieldAccessPartTag
        get() = findNotNullChildByClass(ElmFieldAccessPartTag::class.java)

    val lowerCaseIdentifier: PsiElement
        get() = findNotNullChildByType(LOWER_CASE_IDENTIFIER)
}