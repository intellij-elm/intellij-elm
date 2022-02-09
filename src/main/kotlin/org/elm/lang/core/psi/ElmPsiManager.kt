/*
* Use of this source code is governed by the MIT license that can be
* found in the LICENSE file.
*
* Originally from intellij-rust
*/

package org.elm.lang.core.psi

import com.intellij.injected.editor.VirtualFileWindow
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SimpleModificationTracker
import com.intellij.psi.*
import com.intellij.psi.PsiTreeChangeEvent.PROP_WRITABLE
import com.intellij.psi.impl.PsiTreeChangeEventImpl
import com.intellij.psi.util.PsiModificationTracker
import org.elm.lang.core.ElmFileType
import org.elm.lang.core.psi.elements.ElmFunctionDeclarationLeft

@Service(Service.Level.PROJECT)
class ElmPsiManager(val project: Project) {
    /**
     * A modification tracker that is incremented on PSI changes that can affect non-local references or inference.
     *
     * It is incremented whenever a non-whitespace, non-comment change is made to the PSI of an Elm
     * file outside of function bodies.
     */
    val modificationTracker = SimpleModificationTracker()

    init {
        PsiManager.getInstance(project).addPsiTreeChangeListener(CacheInvalidator(), project)
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
            // If the file is null, then this is an event about VFS changes
            val file = event.file
            if (file == null && (element is ElmFile || element is PsiDirectory)) {
                modificationTracker.incModificationCount()
                return
            }

            if (file?.fileType != ElmFileType) return
            if (element is PsiComment || element is PsiWhiteSpace) return

            updateModificationCount(element)
        }

        private fun updateModificationCount(element: PsiElement) {
            // If something is changed inside an annotated function, we will only increment the
            // function local modification counter. Otherwise, we will increment the global
            // modification counter.

            val owner = element.outermostDeclaration(strict = false)
            if (owner?.typeAnnotation == null ||
                    // Invalidate globally if we change the name of a top level declaration
                    (owner.assignee as? ElmFunctionDeclarationLeft)?.lowerCaseIdentifier == element) {
                modificationTracker.incModificationCount()
            } else {
                owner.modificationTracker.incModificationCount()
            }
        }
    }
}

private val Project.elmPsiManager: ElmPsiManager
    get() = getService(ElmPsiManager::class.java)

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
