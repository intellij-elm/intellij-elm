package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import org.elm.lang.core.psi.ElmOperandTag
import org.elm.lang.core.psi.ElmPsiElementImpl


class ElmGlslCodeExpr(node: ASTNode) : ElmPsiElementImpl(node), ElmOperandTag
