package org.elm.lang.core.resolve.reference

import com.intellij.psi.impl.source.resolve.ResolveCache
import org.elm.lang.core.psi.ElmNamedElement
import org.elm.lang.core.resolve.ElmReferenceElement


abstract class ElmReferenceCached<T : ElmReferenceElement>(element: T)
    : ElmReference, ElmReferenceBase<T>(element) {

    abstract fun resolveInner(): ElmNamedElement?

    override fun resolve(): ElmNamedElement? {
        return ResolveCache.getInstance(element.project)
                .resolveWithCaching(this, Resolver, true, false)
    }

    private object Resolver : ResolveCache.AbstractResolver<ElmReferenceCached<*>, ElmNamedElement?> {
        override fun resolve(ref: ElmReferenceCached<*>, incompleteCode: Boolean): ElmNamedElement? {
            return ref.resolveInner()
        }
    }
}