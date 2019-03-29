package org.elm.utils

import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.util.DocumentUtil

fun Document.getIndent(offset: Int): String = DocumentUtil.getIndent(this, offset).toString()
fun Editor.getIndent(offset: Int): String = document.getIndent(offset)
