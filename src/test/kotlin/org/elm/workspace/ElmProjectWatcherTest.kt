/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 *
 * Originally from intellij-rust
 */

package org.elm.workspace

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import org.elm.lang.ElmTestBase


class ElmProjectWatcherTest: ElmTestBase() {


    private var counter = 0
    private lateinit var watcher: ElmProjectWatcher


    override fun setUp() {
        super.setUp()
        watcher = ElmProjectWatcher { counter++ }
    }


    fun `test detects a change involving elm json project manifest file`() {
        val vFile = newVirtualFile(ElmToolchain.ELM_JSON)
        watcher.checkTriggered(newCreateEvent(vFile))
        watcher.checkTriggered(newChangeEvent(vFile))
        watcher.checkTriggered(newDeleteEvent(vFile))
        runWriteAction { vFile.delete(this) }
    }


    // TODO [drop 0.18] remove this test
    fun `test detects a change involving legacy elm json project manifest file`() {
        val vFile = newVirtualFile(ElmToolchain.ELM_LEGACY_JSON)
        watcher.checkTriggered(newCreateEvent(vFile))
        watcher.checkTriggered(newChangeEvent(vFile))
        watcher.checkTriggered(newDeleteEvent(vFile))
        runWriteAction { vFile.delete(this) }
    }


    fun `test ignores files other than elm json`() {
        val vFile = newVirtualFile("foo.json")
        watcher.checkNotTriggered(newCreateEvent(vFile))
        watcher.checkNotTriggered(newChangeEvent(vFile))
        watcher.checkNotTriggered(newDeleteEvent(vFile))
        runWriteAction { vFile.delete(this) }
    }


    private fun ElmProjectWatcher.checkTriggered(event: VFileEvent) {
        val old = counter
        after(listOf(event))
        check(counter == old + 1) {
            "Watcher ignored $event"
        }
    }


    private fun ElmProjectWatcher.checkNotTriggered(event: VFileEvent) {
        val old = counter
        after(listOf(event))
        check(counter == old) {
            "Watcher should have ignored $event"
        }
    }


    private fun newCreateEvent(vFile: VirtualFile) =
            VFileCreateEvent(null, vFile.parent, vFile.name, false, true)


    private fun newChangeEvent(vFile: VirtualFile) =
            VFileContentChangeEvent(null, vFile, vFile.modificationStamp - 1, vFile.modificationStamp, true)


    private fun newDeleteEvent(vFile: VirtualFile) =
            VFileDeleteEvent(null, vFile, true)


    private fun newVirtualFile(name: String) =
            myFixture.tempDirFixture.createFile("proj-watcher/$name")
}