package org.elm.workspace.elmreview

import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken

data class ElmReviewError(
    // TODO Add a optional fix field (For later)
    var suppressed: Boolean? = null,
    var path: String? = null,
    var rule: String? = null,
    var message: String? = null,
    var region: Region? = null,
    var html: String? = null
)

data class Region(
    var start: Location? = null,
    var end: Location? = null
)

data class Location(
    var line: Int = 0,
    var column: Int = 0
)

sealed class Chunk {
    data class Unstyled(var str: String) : Chunk()
    data class Styled(var string: String? = null, var bold: Boolean? = null, var underline: Boolean? = null, var color: String? = null, var href: String? = null) : Chunk()
}

enum class ReviewOutputType(val label: String) {
    ERROR("error"), COMPILE_ERRORS("compile-errors"), REVIEW_ERRORS("review-errors");

    companion object {
        fun fromLabel(label: String) =
            when (label) {
                ERROR.label -> ERROR
                COMPILE_ERRORS.label -> COMPILE_ERRORS
                REVIEW_ERRORS.label -> REVIEW_ERRORS
                else -> TODO("unknown type $label")
            }
    }
}

fun parseReviewJsonStream(reader: JsonReader, process: Process, emit: (List<ElmReviewError>) -> Unit): Int {
    reader.use {
        while (process.isAlive) {
            if (it.hasNext()) {
                val errors = it.readErrorReport()
                emit(errors)
            }
        }
    }
    return process.exitValue()
}

fun JsonReader.readProperties(propertyHandler: (String) -> Unit) {
    beginObject()
    while (hasNext()) {
        propertyHandler(nextName())
    }
    endObject()
}

fun JsonReader.readErrorReport(): List<ElmReviewError> {
    val errors = mutableListOf<ElmReviewError>()
    if (this.peek() == JsonToken.END_DOCUMENT) return emptyList()

    var type: ReviewOutputType? = null
    readProperties { property ->
        when (property) {
            "type" -> {
                type = ReviewOutputType.fromLabel(nextString())
            }
            "errors" -> {
                when (type) {
                    ReviewOutputType.REVIEW_ERRORS -> {
                        beginArray()
                        while (hasNext()) {
                            var currentPath: String? = null
                            readProperties { property ->
                                when (property) {
                                    "path" -> currentPath = nextString()
                                    "errors" -> {
                                        beginArray()
                                        while (hasNext()) {
                                            val elmReviewError = ElmReviewError(path = currentPath)
                                            readProperties { property ->
                                                when (property) {
                                                    "suppressed" -> elmReviewError.suppressed = nextBoolean()
                                                    "rule" -> elmReviewError.rule = nextString()
                                                    "message" -> elmReviewError.message = nextString()
                                                    "region" -> {
                                                        elmReviewError.region = readRegion()
                                                    }
                                                    "formatted" -> {
                                                        val chunkList = readChunkList()
                                                        elmReviewError.html = chunksToHtml(chunkList)
                                                    }
                                                    else -> {
                                                        // TODO "fix", "details", "ruleLink", "originallySuppressed"
                                                        skipValue()
                                                    }
                                                }
                                            }
                                            errors.add(elmReviewError)
                                        }
                                        endArray()
                                    }
                                }
                            }
                        }
                        endArray()
                    }
                    ReviewOutputType.COMPILE_ERRORS -> {
                        beginArray()
                        while (hasNext()) {
                            var currentPath: String? = null
                            // TODO type 'compile-errors' with property errors ARRAY !?
                            readProperties { property ->
                                when (property) {
                                    "path" -> currentPath = nextString()
                                    "name" -> skipValue()
                                    "problems" -> {
                                        beginArray()
                                        while (hasNext()) {
                                            val elmReviewError = ElmReviewError(path = currentPath)
                                            readProperties { property ->
                                                when (property) {
                                                    "title" -> elmReviewError.rule = nextString()
                                                    else -> {
                                                        // TODO "fix", "details", "ruleLink", "originallySuppressed"
                                                        skipValue()
                                                    }
                                                }
                                            }
                                            errors.add(elmReviewError)
                                        }
                                        endArray()
                                    }
                                }
                            }
                        }
                        endArray()
                    }
                    ReviewOutputType.ERROR -> throw RuntimeException("Unexpected json-type 'error' with 'errors' array")
                    null -> println("ERROR: no report 'type'")
                }
            }
            else -> {
                // TODO make resilient against property order, title is expected first !
                val elmReviewError = ElmReviewError(rule = nextString())
                while (hasNext()) {
                    when (nextName()) {
                        "path" -> elmReviewError.path = nextString()
                        "message" -> {
                            val chunkList = readChunkList()
                            elmReviewError.message = chunksToLines(chunkList).joinToString("\n")
                        }
                    }
                }
                errors.add(elmReviewError)
            }
        }
    }
    return errors
}

private fun JsonReader.readChunkList(): List<Chunk> {
    val chunkList = mutableListOf<Chunk>()
    beginArray()
    while (hasNext()) {
        if (this.peek() == JsonToken.BEGIN_OBJECT) {
            val chunkStyled = Chunk.Styled()
            beginObject()
            while (hasNext()) {
                when (nextName()) {
                    "string" -> chunkStyled.string = nextString()
                    "color" -> chunkStyled.color = nextString()
                    "href" -> chunkStyled.href = nextString()
                }
            }
            endObject()
            chunkList.add(chunkStyled)
        } else {
            chunkList.add(Chunk.Unstyled(nextString()))
        }
    }
    endArray()
    return chunkList
}

fun JsonReader.readRegion(): Region {
    val region = Region()
    beginObject()
    while (hasNext()) {
        when (nextName()) {
            "start" -> {
                val location = readLocation()
                region.start = location
            }
            "end" -> {
                val location = readLocation()
                region.end = location
            }
        }
    }
    endObject()
    return region
}

fun JsonReader.readLocation(): Location {
    val location = Location()
    beginObject()
    while (hasNext()) {
        when (val prop = nextName()) {
            "line" -> location.line = nextInt()
            "column" -> location.column = nextInt()
            else -> TODO("unexpected property $prop")
        }
    }
    endObject()
    return location
}

fun chunksToHtml(chunks: List<Chunk>): String =
    chunks.joinToString(
        "",
        prefix = "<html><body style=\"font-family: monospace; font-weight: bold\">",
        postfix = "</body></html>"
    ) { chunkToHtml(it) }

fun chunkToHtml(chunk: Chunk): String =
    when (chunk) {
        is Chunk.Unstyled -> toHtmlSpan("color: #4F9DA6", chunk.str)
        is Chunk.Styled -> with(StringBuilder()) {
            append("color: ${chunk.color.adjustForDisplay()};")
            val str = if (chunk.href == null) {
                chunk.string
            } else {
                createHyperlinks(chunk.href!!, chunk.string!!)
            }
            toHtmlSpan(this, str!!)
        }
    }


private fun toHtmlSpan(style: CharSequence, text: String) =
    """<span style="$style">${text.convertWhitespaceToHtml().createHyperlinks()}</span>"""

// The Elm compiler emits HTTP URLs with angle brackets around them
private val urlPattern = Regex("""<((http|https)://.*?)>""")

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

private fun chunksToLines(chunks: List<Chunk>): List<String> {
    return chunks.asSequence().map {
        when (it) {
            is Chunk.Unstyled -> it.str
            is Chunk.Styled -> it.string
        }
    }.joinToString("").lines()
}
