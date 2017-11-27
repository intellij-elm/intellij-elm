package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElementVisitor
import org.elm.lang.core.psi.ElmPsiElementImpl

class ElmFieldAccessorFunction(node: ASTNode) : ElmPsiElementImpl(node)