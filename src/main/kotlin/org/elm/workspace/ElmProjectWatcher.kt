package org.elm.workspace

import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent
import org.elm.workspace.ElmToolchain.Companion.ELM_INTELLIJ_JSON
import org.elm.workspace.ElmToolchain.Companion.ELM_JSON
import org.elm.workspace.ElmToolchain.Companion.ELM_LEGACY_JSON


/**
 * Watches for changes to any `elm.json` files
 */
class ElmProjectWatcher(val onChange: () -> Unit) : BulkFileListener {


    override fun after(events: List<VFileEvent>) {
        if (events.any(::isInterestingEvent)) {
            onChange()
        }
    }

}


private fun isInterestingEvent(event: VFileEvent) =
        event.pathEndsWith(ELM_JSON)
                || event.pathEndsWith(ELM_INTELLIJ_JSON)
                || event.pathEndsWith(ELM_LEGACY_JSON) // TODO [drop 0.18] remove the legacy clause


private fun VFileEvent.pathEndsWith(suffix: String) =
        path.endsWith(suffix) || this is VFilePropertyChangeEvent && oldPath.endsWith(suffix)
