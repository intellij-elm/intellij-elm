package org.elm.ide.commenter

import com.intellij.lang.Commenter

class ElmCommenter: Commenter {
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
}