package org.elm.lang.core.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.editor.EditorModificationUtil
import com.intellij.util.ProcessingContext

// TODO [kl] this is based on how the Rust plugin did keyword completion
// but I'm not using it until I figure out how IntelliJ's pattern DSL works.
class ElmKeywordCompletionProvider(private vararg val keywords: String
): CompletionProvider<CompletionParameters>() {


    override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext?, result: CompletionResultSet) {
        for (keyword in keywords) {
            var item = LookupElementBuilder.create(keyword)
            item = addInsertionHandler(keyword, item)
            result.addElement(item)
        }
    }


    private val ALWAYS_NEEDS_SPACE = setOf("type", "alias", "module", "import")


    private fun addInsertionHandler(keyword: String, item: LookupElementBuilder): LookupElementBuilder {
        val suffix = when (keyword) {
            in ALWAYS_NEEDS_SPACE -> " "
            else -> return item
        }

        return item.withInsertHandler({ ctx, _ -> ctx.addSuffix(suffix) })
    }


    fun InsertionContext.addSuffix(suffix: String) {
        document.insertString(selectionEndOffset, suffix)
        EditorModificationUtil.moveCaretRelatively(editor, suffix.length)
    }

}