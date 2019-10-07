package org.elm.ide.commenter

import com.intellij.codeInsight.generation.IndentedCommenter

class ElmCommenter : IndentedCommenter {
    override fun getLineCommentPrefix() =
            "--"

    override fun getBlockCommentPrefix() =
            "{-"

    override fun getBlockCommentSuffix() =
            "-}"

    override fun getCommentedBlockCommentPrefix() =
            "{-"

    override fun getCommentedBlockCommentSuffix() =
            "-}"

    override fun forceIndentedLineComment() =
            true
}