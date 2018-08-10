package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import org.elm.lang.core.psi.ElmPsiElementImpl
import org.elm.lang.core.psi.tags.ElmOperandTag
import org.elm.lang.core.psi.tags.ElmConsPatternChildTag


class ElmUnit(node: ASTNode) : ElmPsiElementImpl(node), ElmConsPatternChildTag, ElmOperandTag
