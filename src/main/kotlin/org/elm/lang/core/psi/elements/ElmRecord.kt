package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.elm.lang.core.psi.ElmPsiElementImpl
import org.elm.lang.core.psi.ElmTypes.LOWER_CASE_IDENTIFIER


/**
 * A record literal value
 *
 * e.g. { name = "George", age = 42 }
 */
class ElmRecord(node: ASTNode) : ElmPsiElementImpl(node) {

    /**
     * The name of the existing record which is to be updated, or null
     * if this record literal is constructing a completely new value.
     *
     * e.g. person in `{ person | age = person.age + 1 }`
     */
    val baseRecordIdentifier: PsiElement?
        get() = findChildByType(LOWER_CASE_IDENTIFIER)

    /**
     * The fields to set in the new record.
     */
    val fieldList: List<ElmField>
        get() = PsiTreeUtil.getChildrenOfTypeAsList(this, ElmField::class.java)
}
