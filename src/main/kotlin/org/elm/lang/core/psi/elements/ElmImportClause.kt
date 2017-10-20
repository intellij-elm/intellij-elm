
package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElementVisitor
import org.elm.lang.core.psi.ElmPsiElement
import org.elm.lang.core.psi.ElmVisitor


/**
 * e.g. 'import Data.User exposing (User, name, age)'
 */
class ElmImportClause(node: ASTNode) : ElmPsiElement(node) {

    fun accept(visitor: ElmVisitor) {
        visitor.visitImportClause(this)
    }

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is ElmVisitor)
            accept(visitor)
        else
            super.accept(visitor)
    }

    val modulePath: ElmUpperCasePath
        get() = findNotNullChildByClass(ElmUpperCasePath::class.java)

    val asClause: ElmAsClause?
        get() = findChildByClass(ElmAsClause::class.java)

    val exposingClause: ElmExposingClause?
        get() = findChildByClass(ElmExposingClause::class.java)

}
