package org.elm.lang.core.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.*
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubElement
import org.elm.ide.presentation.getPresentation
import org.elm.lang.core.psi.ElmTypes.*
import org.elm.lang.core.psi.IdentifierCase.LOWER
import org.elm.lang.core.psi.IdentifierCase.OPERATOR
import org.elm.lang.core.psi.IdentifierCase.UPPER
import org.elm.lang.core.stubs.ElmNamedStub


interface ElmNamedElement : ElmPsiElement, PsiNamedElement, NavigatablePsiElement


interface ElmNameIdentifierOwner : ElmNamedElement, PsiNameIdentifierOwner {
    override fun getNameIdentifier(): PsiElement
    override fun getName(): String
}

/** An element that can have an attached documentation comment */
interface ElmDocTarget: ElmPsiElement {
    /** The doc comment for this element, or `null` if there isn't one. */
    val docComment: PsiComment?
        get() = (prevSiblings.withoutWs.firstOrNull() as? PsiComment)?.takeIf { it.elementType == DOC_COMMENT }
}


open class ElmNamedElementImpl(node: ASTNode, val case: IdentifierCase) : ElmPsiElementImpl(node), ElmNameIdentifierOwner {

    override fun getNameIdentifier(): PsiElement =
            when (case) {
                UPPER -> findNotNullChildByType(UPPER_CASE_IDENTIFIER)
                LOWER -> findNotNullChildByType(LOWER_CASE_IDENTIFIER)
                OPERATOR -> findNotNullChildByType(OPERATOR_IDENTIFIER)
            }

    override fun getName(): String =
            nameIdentifier.text


    override fun setName(name: String): PsiElement {
        val newIdentifier = when (nameIdentifier.elementType) {
            UPPER_CASE_IDENTIFIER -> ElmPsiFactory(project).createUpperCaseIdentifier(name)
            LOWER_CASE_IDENTIFIER -> ElmPsiFactory(project).createLowerCaseIdentifier(name)
            OPERATOR_IDENTIFIER -> ElmPsiFactory(project).createOperatorIdentifier(name)
            else -> error("unexpected name identifier type: ${nameIdentifier.elementType}")
        }
        nameIdentifier.replace(newIdentifier)
        return this
    }

    override fun getTextOffset(): Int =
            nameIdentifier.textOffset

    override fun getPresentation() =
            getPresentation(this)
}


open class ElmStubbedNamedElementImpl<StubT> : ElmStubbedElement<StubT>, ElmNameIdentifierOwner
        where StubT : ElmNamedStub, StubT : StubElement<*> {

    val case: IdentifierCase

    constructor(node: ASTNode, case: IdentifierCase) : super(node) {
        this.case = case
    }

    constructor(stub: StubT, nodeType: IStubElementType<*, *>, case: IdentifierCase) : super(stub, nodeType) {
        this.case = case
    }

    override fun getNameIdentifier(): PsiElement =
            when (case) {
                UPPER -> findNotNullChildByType(UPPER_CASE_IDENTIFIER)
                LOWER -> findNotNullChildByType(LOWER_CASE_IDENTIFIER)
                OPERATOR -> findNotNullChildByType(OPERATOR_IDENTIFIER)
            }

    override fun getName(): String =
            stub?.name ?: nameIdentifier.text

    override fun setName(name: String): PsiElement {
        val newIdentifier = when (nameIdentifier.elementType) {
            UPPER_CASE_IDENTIFIER -> ElmPsiFactory(project).createUpperCaseIdentifier(name)
            LOWER_CASE_IDENTIFIER -> ElmPsiFactory(project).createLowerCaseIdentifier(name)
            OPERATOR_IDENTIFIER -> ElmPsiFactory(project).createOperatorIdentifier(name)
            else -> error("unexpected name identifier type: ${nameIdentifier.elementType}")
        }
        nameIdentifier.replace(newIdentifier)
        return this
    }

    override fun getTextOffset() =
            nameIdentifier.textOffset

    override fun getPresentation() =
            getPresentation(this)
}


enum class IdentifierCase {
    UPPER,
    LOWER,
    OPERATOR
}
