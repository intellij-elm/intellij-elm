package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import org.elm.lang.core.psi.ElmFunctionCallTarget
import org.elm.lang.core.psi.ElmOperandTag
import org.elm.lang.core.psi.ElmPsiElementImpl

// TODO [drop 0.18] remove this class
class ElmTupleConstructor(node: ASTNode) : ElmPsiElementImpl(node), ElmOperandTag, ElmFunctionCallTarget
