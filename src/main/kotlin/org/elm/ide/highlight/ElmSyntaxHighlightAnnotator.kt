package org.elm.ide.highlight


import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.elm.ide.color.ElmColor
import org.elm.lang.core.psi.elements.*


class ElmSyntaxHighlightAnnotator : Annotator {

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        val h = Highlighter(holder)
        when (element) {
            is ElmValueDeclaration -> h.valueDeclaration(element)
            is ElmTypeAnnotation -> h.typeAnnotation(element)
            is ElmUpperCaseQID -> h.upperCaseQID(element)
            is ElmField -> h.field(element.lowerCaseIdentifier)
            is ElmFieldType -> h.field(element.lowerCaseIdentifier)
            is ElmFieldAccessExpr -> h.field(element.lowerCaseIdentifier)
            is ElmFieldAccessorFunctionExpr -> h.fieldAccessorFunction(element)
            is ElmUnionVariant -> h.unionVariant(element.upperCaseIdentifier)
            is ElmTypeVariable -> h.typeExpr(element)
            is ElmLowerTypeName -> h.typeExpr(element)
        }
    }
}


@Suppress("EXPERIMENTAL_FEATURE_WARNING")
inline class Highlighter(private val holder: AnnotationHolder) {

    fun typeExpr(element: PsiElement) {
        highlight(element, ElmColor.TYPE_EXPR)
    }

    fun unionVariant(element: PsiElement) {
        highlight(element, ElmColor.UNION_VARIANT)
    }

    fun upperCaseQID(element: ElmUpperCaseQID) {
        val isTypeExpr = PsiTreeUtil.getParentOfType(element,
                ElmTypeExpression::class.java,
                ElmUnionVariant::class.java)
        if (isTypeExpr != null) {
            typeExpr(element)
            return
        }

        val isModuleName = PsiTreeUtil.getParentOfType(element,
                ElmImportClause::class.java,
                ElmModuleDeclaration::class.java) != null
        if (!isModuleName) {
            highlight(element, ElmColor.UNION_VARIANT)
        }
    }

    fun valueDeclaration(declaration: ElmValueDeclaration) {
        declaration.declaredNames().forEach {
            highlight(it, ElmColor.DEFINITION_NAME)
        }
    }

    fun typeAnnotation(typeAnnotation: ElmTypeAnnotation) {
        typeAnnotation.lowerCaseIdentifier?.let {
            highlight(it, ElmColor.DEFINITION_NAME)
        }
    }

    fun field(element: PsiElement?) {
        if (element == null) return
        highlight(element, ElmColor.RECORD_FIELD)
    }

    fun fieldAccessorFunction(element: ElmFieldAccessorFunctionExpr) {
        highlight(element, ElmColor.RECORD_FIELD_ACCESSOR)
    }

    private fun highlight(element: PsiElement, color: ElmColor) {
        holder.createInfoAnnotation(element, null).textAttributes = color.textAttributesKey
    }
}
