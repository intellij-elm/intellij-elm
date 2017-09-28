package org.elm.lang.core.psi.ext

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.PsiNamedElement
import org.elm.lang.core.psi.ElmPsiElement
import org.elm.lang.core.psi.ElmPsiFactory
import org.elm.lang.core.psi.ElmTypes.LOWER_CASE_IDENTIFIER
import org.elm.lang.core.psi.ElmTypes.UPPER_CASE_IDENTIFIER
import org.elm.lang.core.psi.interfaces.ElmLowerCaseId
import org.elm.lang.core.psi.interfaces.ElmUpperCaseId


interface ElmNamedElement : PsiNamedElement


interface ElmNameIdentifierOwner : ElmNamedElement, PsiNameIdentifierOwner


open class ElmNamedElementImpl(node: ASTNode) : ElmPsiElement(node), ElmNameIdentifierOwner {

    // TODO revisit whether this is a good idea to hide the lower-/upper-case id distinction
    override fun getNameIdentifier(): PsiElement? =
            findChildByType(UPPER_CASE_IDENTIFIER)
                ?: findChildByType(LOWER_CASE_IDENTIFIER)

    override fun getName() =
            nameIdentifier?.text

    override fun setName(name: String): PsiElement {
        val newIdentifier = when (nameIdentifier) {
            is ElmUpperCaseId -> ElmPsiFactory(project).createUpperCaseIdentifier(name)
            is ElmLowerCaseId -> ElmPsiFactory(project).createLowerCaseIdentifier(name)
            else -> error("unexpected name identifier type")
        }
        nameIdentifier?.replace(newIdentifier)
        return this
    }
}