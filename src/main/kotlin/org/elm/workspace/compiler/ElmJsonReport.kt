package org.elm.workspace.compiler

import java.util.regex.Pattern

private val urlPattern = Pattern.compile(".*<((http|https)(://.*))>.*", Pattern.CASE_INSENSITIVE or Pattern.DOTALL)
private const val nonBreakingSpace = "&nbsp;"
private const val tempAnchorReplacement = "##TEMP##"


fun elmJsonToCompilerMessages(json: String): List<ElmError> {
    val report = newGson().fromJson(json, Report::class.java) ?: error("failed to parse JSON report from elm")
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
                            location = ElmLocation(path = error.path, moduleName = error.name, region = problem.region)
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
                styleBuilder.append("color: ${mapColor(chunk.color)};")
                "<span style=\"$styleBuilder\">${chunk.string.replace(" ", nonBreakingSpace)}</span>"
            }
        }

private fun asTextSpan(text: String) = "<span style='color: #4F9DA6'>$text</span>"

private fun mapColor(color: String?): String =
        when (color) {
            "yellow" -> "#FACF5A"
            "red" -> "#FF5959"
            null -> "white" // Elm compiler uses null to indicate default foreground color? who knows!
            else -> color
        }
