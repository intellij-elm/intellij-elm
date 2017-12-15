package org.elm.lang.core.completion

/**
 * Provide code completion for Elm's keywords
 */
// TODO [kl] fixme
// as of 2017-12-15, this is totally busted because the parse error recovery is poor
// which results in a bad Psi tree that is hard to work with. It may also be smart
// if we take the time to learn how [PsiElementPattern] and associated classes work
// as they may be more flexible than writing ad-hoc code to inspect the tree.
/*
object ElmKeywordSuggestor: Suggestor {

    override fun addCompletions(parameters: CompletionParameters, result: CompletionResultSet) {
        val pos = parameters.position
        val parent = pos.parent
        val grandParent = pos.parent?.parent
        val file = pos.containingFile as ElmFile

        if (pos.elementType == LOWER_CASE_IDENTIFIER && parent is ElmFile) {
            val prevVisibleLeaf = PsiTreeUtil.prevVisibleLeaf(pos)
            if (prevVisibleLeaf == null || prevVisibleLeaf.text == "\n") {
                // at the beginning of the file or beginning of a line
                result.add("type")
                result.add("module")
                result.add("import")
            }

            if (prevVisibleLeaf?.elementType == TYPE) {
                result.add("alias")
            }

            if (prevVisibleLeaf?.elementType == UPPER_CASE_IDENTIFIER
                    && PsiTreeUtil.prevVisibleLeaf(prevVisibleLeaf!!)?.elementType in setOf(MODULE, IMPORT)) {
                result.add("exposing")
            }
        }
    }

    private fun CompletionResultSet.add(keyword: String) {
        var builder = LookupElementBuilder.create(keyword)
        builder = addInsertionHandler(keyword, builder)
        addElement(builder)
    }

    private val ALWAYS_NEEDS_SPACE = setOf("type", "alias", "module", "import")


    private fun addInsertionHandler(keyword: String, item: LookupElementBuilder): LookupElementBuilder {
        when (keyword) {
            in ALWAYS_NEEDS_SPACE ->
                return item.withInsertHandler({ ctx, _ -> ctx.addSuffix(" ") })

            "exposing" ->
                return item.withInsertHandler({ ctx, _ -> ctx.addSuffix(" ()", moveCurserLeft = 1) })

            else ->
                return item
        }


    }


    fun InsertionContext.addSuffix(suffix: String, moveCurserLeft: Int = 0) {
        document.insertString(selectionEndOffset, suffix)
        EditorModificationUtil.moveCaretRelatively(editor, suffix.length - moveCurserLeft)
    }
}*/
