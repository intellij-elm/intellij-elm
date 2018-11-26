package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.util.PsiTreeUtil
import org.elm.lang.core.psi.ElmAtomTag
import org.elm.lang.core.psi.ElmFieldAccessTargetTag
import org.elm.lang.core.psi.ElmPsiElementImpl


/**
 * A record literal value
 *
 * e.g. { name = "George", age = 42 }
 */
class ElmRecordExpr(node: ASTNode) : ElmPsiElementImpl(node), ElmAtomTag, ElmFieldAccessTargetTag {

    /**
     * The name of the existing record which is to be updated, or null
     * if this record literal is constructing a completely new value.
     *
     * e.g. person in `{ person | age = person.age + 1 }`
     */
    val baseRecordIdentifier: ElmRecordBaseIdentifier?
        get() = findChildByClass(ElmRecordBaseIdentifier::class.java)

    /**
     * The fields to set in the new record.
     */
    val fieldList: List<ElmField>
        get() = PsiTreeUtil.getChildrenOfTypeAsList(this, ElmField::class.java)
}
