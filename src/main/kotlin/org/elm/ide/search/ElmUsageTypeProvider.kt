package org.elm.ide.search

import com.intellij.psi.PsiElement
import com.intellij.usages.impl.rules.UsageType
import com.intellij.usages.impl.rules.UsageTypeProvider
import org.elm.lang.core.psi.elements.*

class ElmUsageTypeProvider : UsageTypeProvider {
    // Instantiate each UsageType only once, so that the equality check in UsageTypeGroup.equals() works correctly
    private val FUNCTION_DECLARATION = UsageType { "Function Declaration" }
    private val IMPORT = UsageType { "Import" }
    private val MODULE_DECLARATION = UsageType { "Module Declaration" }
    private val OPERATOR = UsageType { "Operator" }
    private val OTHER = UsageType { "Other" }
    private val PORT_ANNOTATION = UsageType { "Port Annotation" }
    private val TYPE_ALIAS_DECLARATION = UsageType { "Type Alias Declaration"}
    private val TYPE_ANNOTATION = UsageType { "Type Annotation" }
    private val TYPE_DECLARATION = UsageType { "Type Declaration"}
    private val TYPE_EXPRESSION = UsageType { "Type Expression" }
    private val TYPE_VARIABLE = UsageType { "Type Variable" }
    private val UNION_PATTERN = UsageType { "Union Pattern" }
    private val UNION_VARIANT_REFERENCE = UsageType { "Union Variant Reference" }
    private val UNKNOWN = UsageType { "Unknown" }
    private val VALUE_DECLARATION = UsageType { "Value Declaration" }

    override fun getUsageType(element: PsiElement): UsageType? {
        // This lists all PsiElements although many cannot can be a result of a
        // usage search (e.g. a ElmCaseOfExpr). They are categorized as OTHER.
        return when (element) {
            is ElmAnonymousFunctionExpr -> OTHER
            is ElmAnythingPattern -> OTHER
            is ElmAsClause -> OTHER
            is ElmBinOpExpr -> OTHER
            is ElmCaseOfBranch -> OTHER
            is ElmCaseOfExpr -> OTHER
            is ElmCharConstantExpr -> OTHER
            is ElmConsPattern -> OTHER
            is ElmExposedOperator -> OTHER
            is ElmExposedType -> IMPORT
            is ElmExposedValue -> IMPORT
            is ElmExposingList -> OTHER
            is ElmField -> UsageType.WRITE
            is ElmFieldAccessExpr -> UsageType.READ
            is ElmFieldAccessorFunctionExpr -> UsageType.READ
            is ElmFieldType -> UsageType.CLASS_FIELD_DECLARATION
            is ElmFunctionCallExpr -> UsageType.READ
            is ElmFunctionDeclarationLeft -> FUNCTION_DECLARATION
            is ElmGlslCodeExpr -> OTHER
            is ElmIfElseExpr -> OTHER
            is ElmImportClause -> IMPORT
            is ElmInfixDeclaration -> OTHER
            is ElmInfixFuncRef -> FUNCTION_DECLARATION
            is ElmLetInExpr -> OTHER
            is ElmListExpr -> OTHER
            is ElmListPattern -> OTHER
            is ElmLowerPattern -> OTHER
            is ElmLowerTypeName -> TYPE_VARIABLE
            is ElmModuleDeclaration -> MODULE_DECLARATION
            is ElmNegateExpr -> OTHER
            is ElmNullaryConstructorArgumentPattern -> UNION_PATTERN
            is ElmNumberConstantExpr -> OTHER
            is ElmOperator -> OPERATOR
            is ElmOperatorAsFunctionExpr -> OPERATOR
            is ElmParenthesizedExpr -> OTHER
            is ElmPattern -> OTHER
            is ElmPortAnnotation -> PORT_ANNOTATION
            is ElmRecordBaseIdentifier -> UsageType.READ
            is ElmRecordExpr -> OTHER
            is ElmRecordPattern -> OTHER
            is ElmRecordType -> TYPE_ANNOTATION
            is ElmStringConstantExpr -> OTHER
            is ElmTupleExpr -> OTHER
            is ElmTuplePattern -> OTHER
            is ElmTupleType -> OTHER
            is ElmTypeAliasDeclaration -> TYPE_ALIAS_DECLARATION
            is ElmTypeAnnotation -> TYPE_ANNOTATION
            is ElmTypeDeclaration -> TYPE_DECLARATION
            is ElmTypeExpression -> TYPE_EXPRESSION
            is ElmTypeRef -> TYPE_EXPRESSION
            is ElmTypeVariable -> TYPE_VARIABLE
            is ElmUnionPattern -> UNION_PATTERN
            is ElmUnionVariant -> UNION_VARIANT_REFERENCE
            is ElmUnitExpr -> OTHER
            is ElmUpperCaseQID -> UsageType.READ
            is ElmValueDeclaration -> VALUE_DECLARATION
            is ElmValueExpr -> UsageType.READ
            is ElmValueQID -> UsageType.READ
            is Pipeline -> OTHER
            else -> UNKNOWN
        }
    }
}
