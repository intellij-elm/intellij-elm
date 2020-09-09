package org.elm.ide.intentions

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.elm.lang.core.psi.ElmPsiFactory
import org.elm.lang.core.psi.ElmTypes.UPPER_CASE_IDENTIFIER
import org.elm.lang.core.psi.elements.*

/**
 * An intention action that transforms a usage of record constructor to the corresponding record literal, i.e. converts
 * this:
 * ```
 * type alias MyTypeAlias = { foo: String, bar: Int }
 *
 * baz = MyTypeAlias "a" 1
 * ```
 * to this:
 * ```
 * type alias MyTypeAlias = { foo: String, bar: Int }
 *
 * baz = { foo = "a", bar = 1 }
 * ```
 */
class RecordConstructorToLiteralIntention :
    ElmAtCaretIntentionActionBase<RecordConstructorToLiteralIntention.Context>() {

    /**
     * @param functionCall The current record constructor function call, to be replaced with a record literal.
     * @param arguments The arguments to the record literal. Each entry in the list is a pair of strings: the first is
     * the argument name, the second is the argument's text.
     */
    data class Context(val functionCall: ElmFunctionCallExpr, val arguments: List<Pair<String, String>>)

    override fun getText() = "Use record literal"
    override fun getFamilyName() = text

    override fun findApplicableContext(project: Project, editor: Editor, element: PsiElement): Context? {
        // This intention is valid in a situation like this:
        //      foo = MyRecord "a" 1
        // In order to be valid the caret must be in the text "MyRecord". If the caret is inside this word (or just
        // before the M) then the PSI structure is as follows:
        //      ElmFunctionCallExpr  (this is the record constructor function)
        //          ElmValueExpr
        //              ElmUpperCaseQID
        //                  LeafPsiElement (the current element, elementType = UPPER_CASE_IDENTIFIER)
        // But if the caret is just after the last "d" of "MyRecord" the structure is as follows:
        //      ElmFunctionCallExpr  (this is the record constructor function)
        //          ElmValueExpr
        //          PsiWhiteSpace    (the current element, sibling of the ElmValueExpr above)
        val functionCall = when {
            element is LeafPsiElement &&
                    element.elementType == UPPER_CASE_IDENTIFIER &&
                    element.parent is ElmUpperCaseQID &&
                    element.parent.parent is ElmValueExpr &&
                    element.parent.parent.parent is ElmFunctionCallExpr -> element.parent.parent.parent as ElmFunctionCallExpr
            element is PsiWhiteSpace &&
                    element.prevSibling is ElmValueExpr &&
                    element.parent is ElmFunctionCallExpr -> element.parent as ElmFunctionCallExpr
            else -> return null
        }

        // If we get here, we are in a function call. Check if that function resolves to a record type alias.
        val recordArgNames = ((functionCall.target.reference?.resolve() as? ElmTypeAliasDeclaration)
            // The logic in the next three lines is what's in ElmTypeAliasDeclaration.isRecordAlias, but we duplicate it
            // here to get at the actual ElmRecordType object.
            ?.typeExpression
            ?.allSegments
            ?.firstOrNull() as? ElmRecordType)
            ?.fieldTypeList
            ?.map { field -> field.lowerCaseIdentifier.text }
            ?: return null

        // Check the number of args to the function match the number of args expected by the type alias.
        val args = functionCall.arguments.toList()
            .takeIf { it.size == recordArgNames.size }
            ?.mapIndexed { index, elmAtomTag -> recordArgNames[index] to elmAtomTag.text }
            ?: return null

        return Context(functionCall, args)
    }

    override fun invoke(project: Project, editor: Editor, context: Context) {
        context.functionCall.replace(
            ElmPsiFactory(project).createRecordExpr(context.arguments)
        )
    }
}
