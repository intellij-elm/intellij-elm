package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import org.elm.lang.core.psi.ElmConstantTag
import org.elm.lang.core.psi.ElmPsiElementImpl


/** A literal string. e.g. `""` or `"""a"b"""` */
class ElmStringConstant(node: ASTNode) : ElmPsiElementImpl(node), ElmConstantTag
