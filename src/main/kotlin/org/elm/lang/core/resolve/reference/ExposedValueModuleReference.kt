package org.elm.lang.core.resolve.reference

import com.intellij.psi.PsiElement
import org.elm.lang.core.psi.ElmNamedElement
import org.elm.lang.core.psi.elements.ElmExposedValue
import org.elm.lang.core.resolve.scope.ModuleScope

/**
 * A value reference from an 'exposing' list in a module declaration (points within the same file)
 */
class ExposedValueModuleReference(exposedValue: ElmExposedValue) : ElmReferenceBase<ElmExposedValue>(exposedValue) {

    override fun getVariants(): Array<ElmNamedElement> {
        return ModuleScope(element.elmFile).getDeclaredValues().toTypedArray()
    }

    override fun resolve(): PsiElement? {
        return getVariants().firstOrNull { it.name == element.referenceName }
    }
}