package org.elm.lang.core.resolve.reference

import com.intellij.psi.PsiElement
import org.elm.lang.core.psi.ElmNamedElement
import org.elm.lang.core.psi.elements.ElmExposedType
import org.elm.lang.core.resolve.scope.ModuleScope

/**
 * Reference from the name of a type in the module's exposing list to its definition
 * in the same file.
 */
class ExposedTypeReferenceFromModuleDecl(exposedType: ElmExposedType) : ElmReferenceBase<ElmExposedType>(exposedType) {

    override fun getVariants(): Array<ElmNamedElement> {
        return ModuleScope(element.elmFile).getDeclaredTypes().toTypedArray()
    }

    override fun resolve(): PsiElement? {
        return getVariants().firstOrNull { it.name == element.referenceName }
    }
}