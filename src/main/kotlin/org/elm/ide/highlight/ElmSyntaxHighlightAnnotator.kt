package org.elm.ide.highlight


import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.elm.ide.color.ElmColor
import org.elm.lang.core.psi.elements.*


class ElmSyntaxHighlightAnnotator : Annotator {

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        holder.highlight(element)
    }

    private fun AnnotationHolder.highlight(element: PsiElement) {
        when (element) {
            is ElmValueDeclaration -> valueDeclaration(element)
            is ElmTypeAnnotation -> typeAnnotation(element)
            is ElmUpperCaseQID -> upperCaseQID(element)
            is ElmField -> field(element.lowerCaseIdentifier)
            is ElmFieldType -> field(element.lowerCaseIdentifier)
            is ElmFieldAccessExpr -> field(element.lowerCaseIdentifier)
            is ElmFieldAccessorFunctionExpr -> fieldAccessorFunction(element)
            is ElmUnionVariant -> unionVariant(element.upperCaseIdentifier)
            is ElmTypeVariable -> typeExpr(element)
            is ElmLowerTypeName -> typeExpr(element)
        }
    }

    private fun AnnotationHolder.typeExpr(element: PsiElement) {
        applyColor(element, ElmColor.TYPE_EXPR)
    }

    private fun AnnotationHolder.unionVariant(element: PsiElement) {
        applyColor(element, ElmColor.UNION_VARIANT)
    }

    private fun AnnotationHolder.upperCaseQID(element: ElmUpperCaseQID) {
        val isTypeExpr = PsiTreeUtil.getParentOfType(
            element,
            ElmTypeExpression::class.java,
            ElmUnionVariant::class.java
        )
        if (isTypeExpr != null) {
            typeExpr(element)
            return
        }

        val isModuleName = PsiTreeUtil.getParentOfType(
            element,
            ElmImportClause::class.java,
            ElmModuleDeclaration::class.java
        ) != null
        if (!isModuleName) {
            applyColor(element, ElmColor.UNION_VARIANT)
        }
    }

    private fun AnnotationHolder.valueDeclaration(declaration: ElmValueDeclaration) {
        declaration.declaredNames(includeParameters = false).forEach {
            applyColor(it.nameIdentifier, ElmColor.DEFINITION_NAME)
        }
    }

    private fun AnnotationHolder.typeAnnotation(typeAnnotation: ElmTypeAnnotation) {
        applyColor(typeAnnotation.lowerCaseIdentifier, ElmColor.DEFINITION_NAME)
    }

    private fun AnnotationHolder.field(element: PsiElement?) {
        if (element == null) return
        applyColor(element, ElmColor.RECORD_FIELD)
    }

    private fun AnnotationHolder.fieldAccessorFunction(element: ElmFieldAccessorFunctionExpr) {
        applyColor(element, ElmColor.RECORD_FIELD_ACCESSOR)
    }

    private fun AnnotationHolder.applyColor(element: PsiElement, color: ElmColor) {
        this.newSilentAnnotation(HighlightSeverity.INFORMATION).textAttributes(color.textAttributesKey).create()
    }
}
