package org.elm.ide.refactoring

import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.refactoring.rename.RenamePsiFileProcessor
import org.elm.lang.core.psi.elements.ElmModuleDeclaration


/**
 * When renaming an Elm module, always treat it as if the user were renaming the file.
 *
 * See https://intellij-support.jetbrains.com/hc/en-us/community/posts/206760415-Renaming-files-in-IDE
 * and [ElmRenamePsiFileProcessor].
 */
class ElmRenamePsiElementProcessor : RenamePsiFileProcessor() {


    override fun canProcessElement(element: PsiElement) =
            element is ElmModuleDeclaration


    override fun substituteElementToRename(element: PsiElement, editor: Editor?): PsiElement? {
        return when (element) {
            is ElmModuleDeclaration -> element.elmFile
            else -> element
        }
    }
}