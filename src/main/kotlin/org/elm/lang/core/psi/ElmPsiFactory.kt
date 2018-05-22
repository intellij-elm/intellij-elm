package org.elm.lang.core.psi

import com.intellij.lang.ASTNode
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.TokenType.WHITE_SPACE
import com.intellij.psi.tree.IElementType
import org.elm.lang.core.ElmFileType
import org.elm.lang.core.psi.ElmTypes.ANONYMOUS_FUNCTION
import org.elm.lang.core.psi.ElmTypes.AS_CLAUSE
import org.elm.lang.core.psi.ElmTypes.CASE_OF
import org.elm.lang.core.psi.ElmTypes.CASE_OF_BRANCH
import org.elm.lang.core.psi.ElmTypes.EXPOSED_OPERATOR
import org.elm.lang.core.psi.ElmTypes.EXPOSED_TYPE
import org.elm.lang.core.psi.ElmTypes.EXPOSED_UNION_CONSTRUCTOR
import org.elm.lang.core.psi.ElmTypes.EXPOSED_UNION_CONSTRUCTORS
import org.elm.lang.core.psi.ElmTypes.EXPOSED_VALUE
import org.elm.lang.core.psi.ElmTypes.EXPOSING_LIST
import org.elm.lang.core.psi.ElmTypes.EXPRESSION
import org.elm.lang.core.psi.ElmTypes.FIELD
import org.elm.lang.core.psi.ElmTypes.FIELD_TYPE
import org.elm.lang.core.psi.ElmTypes.FUNCTION_DECLARATION_LEFT
import org.elm.lang.core.psi.ElmTypes.GLSL_CODE
import org.elm.lang.core.psi.ElmTypes.IF_ELSE
import org.elm.lang.core.psi.ElmTypes.IMPORT_CLAUSE
import org.elm.lang.core.psi.ElmTypes.INFIX_DECLARATION
import org.elm.lang.core.psi.ElmTypes.INNER_TYPE_ANNOTATION
import org.elm.lang.core.psi.ElmTypes.INNER_VALUE_DECLARATION
import org.elm.lang.core.psi.ElmTypes.LET_IN
import org.elm.lang.core.psi.ElmTypes.LIST
import org.elm.lang.core.psi.ElmTypes.LIST_OF_OPERANDS
import org.elm.lang.core.psi.ElmTypes.LOWER_CASE_IDENTIFIER
import org.elm.lang.core.psi.ElmTypes.LOWER_PATTERN
import org.elm.lang.core.psi.ElmTypes.LOWER_TYPE_NAME
import org.elm.lang.core.psi.ElmTypes.MODULE_DECLARATION
import org.elm.lang.core.psi.ElmTypes.NON_EMPTY_TUPLE
import org.elm.lang.core.psi.ElmTypes.OPERATOR
import org.elm.lang.core.psi.ElmTypes.OPERATOR_AS_FUNCTION
import org.elm.lang.core.psi.ElmTypes.OPERATOR_IDENTIFIER
import org.elm.lang.core.psi.ElmTypes.PARAMETRIC_TYPE_REF
import org.elm.lang.core.psi.ElmTypes.PARENTHESED_EXPRESSION
import org.elm.lang.core.psi.ElmTypes.PATTERN
import org.elm.lang.core.psi.ElmTypes.PATTERN_AS
import org.elm.lang.core.psi.ElmTypes.PORT_ANNOTATION
import org.elm.lang.core.psi.ElmTypes.RECORD
import org.elm.lang.core.psi.ElmTypes.RECORD_PATTERN
import org.elm.lang.core.psi.ElmTypes.RECORD_TYPE
import org.elm.lang.core.psi.ElmTypes.TUPLE_PATTERN
import org.elm.lang.core.psi.ElmTypes.TUPLE_TYPE
import org.elm.lang.core.psi.ElmTypes.TYPE_ALIAS_DECLARATION
import org.elm.lang.core.psi.ElmTypes.TYPE_ANNOTATION
import org.elm.lang.core.psi.ElmTypes.TYPE_DECLARATION
import org.elm.lang.core.psi.ElmTypes.TYPE_REF
import org.elm.lang.core.psi.ElmTypes.TYPE_VARIABLE_REF
import org.elm.lang.core.psi.ElmTypes.UNION_MEMBER
import org.elm.lang.core.psi.ElmTypes.UNION_PATTERN
import org.elm.lang.core.psi.ElmTypes.UNIT
import org.elm.lang.core.psi.ElmTypes.UPPER_PATH_TYPE_REF
import org.elm.lang.core.psi.ElmTypes.VALUE_DECLARATION
import org.elm.lang.core.psi.ElmTypes.VALUE_EXPR
import org.elm.lang.core.psi.elements.ElmAnonymousFunction
import org.elm.lang.core.psi.elements.ElmAsClause
import org.elm.lang.core.psi.elements.ElmCaseOf
import org.elm.lang.core.psi.elements.ElmCaseOfBranch
import org.elm.lang.core.psi.elements.ElmExposedOperator
import org.elm.lang.core.psi.elements.ElmExposedType
import org.elm.lang.core.psi.elements.ElmExposedUnionConstructor
import org.elm.lang.core.psi.elements.ElmExposedUnionConstructors
import org.elm.lang.core.psi.elements.ElmExposedValue
import org.elm.lang.core.psi.elements.ElmExposingList
import org.elm.lang.core.psi.elements.ElmExpression
import org.elm.lang.core.psi.elements.ElmField
import org.elm.lang.core.psi.elements.ElmFieldType
import org.elm.lang.core.psi.elements.ElmFunctionDeclarationLeft
import org.elm.lang.core.psi.elements.ElmGlslCode
import org.elm.lang.core.psi.elements.ElmIfElse
import org.elm.lang.core.psi.elements.ElmImportClause
import org.elm.lang.core.psi.elements.ElmInfixDeclaration
import org.elm.lang.core.psi.elements.ElmInnerTypeAnnotation
import org.elm.lang.core.psi.elements.ElmInnerValueDeclaration
import org.elm.lang.core.psi.elements.ElmLetIn
import org.elm.lang.core.psi.elements.ElmList
import org.elm.lang.core.psi.elements.ElmListOfOperands
import org.elm.lang.core.psi.elements.ElmLowerPattern
import org.elm.lang.core.psi.elements.ElmLowerTypeName
import org.elm.lang.core.psi.elements.ElmModuleDeclaration
import org.elm.lang.core.psi.elements.ElmNonEmptyTuple
import org.elm.lang.core.psi.elements.ElmOperator
import org.elm.lang.core.psi.elements.ElmOperatorAsFunction
import org.elm.lang.core.psi.elements.ElmParametricTypeRef
import org.elm.lang.core.psi.elements.ElmParenthesedExpression
import org.elm.lang.core.psi.elements.ElmPattern
import org.elm.lang.core.psi.elements.ElmPatternAs
import org.elm.lang.core.psi.elements.ElmPortAnnotation
import org.elm.lang.core.psi.elements.ElmRecord
import org.elm.lang.core.psi.elements.ElmRecordPattern
import org.elm.lang.core.psi.elements.ElmRecordType
import org.elm.lang.core.psi.elements.ElmTuplePattern
import org.elm.lang.core.psi.elements.ElmTupleType
import org.elm.lang.core.psi.elements.ElmTypeAliasDeclaration
import org.elm.lang.core.psi.elements.ElmTypeAnnotation
import org.elm.lang.core.psi.elements.ElmTypeDeclaration
import org.elm.lang.core.psi.elements.ElmTypeRef
import org.elm.lang.core.psi.elements.ElmTypeVariableRef
import org.elm.lang.core.psi.elements.ElmUnionMember
import org.elm.lang.core.psi.elements.ElmUnionPattern
import org.elm.lang.core.psi.elements.ElmUnit
import org.elm.lang.core.psi.elements.ElmUpperCaseQID
import org.elm.lang.core.psi.elements.ElmUpperPathTypeRef
import org.elm.lang.core.psi.elements.ElmValueDeclaration
import org.elm.lang.core.psi.elements.ElmValueExpr
import org.elm.lang.core.psi.elements.ElmValueQID


class ElmPsiFactory(private val project: Project)
{
    companion object {
        /**
         * WARNING: this should only be called from the [ParserDefinition] hook
         * which takes [ASTNode]s from the [PsiBuilder] and emits [PsiElement].
         *
         * IMPORTANT: Must be kept in-sync with the BNF. The grammar rules are
         * written in CamelCase, but the IElementType constants that they correspond
         * to are generated by GrammarKit in ALL_CAPS. So if you have add a rule
         * named `FooBar` to the BNF, then you need to add a line here like:
         * `FOO_BAR -> return ElmFooBar(node)`. Don't forget to create the `ElmFooBar`
         * class.
         */
        fun createElement(node: ASTNode): PsiElement {
            when (node.elementType) {
                ANONYMOUS_FUNCTION -> return ElmAnonymousFunction(node)
                AS_CLAUSE -> return ElmAsClause(node)
                CASE_OF -> return ElmCaseOf(node)
                CASE_OF_BRANCH -> return ElmCaseOfBranch(node)
                EXPOSED_OPERATOR -> return ElmExposedOperator(node)
                EXPOSED_TYPE -> return ElmExposedType(node)
                EXPOSED_VALUE -> return ElmExposedValue(node)
                EXPOSED_UNION_CONSTRUCTORS -> return ElmExposedUnionConstructors(node)
                EXPOSED_UNION_CONSTRUCTOR -> return ElmExposedUnionConstructor(node)
                EXPOSING_LIST -> return ElmExposingList(node)
                EXPRESSION -> return ElmExpression(node)
                FIELD -> return ElmField(node)
                FIELD_TYPE -> return ElmFieldType(node)
                FUNCTION_DECLARATION_LEFT -> return ElmFunctionDeclarationLeft(node)
                GLSL_CODE -> return ElmGlslCode(node)
                IF_ELSE -> return ElmIfElse(node)
                IMPORT_CLAUSE -> return ElmImportClause(node)
                INFIX_DECLARATION -> return ElmInfixDeclaration(node)
                INNER_TYPE_ANNOTATION -> return ElmInnerTypeAnnotation(node)
                INNER_VALUE_DECLARATION -> return ElmInnerValueDeclaration(node)
                LET_IN -> return ElmLetIn(node)
                LIST -> return ElmList(node)
                LIST_OF_OPERANDS -> return ElmListOfOperands(node)
                LOWER_PATTERN -> return ElmLowerPattern(node)
                LOWER_TYPE_NAME -> return ElmLowerTypeName(node)
                MODULE_DECLARATION -> return ElmModuleDeclaration(node)
                NON_EMPTY_TUPLE -> return ElmNonEmptyTuple(node)
                OPERATOR -> return ElmOperator(node)
                OPERATOR_AS_FUNCTION -> return ElmOperatorAsFunction(node)
                PARAMETRIC_TYPE_REF -> return ElmParametricTypeRef(node)
                PARENTHESED_EXPRESSION -> return ElmParenthesedExpression(node)
                PATTERN -> return ElmPattern(node)
                PATTERN_AS -> return ElmPatternAs(node)
                PORT_ANNOTATION -> return ElmPortAnnotation(node)
                RECORD -> return ElmRecord(node)
                RECORD_PATTERN -> return ElmRecordPattern(node)
                RECORD_TYPE -> return ElmRecordType(node)
                TUPLE_PATTERN -> return ElmTuplePattern(node)
                TUPLE_TYPE -> return ElmTupleType(node)
                TYPE_ALIAS_DECLARATION -> return ElmTypeAliasDeclaration(node)
                TYPE_ANNOTATION -> return ElmTypeAnnotation(node)
                TYPE_DECLARATION -> return ElmTypeDeclaration(node)
                TYPE_REF -> return ElmTypeRef(node)
                TYPE_VARIABLE_REF -> return ElmTypeVariableRef(node)
                UNION_MEMBER -> return ElmUnionMember(node)
                UNION_PATTERN -> return ElmUnionPattern(node)
                UNIT -> return ElmUnit(node)
                UPPER_PATH_TYPE_REF -> return ElmUpperPathTypeRef(node)
                VALUE_DECLARATION -> return ElmValueDeclaration(node)
                VALUE_EXPR -> return ElmValueExpr(node)
                else -> throw AssertionError("Unknown element type: " + node.elementType)
            }
        }
    }

    fun createLowerCaseIdentifier(text: String): PsiElement =
            createFromText("$text = 42", LOWER_CASE_IDENTIFIER)
                    ?: error("Failed to create lower-case identifier: `$text`")

    fun createUpperCaseIdentifier(text: String): PsiElement =
            createFromText<ElmTypeAliasDeclaration>("type alias $text = Int")
                    ?.upperCaseIdentifier
                    ?: error("Failed to create upper-case identifier: `$text`")

    fun createUpperCaseQID(text: String): ElmUpperCaseQID =
            createFromText<ElmModuleDeclaration>("module $text exposing (..)")
                    ?.upperCaseQID
                    ?: error("Failed to create upper-case QID: `$text`")

    fun createValueQID(text: String): ElmValueQID =
            createFromText<ElmValueDeclaration>("f = $text")
                    ?.expression
                    ?.childOfType<ElmValueQID>()
                    ?: error("Failed to create value QID: `$text`")

    fun createOperatorIdentifier(text: String): PsiElement =
            createFromText("foo = x $text y", OPERATOR_IDENTIFIER)
                    ?: error("Failed to create operator identifier: `$text`")

    fun createImport(moduleName: String) =
            "import $moduleName"
                    .let { createFromText<ElmImportClause>(it) }
                    ?: error("Failed to create import of $moduleName")

    fun createImportExposing(moduleName: String, exposedNames: List<String>) =
            "import $moduleName exposing (${exposedNames.joinToString(", ")})"
                    .let { createFromText<ElmImportClause>(it) }
                    ?: error("Failed to create import of $moduleName exposing $exposedNames")

    fun createValueDeclaration(name: String, argNames: List<String>): ElmValueDeclaration {
        val s = if (argNames.isEmpty())
            "$name = "
        else
            "$name ${argNames.joinToString(" ")} = "
        return createFromText(s)
                ?: error("Failed to create value declaration named $name")
    }

    fun createFreshLine() =
            // TODO [kl] make this more specific by actually find a token which contains
            // newline, not just any whitespace
            PsiFileFactory.getInstance(project)
                    .createFileFromText("DUMMY.elm", ElmFileType, "\n")
                    .descendantOfType(WHITE_SPACE)
                    ?: error("failed to create fresh line: should never happen")

    private inline fun <reified T : PsiElement> createFromText(code: String): T? =
            PsiFileFactory.getInstance(project)
                    .createFileFromText("DUMMY.elm", ElmFileType, code)
                    .childOfType<T>()

    private fun createFromText(code: String, elementType: IElementType): PsiElement? =
            PsiFileFactory.getInstance(project)
                    .createFileFromText("DUMMY.elm", ElmFileType, code)
                    .descendantOfType(elementType)
}
