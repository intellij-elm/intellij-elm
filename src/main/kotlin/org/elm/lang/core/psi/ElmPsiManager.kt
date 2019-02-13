/*
* Use of this source code is governed by the MIT license that can be
* found in the LICENSE file.
*
* Originally from intellij-rust
*/

package org.elm.lang.core.psi

import com.intellij.openapi.components.ProjectComponent
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SimpleModificationTracker
import com.intellij.psi.*
import com.intellij.psi.impl.PsiTreeChangeEventImpl
import org.elm.lang.core.ElmFileType
import org.elm.lang.core.psi.elements.ElmValueDeclaration

class ElmPsiManager(val project: Project) : ProjectComponent {
    val modificationTracker = SimpleModificationTracker()

    override fun projectOpened() {
        PsiManager.getInstance(project).addPsiTreeChangeListener(CacheInvalidator())
    }

    inner class CacheInvalidator : PsiTreeChangeAdapter() {
        override fun beforeChildRemoval(event: PsiTreeChangeEvent) = onPsiChange(event)
        override fun childReplaced(event: PsiTreeChangeEvent) = onPsiChange(event)
        override fun childAdded(event: PsiTreeChangeEvent) = onPsiChange(event)
        override fun childrenChanged(event: PsiTreeChangeEvent) = onPsiChange(event)
        override fun childMoved(event: PsiTreeChangeEvent) = onPsiChange(event)
        override fun propertyChanged(event: PsiTreeChangeEvent) = onPsiChange(event)

        private fun onPsiChange(event: PsiTreeChangeEvent) {
            // `GenericChange` event means that "something changed in the file" and sends
            // after all events for concrete PSI changes in a file.
            // We handle more concrete events and so should ignore generic event
            if (event is PsiTreeChangeEventImpl && event.isGenericChange) return

            // There are some cases when PsiFile stored in the event as a child
            // e.g. file removal by external VFS change
            val file = event.file ?: event.child as? PsiFile
            if (file?.fileType != ElmFileType) return

            val child = event.child
            if (child is PsiComment || child is PsiWhiteSpace) return

            updateModificationCount(child ?: event.parent)
        }

        private fun updateModificationCount(element: PsiElement) {
            // If something is changed inside a function, we will only
            // increment the function local modification counter. Otherwise, we will increment the
            // global modification counter.

            val owner = element.ancestors
                    .drop(1) // skip self
                    .takeWhile { it !is ElmFile }
                    .filterIsInstance<ElmValueDeclaration>()
                    .firstOrNull { it.isTopLevel }
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

val Project.modificationTracker: SimpleModificationTracker
    get() = elmPsiManager.modificationTracker
