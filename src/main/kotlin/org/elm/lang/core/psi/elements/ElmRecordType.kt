package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.elm.lang.core.psi.ElmPsiElementImpl
import org.elm.lang.core.psi.ElmTypes.LOWER_CASE_IDENTIFIER
import org.elm.lang.core.psi.tags.ElmParametricTypeRefParameterTag
import org.elm.lang.core.psi.tags.ElmUnionMemberParameterTag


/**
 * A record type definition
 *
 * e.g. { name : String, age : Int }
 */
class ElmRecordType(node: ASTNode) : ElmPsiElementImpl(node), ElmUnionMemberParameterTag, ElmParametricTypeRefParameterTag {

    /**
     * The type variable representing a generic record which this
     * definition extends.
     *
     * e.g. entity in `{ entity | vx : Float, vy: Float }`
     */
    val baseTypeIdentifier: PsiElement?
        get() = findChildByType(LOWER_CASE_IDENTIFIER)

    /**
     * The definition of the fields which comprise the record proper.
     */
    val fieldTypeList: List<ElmFieldType>
        get() = PsiTreeUtil.getChildrenOfTypeAsList(this, ElmFieldType::class.java)

}
