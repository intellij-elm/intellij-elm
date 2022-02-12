package org.elm.workspace.elmreview


import com.google.gson.Gson

// The Elm compiler emits HTTP URLs with angle brackets around them
private val urlPattern = Regex("""<((http|https)://.*?)>""")


fun elmReviewJsonToMessages(json: String): List<ElmReviewError> {
    return when (val report = Gson().fromJson(json, Report::class.java)
            ?: error("failed to parse JSON report from elm-review")) {
        is Report.General -> {
            listOf(ElmReviewError(
                    path = report.path,
                    rule = report.title,
                    message = "",
                    region = Region(Start(1, 1), End(2, 1)),
                    html = chunksToHtml(report.message)
            ))
        }
        is Report.Specific -> {
            report.errors.flatMap { errorsForFile ->
                errorsForFile.errors.map { error ->
                    ElmReviewError(
                            path = errorsForFile.path,
                            rule = error.rule,
                            message = error.message,
                            region = error.region,
                            html = chunksToHtml(error.formatted)
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
            is Chunk.Unstyled -> toHtmlSpan("color: #4F9DA6", chunk.str)
            is Chunk.Styled -> with(StringBuilder()) {
                append("color: ${chunk.color.adjustForDisplay()};")
                val str = if (chunk.href == null) {
                    chunk.string
                } else {
                    createHyperlinks(chunk.href, chunk.string)
                }
                toHtmlSpan(this, str)
            }
        }


private fun toHtmlSpan(style: CharSequence, text: String) =
        """<span style="$style">${text.convertWhitespaceToHtml().createHyperlinks()}</span>"""

private fun String.createHyperlinks(): String =
        urlPattern.replace(this) { result ->
            val url = result.groupValues[1]
            "<a href=\"$url\">$url</a>"
        }

private fun createHyperlinks(href: String, text: String): String =
        "<a href=\"$href\">$text</a>"

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
 * Adjust the colors to make it look good
 */
private fun String?.adjustForDisplay(): String =
        when (this) {
            "#FF0000" -> "#FF5959"
            null -> "white"
            else -> this
        }
