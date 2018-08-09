package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import org.elm.lang.core.psi.ElmPsiElementImpl
import org.elm.lang.core.psi.tags.ElmOperand
import org.elm.lang.core.psi.tags.ElmPatternChild


class ElmUnit(node: ASTNode) : ElmPsiElementImpl(node), ElmPatternChild, ElmOperand
