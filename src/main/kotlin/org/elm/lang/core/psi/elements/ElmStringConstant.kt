package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import org.elm.lang.core.psi.*


/** A literal string. e.g. `""` or `"""a"b"""` */
class ElmStringConstant(node: ASTNode) : ElmPsiElementImpl(node), ElmOperandTag, ElmConsPatternChildTag,
        ElmFunctionParamTag, ElmPatternChildTag, ElmConstantTag
