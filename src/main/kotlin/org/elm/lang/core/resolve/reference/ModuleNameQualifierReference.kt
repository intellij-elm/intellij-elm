package org.elm.lang.core.resolve.reference

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.elm.lang.core.psi.ElmNamedElement
import org.elm.lang.core.psi.ElmPsiFactory
import org.elm.lang.core.psi.ElmQID
import org.elm.lang.core.psi.elements.ElmUpperCaseQID
import org.elm.lang.core.psi.elements.ElmValueQID
import org.elm.lang.core.psi.offsetIn
import org.elm.lang.core.resolve.ElmReferenceElement
import org.elm.lang.core.resolve.scope.GlobalScope
import org.elm.lang.core.resolve.scope.ModuleScope
import org.elm.lang.core.stubs.index.ElmModulesIndex

/**
 * A module-name (or alias) reference which qualifies a name from the value or type namespaces.
 *
 * e.g. `Data.User` in the expression `Data.User.name defaultUser`
 *
 * e.g. the `DU` alias in the expression `DU.name` in the program:
 * ```
 * import Data.User as DU
 * f x = DU.name x
 * ```
 *
 * @param elem the Psi element which owns the reference
 * @param elementQID the QID (qualified ID) element within `elem`
 */
class ModuleNameQualifierReference<T : ElmReferenceElement>(
        elem: T,
        private val elementQID: ElmQID, // IMPORTANT: do not use in stub-based codepaths; denormalize instead!
        qualifierPrefix: String // denormalized from [elementQID] to support Stubs
) : ElmReferenceCached<T>(elem), ElmReference {

    private val refText: String = qualifierPrefix

    override fun resolveInner(): ElmNamedElement? {
        val clientFile = element.elmFile
        val importDecls = ModuleScope.getImportDecls(clientFile)

        // First, check to see if it resolves to an aliased import
        for (decl in importDecls) {
            val asClause = decl.asClause
            if (asClause?.name == refText) return asClause
        }

        // Otherwise, try to resolve the import directly
        val targetModuleName = GlobalScope.defaultAliases[refText] ?: refText
        val targetDecl = ElmModulesIndex.get(targetModuleName, clientFile)
                ?: return null

        // Ensure that it's in scope
        return when {
            targetModuleName in GlobalScope.defaultImports -> targetDecl
            importDecls.any { it.moduleQID.text == refText } -> targetDecl
            else -> null
        }
    }

    override fun getVariants(): Array<ElmNamedElement> {
        // Code-completion of Elm module names is not done via the PsiReference 'variant' system.
        // Instead, see `ElmCompletionProvider`
        return emptyArray()
    }

    override fun calculateDefaultRangeInElement(): TextRange {
        val startOffset = elementQID.offsetIn(element)
        return TextRange(startOffset, startOffset + refText.length)
    }

    override fun handleElementRename(newElementName: String): PsiElement {
        val factory = ElmPsiFactory(element.project)
        val nameParts = elementQID.text.split(".")
        val newName = newElementName + "." + nameParts.last()
        val newId = when (elementQID) {
            is ElmUpperCaseQID -> factory.createUpperCaseQID(newName)
            is ElmValueQID -> factory.createValueQID(newName)
            else -> error("unexpected QID type")
        }
        elementQID.replace(newId)
        return element
    }
}
