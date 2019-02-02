package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import org.elm.lang.core.psi.ElmFunctionParamTag
import org.elm.lang.core.psi.ElmPatternChildTag
import org.elm.lang.core.psi.ElmPsiElementImpl
import org.elm.lang.core.psi.ElmUnionPatternChildTag
import org.elm.lang.core.psi.ElmValueAssigneeTag


class ElmPattern(node: ASTNode) : ElmPsiElementImpl(node), ElmFunctionParamTag, ElmPatternChildTag, ElmUnionPatternChildTag, ElmValueAssigneeTag {

    /**
     * The actual type of this pattern.
     *
     * If this patten is wrapped in parenthesis, the child will be another [ElmPattern]
     */
    val child: ElmPatternChildTag
        get() = findNotNullChildByClass(ElmPatternChildTag::class.java)

    val patternAs: ElmPatternAs?
        get() = findChildByClass(ElmPatternAs::class.java)
}
