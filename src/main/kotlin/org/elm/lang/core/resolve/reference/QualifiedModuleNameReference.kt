package org.elm.lang.core.resolve.reference

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import org.elm.lang.core.psi.ElmNamedElement
import org.elm.lang.core.psi.ElmPsiElement
import org.elm.lang.core.psi.ElmPsiFactory
import org.elm.lang.core.psi.ElmQID
import org.elm.lang.core.psi.childOfType
import org.elm.lang.core.psi.elements.ElmModuleDeclaration
import org.elm.lang.core.psi.elements.ElmUpperCaseQID
import org.elm.lang.core.psi.elements.ElmValueQID
import org.elm.lang.core.psi.offsetIn
import org.elm.lang.core.resolve.scope.GlobalScope
import org.elm.lang.core.resolve.scope.ImportScope
import org.elm.lang.core.resolve.scope.ModuleScope

/**
 * Qualified module-name reference from the value or type namespaces.
 *
 * e.g. `Data.User` in the expression `Data.User.name defaultUser`
 *
 * @param elem the Psi element which owns the reference
 * @param elementQID the QID (qualified ID) element within `elem`
 */
class QualifiedModuleNameReference<T:ElmPsiElement>(
        elem: T,
        val elementQID: ElmQID
): PsiReferenceBase<T>(elem), ElmReference {

    override fun getVariants(): Array<ElmNamedElement> {
        // TODO [kl] consider inverting this loop to reduce iterations
        val moduleDecls = ImportScope.allElmFiles(element.project)
                .mapNotNull { it.childOfType<ElmModuleDeclaration>() }
                .filter { ModuleScope(element.elmFile).importsModule(it.name)
                    || GlobalScope.implicitlyImportsModule(it)
                }

        val aliasDecls = ModuleScope(element.elmFile).getAliasDecls() as List<ElmNamedElement>

        return listOf(moduleDecls, aliasDecls).flatten().toTypedArray()
    }

    override fun resolve(): ElmPsiElement? {
        return getVariants().find { it.name == refText }
    }

    val refText: String
        get() = elementQID.text.split(".").dropLast(1).joinToString(".")

    override fun calculateDefaultRangeInElement(): TextRange {
        val startOffset = elementQID.offsetIn(element)
        return TextRange(startOffset, startOffset + refText.length)
    }

    override fun handleElementRename(newModuleName: String): PsiElement {
        val factory = ElmPsiFactory(element.project)
        val nameParts = elementQID.text.split(".")
        val newName = newModuleName + "." + nameParts.last()
        val newId = when (elementQID) {
            is ElmUpperCaseQID -> factory.createUpperCaseQID(newName)
            is ElmValueQID -> factory.createValueQID(newName)
            else -> error("unexpected QID type")
        }
        elementQID.replace(newId)
        return element
    }
}