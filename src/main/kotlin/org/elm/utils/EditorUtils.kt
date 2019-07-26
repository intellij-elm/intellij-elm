package org.elm.utils

import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.util.DocumentUtil

/**
 * Calculates indent of the line containing [offset]
 * @return Whitespaces at the beginning of the line
 */
fun Document.getIndent(offset: Int): String = DocumentUtil.getIndent(this, offset).toString()

/**
 * Calculates indent of the line containing [offset]
 * @return Whitespaces at the beginning of the line
 */
fun Editor.getIndent(offset: Int): String = document.getIndent(offset)
