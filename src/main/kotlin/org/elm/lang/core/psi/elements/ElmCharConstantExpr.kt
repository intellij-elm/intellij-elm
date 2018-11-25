package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import org.elm.lang.core.psi.ElmConstantTag
import org.elm.lang.core.psi.ElmPsiElementImpl


/** A literal char. e.g. `'x'` or `'\u{0042}'` */
class ElmCharConstantExpr(node: ASTNode) : ElmPsiElementImpl(node), ElmConstantTag
