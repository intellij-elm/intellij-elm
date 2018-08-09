package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import org.elm.lang.core.psi.ElmPsiElementImpl
import org.elm.lang.core.psi.tags.ElmOperand


class ElmGlslCode(node: ASTNode) : ElmPsiElementImpl(node), ElmOperand
