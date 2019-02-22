package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.util.parentOfType
import org.elm.lang.core.psi.*
import org.elm.lang.core.resolve.ElmReferenceElement
import org.elm.lang.core.resolve.reference.ElmReferenceCached

/**
 * Holds a lower-case identifier within a type reference which
 * gives the type variable in a parametric type.
 *
 * e.g. the 'a' in `map : (a -> b) -> List a -> List b`
 * e.g. the last `a` in `type Foo a = Bar a`
 */
class ElmTypeVariableRef(
        node: ASTNode
) : ElmPsiElementImpl(node),
        ElmReferenceElement,
        ElmUnionVariantParameterTag,
        ElmTypeRefArgumentTag,
        ElmTypeExpressionSegmentTag {

    val identifier: PsiElement
        get() = findNotNullChildByType(ElmTypes.LOWER_CASE_IDENTIFIER)

    override val referenceNameElement: PsiElement
        get() = identifier

    override val referenceName: String
        get() = referenceNameElement.text

    override fun getReference() =
            object : ElmReferenceCached<ElmTypeVariableRef>(this) {

                override fun isSoft(): Boolean {
                    val unionTypeDecl = parentOfType<ElmTypeDeclaration>()
                    val typeAliasDecl = parentOfType<ElmTypeAliasDeclaration>()
                    // The reference is considered soft if we are not in a type declaration
                    // context. In such cases, the type variable is completely free: it does
                    // not refer to anything else.
                    return unionTypeDecl == null && typeAliasDecl == null
                }

                override fun resolveInner(): ElmNamedElement? =
                        getVariants().find { it.name == referenceName }

                override fun getVariants(): Array<ElmNamedElement> {
                    if (isSoft) return emptyArray()

                    val typeVars = element.parentOfType<ElmTypeDeclaration>()?.lowerTypeNameList
                            ?: element.parentOfType<ElmTypeAliasDeclaration>()?.lowerTypeNameList
                            ?: return emptyArray()

                    return typeVars.toTypedArray()
                }
            }
}
