package org.elm.ide.highlight


import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.elm.ide.color.ElmColor
import org.elm.lang.core.psi.ancestors
import org.elm.lang.core.psi.elements.*
import org.elm.lang.core.psi.isTopLevel


class ElmSyntaxHighlightAnnotator : Annotator {

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        holder.highlight(element)
    }

    private fun AnnotationHolder.highlight(element: PsiElement) {
        when (element) {
            is ElmTypeDeclaration -> typeDecl(element)
            is ElmTypeAliasDeclaration -> typeAliasDecl(element)
            is ElmImportClause -> importClause(element)
            is ElmPortAnnotation -> portAnnotation(element)
            is ElmRecordBaseIdentifier -> recordUpdate(element)
            is ElmLowerPattern -> functionArgument(element)
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
            is ElmValueExpr -> valueExpr(element)
        }
    }

    private fun AnnotationHolder.typeDecl(element: ElmTypeDeclaration) {
        applyColor(element.nameIdentifier, ElmColor.TYPE_DECLARATION)
    }

    private fun AnnotationHolder.typeAliasDecl(element: ElmTypeAliasDeclaration) {
        applyColor(element.nameIdentifier, ElmColor.TYPE_DECLARATION)
    }

    private fun AnnotationHolder.importClause(element: ElmImportClause) {
        applyColor(element.moduleQID, ElmColor.EXTERNAL_MODULE)
        element.exposingList?.exposedTypeList?.forEach { applyColor(it, ElmColor.TYPE_EXPR) }
        element.exposingList?.exposedValueList?.forEach { applyColor(it, ElmColor.EXTERNAL_FUNCTION_CALL) }
    }

    private fun AnnotationHolder.portAnnotation(element: ElmPortAnnotation) {
        applyColor(element.lowerCaseIdentifier, ElmColor.PORT)
    }

    private fun AnnotationHolder.recordUpdate(element: ElmRecordBaseIdentifier) {
        annotateArgument(element)
    }

    private fun AnnotationHolder.annotateArgument(element: PsiElement): Boolean {
        val parentFunction = element.ancestors
            .filterIsInstance<ElmValueDeclaration>()
            .firstOrNull {
                it.functionDeclarationLeft?.namedParameters?.any { p -> p.nameIdentifier.text == element.text } ?: false
            }
        if (parentFunction != null) {
            if (parentFunction.isTopLevel) {
                applyColor(element, ElmColor.FUNCTION_ARGUMENT)
            } else {
                applyColor(element, ElmColor.LOCAL_FUNCTION_ARGUMENT)
            }
            return true
        }

        val isInlineFunctionArgument = element.ancestors
            .filterIsInstance<ElmAnonymousFunctionExpr>()
            .firstOrNull {
                it.namedParameters.any { p -> p.nameIdentifier.text == element.text }
            } != null
        if (isInlineFunctionArgument) {
            applyColor(element, ElmColor.INLINE_FUNCTION_ARGUMENT)
            return true
        }

        val isUnionPattern =
            element.ancestors.any { it is ElmCaseOfBranch && it.destructuredNames.any { name -> name.nameIdentifier.text == element.text } }
        if (isUnionPattern) {
            applyColor(element, ElmColor.PATTERN_ARGUMENT)
            return true
        }

        return false
    }

    private fun AnnotationHolder.valueExpr(element: ElmValueExpr) {
        if (element.flavor == Flavor.BareConstructor || element.flavor == Flavor.QualifiedConstructor) {
            return
        }

        if (annotateArgument(element)) {
            return
        }

        val isLocalFunction = element.ancestors
            .filterIsInstance<ElmLetInExpr>()
            .any {
                it.valueDeclarationList.any { decl -> decl.functionDeclarationLeft?.lowerCaseIdentifier?.text == element.text }
            }
        if (isLocalFunction) {
            applyColor(element, ElmColor.LOCAL_FUNCTION)
            return
        }

        val functionDeclaration =
            element.elmFile.children.any { it is ElmValueDeclaration && it.functionDeclarationLeft?.name == element.text }
        if (functionDeclaration) {
            applyColor(element, ElmColor.DEFINITION_NAME)
            return
        }

        val isPortAnnotation =
            element.elmFile.children.any { it is ElmPortAnnotation && it.lowerCaseIdentifier.text == element.text }
        if (isPortAnnotation) {
            applyColor(element, ElmColor.PORT)
            return
        }

        val qid = element.valueQID
        if (qid != null && qid.isQualified == true) {
            qid.qualifiers.forEach {
                applyColor(it, ElmColor.EXTERNAL_MODULE)
            }
            applyColor(qid.lowerCaseIdentifier, ElmColor.EXTERNAL_FUNCTION_CALL)
            return
        }

        applyColor(element, ElmColor.EXTERNAL_FUNCTION_CALL)
    }

    private fun AnnotationHolder.functionArgument(element: ElmLowerPattern) {
        val parentFunction = PsiTreeUtil.getParentOfType(
            element,
            ElmFunctionDeclarationLeft::class.java
        );
        if (parentFunction != null) {
            if (parentFunction.isTopLevel) {
                applyColor(element, ElmColor.FUNCTION_ARGUMENT)
            } else {
                applyColor(element, ElmColor.LOCAL_FUNCTION_ARGUMENT)
            }
            return
        }

        val anonymousFunction = PsiTreeUtil.getParentOfType(
            element,
            ElmAnonymousFunctionExpr::class.java
        );
        if (anonymousFunction != null) {
            applyColor(element, ElmColor.INLINE_FUNCTION_ARGUMENT)
            return
        }

        val unionPattern = PsiTreeUtil.getParentOfType(
            element,
            ElmUnionPattern::class.java
        );
        if (unionPattern != null) {
            applyColor(element, ElmColor.PATTERN_ARGUMENT)
            return
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
            if (it is ElmFunctionDeclarationLeft) {
                if (it.ancestors.filterIsInstance<ElmLetInExpr>().any()) {
                    applyColor(it.nameIdentifier, ElmColor.LOCAL_FUNCTION)
                } else {
                    applyColor(it.nameIdentifier, ElmColor.DEFINITION_NAME)
                }
            }
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
        newSilentAnnotation(HighlightSeverity.INFORMATION)
            .textAttributes(color.textAttributesKey)
            .range(element.textRange)
            .create()
    }
}
