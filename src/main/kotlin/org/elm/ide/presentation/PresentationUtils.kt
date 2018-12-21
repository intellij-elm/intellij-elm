package org.elm.ide.presentation

import com.intellij.ide.projectView.PresentationData
import com.intellij.navigation.ItemPresentation
import com.intellij.psi.PsiElement
import org.elm.lang.core.psi.ElmFile
import org.elm.lang.core.psi.ElmNamedElement
import org.elm.lang.core.psi.ElmPsiElement
import org.elm.lang.core.psi.ancestors
import org.elm.lang.core.psi.elements.ElmValueDeclaration


fun getPresentation(psi: ElmPsiElement): ItemPresentation {
    val name = presentableName(psi)
    val location = "(in ${psi.containingFile.name})"
    return PresentationData(name, location, psi.getIcon(0), null)
}


fun getPresentationForStructure(psi: ElmPsiElement): ItemPresentation {
    val text = presentableName(psi)
    // in the structure view, we might want to extend [text] to include
    // things like the full type annotation for a function declaration.
    return PresentationData(text, null, psi.getIcon(0), null)
}


private fun presentableName(element: PsiElement): String? =
        when (element) {
            is ElmNamedElement ->
                element.name

            is ElmValueDeclaration ->
                element.declaredNames(includeParameters = false).firstOrNull()?.name

            else ->
                null
        }

/** Return the string that should indent an element based on the number of ancestors it has */
fun guessIndent(element: PsiElement, offset: Int = -1): String =
        "    ".repeat(element.ancestors.takeWhile { it !is ElmFile }.count() + offset)
