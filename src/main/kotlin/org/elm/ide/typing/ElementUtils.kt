package org.elm.ide.typing

import com.intellij.psi.PsiElement
import org.elm.lang.core.psi.ElmFile
import org.elm.lang.core.psi.ancestors

/** Return the string that should indent an element based on the number of ancestors it has */
fun guessIndent(element: PsiElement, offset: Int = -1): String =
        "    ".repeat(element.ancestors.takeWhile { it !is ElmFile }.count() + offset)
