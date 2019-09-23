/*
* Use of this source code is governed by the MIT license that can be
* found in the LICENSE file.
*
* Originally from intellij-rust
*/

package org.elm.lang.core.psi

import com.intellij.injected.editor.VirtualFileWindow
import com.intellij.openapi.components.ProjectComponent
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SimpleModificationTracker
import com.intellij.psi.*
import com.intellij.psi.PsiTreeChangeEvent.PROP_WRITABLE
import com.intellij.psi.impl.PsiTreeChangeEventImpl
import com.intellij.psi.util.PsiModificationTracker
import org.elm.lang.core.ElmFileType

class ElmPsiManager(val project: Project) : ProjectComponent {
    /**
     * A modification tracker that is incremented on PSI changes that can affect non-local references or inference.
     *
     * It is incremented whenever a non-whitespace, non-comment change is made to the PSI of an Elm
     * file outside of function bodies.
     */
    val modificationTracker = SimpleModificationTracker()

    override fun projectOpened() {
        PsiManager.getInstance(project).addPsiTreeChangeListener(CacheInvalidator())
    }

    inner class CacheInvalidator : PsiTreeChangeAdapter() {
        override fun beforeChildRemoval(event: PsiTreeChangeEvent) = onPsiChange(event, event.child)
        override fun beforeChildReplacement(event: PsiTreeChangeEvent) = onPsiChange(event, event.oldChild)
        override fun beforeChildMovement(event: PsiTreeChangeEvent) = onPsiChange(event, event.child)
        override fun childReplaced(event: PsiTreeChangeEvent) = onPsiChange(event, event.newChild)
        override fun childAdded(event: PsiTreeChangeEvent) = onPsiChange(event, event.child)
        override fun childMoved(event: PsiTreeChangeEvent) = onPsiChange(event, event.child)
        override fun childrenChanged(event: PsiTreeChangeEvent) {
            // `GenericChange` event means that "something changed in the file" and sends
            // after all events for concrete PSI changes in a file.
            // We handle more concrete events and so should ignore the generic event.
            if (event !is PsiTreeChangeEventImpl || !event.isGenericChange) onPsiChange(event, event.parent)
        }

        override fun propertyChanged(event: PsiTreeChangeEvent) {
            if (event.propertyName != PROP_WRITABLE && event.element != null) {
                onPsiChange(event, event.element)
            }
        }

        private fun onPsiChange(event: PsiTreeChangeEvent, element: PsiElement) {


            // There are some cases when PsiFile stored in the event as a child
            // e.g. file removal by external VFS change
            val file = event.file ?: event.child as? PsiFile
            if (file?.fileType != ElmFileType) return

            val child = event.child
            if (child is PsiComment || child is PsiWhiteSpace) return

            updateModificationCount(child ?: event.parent)
        }

        private fun updateModificationCount(element: PsiElement) {
            // If something is changed inside a function, we will only increment the function local
            // modification counter. Otherwise, we will increment the global modification counter.

            val owner = element.outermostDeclaration(strict = true)
            if (owner == null) {
                modificationTracker.incModificationCount()
            } else {
                owner.modificationTracker.incModificationCount()
            }
        }
    }
}

private val Project.elmPsiManager: ElmPsiManager
    get() = getComponent(ElmPsiManager::class.java)

/** @see ElmPsiManager.modificationTracker */
val Project.modificationTracker: SimpleModificationTracker
    get() = elmPsiManager.modificationTracker

/**
 * Return [ElmPsiManager.modificationTracker], or [PsiModificationTracker.MODIFICATION_COUNT] if
 * this element is in a language injection.
 */
val ElmPsiElement.globalModificationTracker: Any
    get() = elmFile.globalModificationTracker

val ElmFile.globalModificationTracker: Any
    get() = when (virtualFile) {
        // If the element is in a language injection (e.g. a Kotlin string literal with Elm
        // injected, like we use in this project's tests), then we are never notified of PSI
        // change events in the injected code; we have to invalidate the cache after any PSI
        // change in the project.
        is VirtualFileWindow -> PsiModificationTracker.MODIFICATION_COUNT
        else -> project.modificationTracker
    }
