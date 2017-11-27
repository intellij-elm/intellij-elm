package org.elm.ide.annotator

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.psi.PsiElement
import org.elm.lang.core.resolve.scope.GlobalScope

class ElmUnresolvedReferenceAnnotator : Annotator {

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        val ref = element.reference ?: return

        if (ref.resolve() == null) {
            // TODO [kl] re-visit this hack for suppressing errors on built-in symbols
            // Kamil's plugin handled it by returning null for getReference() when the
            // referring name is a built-in symbol, but our `ElmReferenceElement` says
            // requires that `getReference()` returns a non-null value. We could relax
            // this restriction, or maybe this hack is ok?
            if (GlobalScope.builtInSymbols.contains(ref.canonicalText))
                return

            holder.createErrorAnnotation(element, "Unresolved reference '${ref.canonicalText}'")
        }
    }
}