package org.elm.ide.refactoring

import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.PsiElement
import com.intellij.refactoring.rename.RenamePsiFileProcessor
import org.elm.lang.core.psi.ElmFile
import org.elm.lang.core.psi.elements.ElmModuleDeclaration


/**
 * When renaming an Elm file, also rename any [ElmModuleDeclaration]s (which, transitively,
 * will also rename all imports and qualified references).
 *
 * See https://intellij-support.jetbrains.com/hc/en-us/community/posts/206760415-Renaming-files-in-IDE
 * and [ElmRenamePsiElementProcessor]
 */
class ElmRenamePsiFileProcessor : RenamePsiFileProcessor() {

    override fun canProcessElement(element: PsiElement) =
            element is ElmFile


    override fun prepareRenaming(element: PsiElement, newName: String, allRenames: MutableMap<PsiElement, String>) {
        val file = element as? ElmFile
                ?: return

        val moduleDecl = file.getModuleDecl()
                ?: return

        val newModuleName = FileUtil.getNameWithoutExtension(newName)
        if (!isValidUpperIdentifier(newModuleName))
            return

        // When renaming the module, we must only replace the final part of the
        // module name. Given a module `Foo.Bar` in file `src/Foo/Bar.elm`, if we want to
        // rename `Bar.elm` to `Quux.elm`, the new module name will be `Foo.Quux`.

        val moduleParts = moduleDecl.upperCaseQID.upperCaseIdentifierList.map { it.text }.toMutableList()
        moduleParts[moduleParts.size - 1] = newModuleName

        allRenames[moduleDecl] = moduleParts.joinToString(".")
    }
}