package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import org.elm.lang.core.psi.*


class ElmUnit(node: ASTNode) : ElmPsiElementImpl(node), ElmOperandTag, ElmFunctionParamTag,
        ElmPatternChildTag, ElmUnionPatternChildTag
