package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import org.elm.lang.core.psi.ElmPsiElementImpl
import org.elm.lang.core.psi.ElmConsPatternChildTag
import org.elm.lang.core.psi.ElmFunctionParamTag
import org.elm.lang.core.psi.ElmOperandTag
import org.elm.lang.core.psi.ElmPatternChildTag


class ElmUnit(node: ASTNode) : ElmPsiElementImpl(node), ElmConsPatternChildTag, ElmOperandTag, ElmFunctionParamTag, ElmPatternChildTag
