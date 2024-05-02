package org.elm.workspace

import com.google.common.annotations.VisibleForTesting
import com.intellij.openapi.components.*
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import org.jdom.Element
import java.util.concurrent.CompletableFuture


private val log = logger<ElmReviewService>()

@State(name = "ElmReview", storages = [Storage(StoragePathMacros.WORKSPACE_FILE)])
@Service(Service.Level.PROJECT)
class ElmReviewService(val intellijProject: Project) : PersistentStateComponent<Element> {

    var activeWatchmodeProcess: Process? = null

    // PERSISTENT STATE

    override fun getState(): Element {
        val state = Element("state")

/*
        val configElement = Element("elmReviewConfig")
        state.addContent(configElement)
        ..etc
*/

        return state
    }

    override fun loadState(state: Element) {
        asyncLoadState(state)
    }

    @VisibleForTesting
    fun asyncLoadState(@Suppress("UNUSED_PARAMETER") state: Element): CompletableFuture<Unit> {

/*
        val settingsElement = state.getChild("settings")
        ...etc
*/
        return CompletableFuture()
    }

    override fun noStateLoaded() {
    }
}
