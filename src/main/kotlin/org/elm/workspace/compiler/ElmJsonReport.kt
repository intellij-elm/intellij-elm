package org.elm.workspace.compiler

import java.util.regex.Pattern

private val urlPattern = Pattern.compile(".*<((http|https)(://.*))>.*", Pattern.CASE_INSENSITIVE or Pattern.DOTALL)
private const val nonBreakingSpace = "&nbsp;"
private const val tempAnchorReplacement = "##TEMP##"
private val dummyRegion = Region(Start(0, 0), End(0, 0))


fun elmJsonToCompilerMessages(json: String): List<CompilerMessage> {
    val report = newGson().fromJson(json, Report::class.java) ?: error("failed to parse JSON report from elm")
    return when (report) {
        is Report.General -> {
            listOf(CompilerMessage(report.title, report.path ?: "",
                    MessageAndRegion(chunksToHtml(report.message), dummyRegion, report.title)))
        }
        is Report.Specific -> {
            report.errors.flatMap { error ->
                error.problems.map { problem ->
                    CompilerMessage(error.name, error.path,
                            MessageAndRegion(chunksToHtml(problem.message), problem.region, problem.title))
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
            is Chunk.Unstyled -> {
                val urlMatcher = urlPattern.matcher(chunk.string)
                if (urlMatcher.find()) {
                    val httpUrl = urlMatcher.group(1)
                    val anchor = "<a href=\"$httpUrl\">$httpUrl</a>"
                    asTextSpan(chunk.string
                            .replace("<$httpUrl>", tempAnchorReplacement)
                            .replace(" ", nonBreakingSpace)
                            .replace("\n", "<br>")
                            .replace(tempAnchorReplacement, anchor))
                } else {
                    asTextSpan(chunk.string.replace(" ", nonBreakingSpace).replace("\n", "<br>"))
                }
            }
            is Chunk.Styled -> {
                val styleBuilder = StringBuilder()
                if (chunk.bold) styleBuilder.append("font-weight: bold;")
                if (chunk.underline) styleBuilder.append("text-decoration: underline;")
                val toolwindowColor = chunk.color?.let { mapColor(it) } ?: "#FFFFFF"
                styleBuilder.append("color: $toolwindowColor;")
                "<span style=\"$styleBuilder\">${chunk.string.replace(" ", nonBreakingSpace)}</span>"
            }
        }

private fun asTextSpan(text: String) = "<span style='color: #4F9DA6'>$text</span>"

private fun mapColor(color: String): String =
        when (color) {
            "yellow" -> "#FACF5A"
            "red" -> "#FF5959"
            else -> color
        }
