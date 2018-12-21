package org.elm.ide.highlight

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil


import org.elm.ide.color.ElmColor
import org.elm.lang.core.psi.elements.*


class ElmSyntaxHighlightAnnotator : Annotator {

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        when (element) {
            is ElmValueDeclaration -> highlightValueDeclaration(holder, element)
            is ElmTypeAnnotation -> highlightTypeAnnotation(holder, element)
            is ElmUpperCaseQID -> highlightUpperCaseQID(holder, element)
        }
    }

    private fun highlightUpperCaseQID(holder: AnnotationHolder, element: ElmUpperCaseQID) {
        // we only want to highlight upper-case QIDs when they are union or record constructors
        val isModuleName = PsiTreeUtil.getParentOfType(element,
                ElmTypeAnnotation::class.java,
                ElmModuleDeclaration::class.java,
                ElmImportClause::class.java) != null
        if (!isModuleName)
            highlightElement(holder, element, ElmColor.TYPE)
    }

    private fun highlightValueDeclaration(holder: AnnotationHolder, declaration: ElmValueDeclaration) {
        // first try to get the name of a value/function
        // TODO [kl] we will need to do something smarter here if we want to highlight
        // destructuring pattern value declarations (e.g. within a `let/in` expression)
        // TODO [kl] cleanup the PSI so that this is cleaner (or use ElmNamedElement)
        val nameElement = declaration.functionDeclarationLeft?.lowerCaseIdentifier

        nameElement?.let {
            highlightElement(holder, it, ElmColor.DEFINITION_NAME)
        }
    }

    private fun highlightTypeAnnotation(holder: AnnotationHolder, typeAnnotation: ElmTypeAnnotation) {
        typeAnnotation.lowerCaseIdentifier?.let {
            highlightElement(holder, it, ElmColor.TYPE_ANNOTATION_NAME)
        }

        val def = typeAnnotation.typeExpression
        listOf(ElmUpperCaseQID::class.java, ElmTypeVariableRef::class.java)
                .flatMap { PsiTreeUtil.findChildrenOfType(def, it) }
                .forEach {
                    highlightElement(holder, it, ElmColor.TYPE_ANNOTATION_SIGNATURE_TYPES)
                }
    }

    private fun highlightElement(holder: AnnotationHolder, element: PsiElement, color: ElmColor) {
        holder.createInfoAnnotation(element, null).textAttributes = color.textAttributesKey
    }
}
