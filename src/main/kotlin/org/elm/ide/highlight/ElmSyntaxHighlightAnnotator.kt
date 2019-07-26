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
            is ElmField -> highlightField(holder, element.lowerCaseIdentifier)
            is ElmFieldType -> highlightField(holder, element.lowerCaseIdentifier)
            is ElmFieldAccessExpr -> highlightField(holder, element.lowerCaseIdentifier)
            is ElmFieldAccessorFunctionExpr -> highlightFieldAccessorFunction(holder, element)
            is ElmUnionVariant -> highlightTypeConstructor(holder, element.upperCaseIdentifier)
            is ElmTypeVariable -> highlightTypeExpr(holder, element)
            is ElmLowerTypeName -> highlightTypeExpr(holder, element)
        }
    }

    private fun highlightTypeExpr(holder: AnnotationHolder, element: PsiElement) {
        highlightElement(holder, element, ElmColor.TYPE_EXPR)
    }

    private fun highlightTypeConstructor(holder: AnnotationHolder, element: PsiElement) {
        highlightElement(holder, element, ElmColor.UNION_VARIANT)
    }

    private fun highlightUpperCaseQID(holder: AnnotationHolder, element: ElmUpperCaseQID) {
        val isTypeExpr = PsiTreeUtil.getParentOfType(element,
                ElmTypeExpression::class.java,
                ElmUnionVariant::class.java)
        if (isTypeExpr != null) {
            highlightTypeExpr(holder, element)
            return
        }

        val isModuleName = PsiTreeUtil.getParentOfType(element,
                ElmImportClause::class.java,
                ElmModuleDeclaration::class.java) != null
        if (!isModuleName) {
            highlightElement(holder, element, ElmColor.UNION_VARIANT)
        }
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
    }

    private fun highlightField(holder: AnnotationHolder, element: PsiElement?) {
        if (element == null) return
        highlightElement(holder, element, ElmColor.RECORD_FIELD)
    }

    private fun highlightFieldAccessorFunction(holder: AnnotationHolder, element: ElmFieldAccessorFunctionExpr) {
        highlightElement(holder, element, ElmColor.RECORD_FIELD_ACCESSOR)
    }

    private fun highlightElement(holder: AnnotationHolder, element: PsiElement, color: ElmColor) {
        val msg = "Highlighting %-60s \"%-20s\" with %s".format(element, element.text, color)
        println(msg)
        holder.createInfoAnnotation(element, null).textAttributes = color.textAttributesKey
    }
}
