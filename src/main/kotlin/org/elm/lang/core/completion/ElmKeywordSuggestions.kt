package org.elm.lang.core.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.editor.EditorModificationUtil
import com.intellij.psi.util.PsiTreeUtil
import org.elm.lang.core.psi.ElmExpressionTag
import org.elm.lang.core.psi.ElmTypes.*
import org.elm.lang.core.psi.elementType
import org.elm.lang.core.psi.elements.ElmCaseOfExpr
import org.elm.lang.core.psi.elements.ElmFunctionCallExpr
import org.elm.lang.core.psi.elements.ElmIfElseExpr
import org.elm.lang.core.psi.elements.ElmLetInExpr

/**
 * Provide code completion for Elm's keywords
 */
object ElmKeywordSuggestor : Suggestor {

    override fun addCompletions(parameters: CompletionParameters, result: CompletionResultSet) {
        val pos = parameters.position
        val parent = pos.parent
        val grandParent = pos.parent?.parent

        if (pos.elementType == LOWER_CASE_IDENTIFIER) {
            // NOTE TO SELF:
            // PsiTreeUtil.prevVisibleLeaf() skips over PsiErrorElement, which is good,
            //                               but also skips the VIRTUAL_END_DECL tokens
            // PsiElement#getPrevSibling() includes VIRTUAL_END_DECL tokens,
            //                             but does not skip over PsiErrorElement, which is bad
            // TODO [kl] find a less brittle way to express this. Maybe using IntelliJ's pattern DSL.
            val prevVisibleLeaf = PsiTreeUtil.prevVisibleLeaf(pos)
            if (prevVisibleLeaf == null || pos.prevSibling?.text == "\n") {
                // at the beginning of the file or beginning of a line
                // TODO [kl] make this more restrictive
                // (i.e. we shouldn't suggest "module" when we are in the middle of the file)
                result.add("type")
                result.add("module")
                result.add("import")
            }

            if (prevVisibleLeaf?.elementType == TYPE) {
                result.add("alias")
            }

            // Import declarations
            val beforeParentType = PsiTreeUtil.skipWhitespacesAndCommentsBackward(parent)?.elementType
            if (prevVisibleLeaf?.elementType == UPPER_CASE_IDENTIFIER && beforeParentType == IMPORT_CLAUSE) {
                result.add("as")
                result.add("exposing")
            }

            // Module declaration
            if (prevVisibleLeaf?.elementType == UPPER_CASE_IDENTIFIER && parent.elementType == MODULE_DECLARATION) {
                result.add("exposing")
            }

            // various expressions
            if (PsiTreeUtil.getParentOfType(pos, ElmExpressionTag::class.java) != null) {
                result.add("if")
                result.add("case")
                result.add("let")
            }

            // keywords within the context of an 'if' expression
            if (PsiTreeUtil.getParentOfType(pos, ElmIfElseExpr::class.java) != null) {
                result.add("then")
                result.add("else")
            }

            // the 'of' in a 'case' expression
            val caseOfExpr = PsiTreeUtil.getParentOfType(pos, ElmCaseOfExpr::class.java)
            if (caseOfExpr != null) {
                // boy this is ugly. I either need to level-up my PsiTreeUtil-fu or learn how to use PsiPattern.
                val functionCall = PsiTreeUtil.getParentOfType(pos, ElmFunctionCallExpr::class.java)
                if (functionCall?.parent?.parent == caseOfExpr && grandParent?.parent == functionCall) {
                    result.add("of")
                }
            }

            // the 'in' in a 'let' expression
            // TODO [kl] this is not nearly specific enough, but it will do for now
            if (PsiTreeUtil.getParentOfType(pos, ElmLetInExpr::class.java) != null) {
                result.add("in")
            }
        }
    }

    private fun CompletionResultSet.add(keyword: String) {
        var builder = LookupElementBuilder.create(keyword)
        builder = addInsertionHandler(keyword, builder)
        addElement(builder)
    }

    private val ALWAYS_NEEDS_SPACE = setOf("type", "alias", "module", "import", "as", "if", "else", "case")


    private fun addInsertionHandler(keyword: String, item: LookupElementBuilder): LookupElementBuilder {
        when (keyword) {
            in ALWAYS_NEEDS_SPACE ->
                return item.withInsertHandler { ctx, _ -> ctx.addSuffix(" ") }

            "exposing" ->
                return item.withInsertHandler { ctx, _ -> ctx.addSuffix(" ()", moveCursorLeft = 1) }

            else ->
                return item
        }
    }


    private fun InsertionContext.addSuffix(suffix: String, moveCursorLeft: Int = 0) {
        document.insertString(selectionEndOffset, suffix)
        EditorModificationUtil.moveCaretRelatively(editor, suffix.length - moveCursorLeft)
    }
}
