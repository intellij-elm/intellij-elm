package org.elm.lang.core.parser.manual

import com.intellij.lang.ASTNode
import org.elm.lang.core.psi.ElmTypes
import org.elm.lang.core.psi.elements.*

object ElmManualPsiElementFactory {

    fun createElement(node: ASTNode) =
            when (node.elementType) {
                ElmTypes.UPPER_CASE_PATH -> ElmUpperCasePath(node)
                ElmTypes.LOWER_CASE_PATH -> ElmLowerCasePath(node)
                ElmTypes.MIXED_CASE_PATH -> ElmMixedCasePath(node)
                ElmTypes.FIELD_ACCESS -> ElmFieldAccess(node)
                ElmTypes.EFFECT -> ElmEffect(node)
                else ->                     null
            }
}
