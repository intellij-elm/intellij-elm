package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.util.PsiTreeUtil
import org.elm.lang.core.psi.ElmPsiElementImpl
import org.elm.lang.core.psi.ElmParametricTypeRefParameterTag
import org.elm.lang.core.psi.ElmTypeRefSegmentTag
import org.elm.lang.core.psi.ElmUnionMemberParameterTag


/**
 * A record type definition
 *
 * e.g. { name : String, age : Int }
 */
class ElmRecordType(node: ASTNode) : ElmPsiElementImpl(node), ElmUnionMemberParameterTag, ElmParametricTypeRefParameterTag, ElmTypeRefSegmentTag {

    /**
     * The type variable representing a generic record which this
     * definition extends.
     *
     * e.g. entity in `{ entity | vx : Float, vy: Float }`
     */
    val baseTypeIdentifier: ElmRecordBaseIdentifier?
        get() = findChildByClass(ElmRecordBaseIdentifier::class.java)

    /**
     * The definition of the fields which comprise the record proper.
     */
    val fieldTypeList: List<ElmFieldType>
        get() = PsiTreeUtil.getChildrenOfTypeAsList(this, ElmFieldType::class.java)

}
