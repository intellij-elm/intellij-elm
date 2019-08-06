package org.elm.lang.core.resolve.reference

import com.intellij.psi.impl.source.resolve.ResolveCache
import org.elm.lang.core.psi.ElmNamedElement
import org.elm.lang.core.resolve.ElmReferenceElement


/**
 * A reference that will resolve to at most one element. The resolve result is cached.
 */
abstract class ElmReferenceCached<T : ElmReferenceElement>(element: T)
    : ElmReference, ElmReferenceBase<T>(element) {

    abstract fun resolveInner(): ElmNamedElement?

    final override fun multiResolve(): List<ElmNamedElement> {
        return ResolveCache.getInstance(element.project)
                .resolveWithCaching(this, Resolver, true, false)
                ?.let { listOf(it) }.orEmpty()
    }

    private object Resolver : ResolveCache.AbstractResolver<ElmReferenceCached<*>, ElmNamedElement?> {
        override fun resolve(ref: ElmReferenceCached<*>, incompleteCode: Boolean): ElmNamedElement? {
            return ref.resolveInner()
        }
    }
}
