package org.elm.ide.presentation

import com.intellij.ide.projectView.PresentationData
import com.intellij.navigation.ItemPresentation
import com.intellij.psi.PsiElement
import org.elm.lang.core.psi.ElmNamedElement
import org.elm.lang.core.psi.ElmPsiElement


fun getPresentation(psi: ElmPsiElement): ItemPresentation {
    val name = presentableName(psi)
    val location = "(in ${psi.containingFile.name})"
    return PresentationData(name, location, psi.getIcon(0), null)
}


private fun presentableName(element: PsiElement): String? {
    return when (element) {
        is ElmNamedElement -> element.name
        else -> null
    }

}