/*
The MIT License (MIT)

Derived from intellij-rust
Copyright (c) 2015 Aleksey Kladov, Evgeny Kurbatsky, Alexey Kudinkin and contributors
Copyright (c) 2016 JetBrains

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */

package org.elm.openapiext

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.execution.process.ProcessOutput
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.util.io.systemIndependentPath
import org.elm.utils.runAsyncTask
import java.io.OutputStreamWriter
import java.nio.file.Path

private val log = Logger.getInstance("org.elm.openapiext.Subprocesses")

@Suppress("FunctionName")
fun GeneralCommandLine(path: Path, vararg args: String) =
    GeneralCommandLine(path.systemIndependentPath, *args)

fun GeneralCommandLine.withWorkDirectory(path: Path?) =
    withWorkDirectory(path?.systemIndependentPath)


@Throws(ExecutionException::class)
fun GeneralCommandLine.execute(
    toolName: String,
    project: Project,
    timeoutInMilliseconds: Int = 3000,
    stdIn: String? = null
): ProcessOutput {

    val handler =
        if (stdIn != null) {
            val process = createProcess()
            val stdInStream = process.outputStream
            val writer = OutputStreamWriter(stdInStream, Charsets.UTF_8)
            try {
                writer.write(stdIn)
            } finally {
                writer.flush()
                writer.close()
            }
            CapturingProcessHandler(process, Charsets.UTF_8, commandLineString)
        } else {
            CapturingProcessHandler(this)
        }

    val processKiller = Disposable { handler.destroyProcess() }
    val alreadyDisposed = runReadAction { project.isDisposed }
    if (alreadyDisposed) {
        return ProcessOutput().apply { setCancelled() }
    }

    Disposer.register(project, processKiller)

    try {
        // see javadoc at OSProcessHandler.checkEdtAndReadAction()
        val future = runAsyncTask(project, toolName) {
            val output = handler.runProcess(timeoutInMilliseconds)
            if (output.exitCode != 0) {
                log.warn("Command $toolName exited with code ${output.exitCode}")
            }
            output
        }
        return future.join()
    } finally {
        Disposer.dispose(processKiller)
    }
}

val ProcessOutput.isSuccess: Boolean
    get() = !isTimeout && !isCancelled && exitCode == 0

val ProcessOutput.isNotSuccess: Boolean
    get() = !isSuccess
