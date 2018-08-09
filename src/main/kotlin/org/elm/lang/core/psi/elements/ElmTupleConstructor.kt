package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import org.elm.lang.core.psi.ElmPsiElementImpl
import org.elm.lang.core.psi.tags.ElmOperand

// TODO [drop 0.18] remove this class
class ElmTupleConstructor(node: ASTNode) : ElmPsiElementImpl(node), ElmOperand
