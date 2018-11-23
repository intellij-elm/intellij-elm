package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import org.elm.lang.core.psi.ElmFunctionCallTargetTag
import org.elm.lang.core.psi.ElmOperandTag
import org.elm.lang.core.psi.ElmPsiElementImpl

// TODO [drop 0.18] remove this class
class ElmTupleConstructorExpr(node: ASTNode) : ElmPsiElementImpl(node), ElmOperandTag, ElmFunctionCallTargetTag
