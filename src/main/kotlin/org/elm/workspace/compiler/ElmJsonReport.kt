package org.elm.workspace.compiler

import com.google.gson.Gson

// The Elm compiler emits HTTP URLs with angle brackets around them
private val urlPattern = Regex("""<((http|https)://.*?)>""")


fun elmJsonToCompilerMessages(json: String): List<ElmError> {
    val report = Gson().fromJson(json, Report::class.java) ?: error("failed to parse JSON report from elm")
    return when (report) {
        is Report.General -> {
            listOf(ElmError(
                    title = report.title,
                    html = chunksToHtml(report.message),
                    location = report.path?.let { ElmLocation(path = it, moduleName = null, region = null) })
            )
        }
        is Report.Specific -> {
            report.errors.flatMap { error ->
                error.problems.map { problem ->
                    ElmError(
                            title = problem.title,
                            html = chunksToHtml(problem.message),
                            location = ElmLocation(
                                    path = error.path,
                                    moduleName = error.name,
                                    region = problem.region)
                    )
                }
            }
        }
    }
}

private fun chunksToHtml(chunks: List<Chunk>): String =
        chunks.joinToString("",
                prefix = "<html><body style=\"font-family: monospace; font-weight: bold\">",
                postfix = "</body></html>"
        ) { chunkToHtml(it) }

private fun chunkToHtml(chunk: Chunk): String =
        when (chunk) {
            is Chunk.Unstyled -> toHtmlSpan("color: #4F9DA6", chunk.string)
            is Chunk.Styled -> with(StringBuilder()) {
                if (chunk.bold) append("font-weight: bold;")
                if (chunk.underline) append("text-decoration: underline;")
                append("color: ${chunk.color.adjustForDisplay()};")
                toHtmlSpan(this, chunk.string)
            }
        }

private fun toHtmlSpan(style: CharSequence, text: String) =
        """<span style="$style">${text.convertWhitespaceToHtml().createHyperlinks()}</span>"""

private fun String.createHyperlinks(): String =
        urlPattern.replace(this) { result ->
            val url = result.groupValues[1]
            "<a href=\"$url\">$url</a>"
        }

/**
 * The Elm compiler emits the text where the whitespace is already formatted to line up
 * using a fixed-width font. But HTML does its own thing with whitespace. We could use a
 * `<pre>` element, but then we wouldn't be able to do the color highlights and other
 * formatting tricks.
 *
 * The best solution would be to use the "white-space: pre" CSS rule, but the UI widget
 * that we use to render the HTML, [javax.swing.JTextPane], does not support it
 * (as best I can tell).
 *
 * So we will instead manually convert the whitespace so that it renders correctly.
 */
private fun String.convertWhitespaceToHtml() =
        replace(" ", "&nbsp;").replace("\n", "<br>")

/**
 * Adjust the Elm compiler's colors to make it look good
 */
private fun String?.adjustForDisplay(): String =
        when (this) {
            "yellow" -> "#FACF5A"
            "red" -> "#FF5959"
            null -> "white" // Elm compiler uses null to indicate default foreground color? who knows!
            else -> this
        }
