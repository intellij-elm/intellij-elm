package org.elm.utils

import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture

/**
 * Run [fn] on a pooled background thread and show IntelliJ's indeterminate progress bar while it's running.
 *
 * @param project The IntelliJ project
 * @param progressTitle The text to be shown with the progress bar
 * @param fn The work to be performed in the background
 * @return A future providing the result of [fn]
 */
fun <T> runAsyncTask(project: Project, progressTitle: String, fn: () -> T): CompletableFuture<T> {
    val fut = CompletableFuture<T>()

    ProgressManager.getInstance().run(
            object : Task.Backgroundable(project, progressTitle) {

                // NOTE: At first I thought that you should override [onThrowable] to make the future complete
                // exceptionally, but when I did that, the exceptions were still escaping to the top-level.
                // So now I just catch the exceptions myself within `run`.

                override fun run(indicator: ProgressIndicator) {
                    try {
                        fut.complete(fn())
                    } catch (e: ProcessCanceledException) {
                        throw e // ProgressManager needs to be able to see these
                    } catch (e: Throwable) {
                        fut.completeExceptionally(e)
                    }

                }

                override fun onCancel() {
                    // TODO [kl] make sure that this is working correctly with IntelliJ cancellation
                    fut.completeExceptionally(CancellationException())
                    super.onCancel()
                }
            })

    return fut
}


/**
 * Join on the completion of all futures in the list.
 */
fun <T> List<CompletableFuture<T>>.joinAll(): CompletableFuture<List<T>> =
        CompletableFuture.allOf(*this.toTypedArray()).thenApply { map { it.join() } }


/**
 * Handle an error, ignoring successful result.
 */
fun <T> CompletableFuture<T>.handleError(fn: (error: Throwable) -> Unit): CompletableFuture<Unit> =
        handle { _, error -> fn(error) }