package org.elm.lang.core.parser.manual

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import org.elm.lang.core.psi.impl.*
import org.elm.lang.core.psi.ElmTypes

object ElmManualPsiElementFactory {

    fun createElement(node: ASTNode?) =
            when (node?.elementType) {
                ElmTypes.CASE_OF ->         ElmCaseOfImpl(node)
                ElmTypes.LET_IN ->          ElmLetInImpl(node)
                ElmTypes.UPPER_CASE_PATH -> ElmUpperCasePathImpl(node)
                ElmTypes.LOWER_CASE_PATH -> ElmLowerCasePathImpl(node)
                ElmTypes.MIXED_CASE_PATH -> ElmMixedCasePathImpl(node)
                ElmTypes.FIELD_ACCESS ->    ElmFieldAccessImpl(node)
                ElmTypes.EFFECT ->          ElmEffectImpl(node)
                else ->                     null
            }
}
