package org.elm.lang.core.psi

import org.elm.fileTree
import org.elm.workspace.ElmWorkspaceTestBase
import org.elm.workspace.elmWorkspace

internal class ElmGlobalModificationTrackerWorkspaceTest : ElmWorkspaceTestBase() {
    fun `test mod count incremented on workspace refresh`() {
        fileTree {
            dir("a") {
                project("elm.json", """
                    {
                      "type": "application",
                      "source-directories": [ "src" ],
                      "elm-version": "0.19.1",
                      "dependencies": {
                        "direct": {},
                        "indirect": {}
                      },
                      "test-dependencies": {
                        "direct": {},
                        "indirect": {}
                      }
                    }
                    """)
                dir("src") {
                    elm("Main.elm", "")
                }
            }
        }.create(project, elmWorkspaceDirectory)

        val modTracker = project.modificationTracker
        val oldCount = modTracker.modificationCount
        project.elmWorkspace.asyncRefreshAllProjects().get()
        check(modTracker.modificationCount > oldCount)
    }
}

