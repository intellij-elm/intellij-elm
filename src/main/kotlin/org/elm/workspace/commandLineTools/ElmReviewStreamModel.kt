package org.elm.workspace.commandLineTools

import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken

data class ElmReviewWatchError(
    // TODO Add a optional fix field (For later)
    var suppressed: Boolean? = null,
    var path: String? = null,
    var rule: String? = null,
    var message: String? = null,
    var regionWatch: RegionWatch? = null,
    var html: String? = null
)

data class RegionWatch(
    var start: LocationWatch? = null,
    var end: LocationWatch? = null
)

data class LocationWatch(
    var line: Int = 0,
    var column: Int = 0
)

sealed class ChunkWatch {
    data class Unstyled(var str: String) : ChunkWatch()
    data class Styled(var string: String? = null, var bold: Boolean? = null, var underline: Boolean? = null, var color: String? = null, var href: String? = null) : ChunkWatch()
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

fun parseReviewJsonStream(reader: JsonReader, process: Process, emit: (List<ElmReviewWatchError>) -> Unit): Int {
    with(reader) {
        while (process.isAlive) {
            if (process.errorStream.available() > 0)
                return 1
            if (hasNext()) {
                val errors = readErrorReport()
                emit(errors)
            }
        }
        close()
        return process.exitValue()
    }
}

fun JsonReader.readProperties(propertyHandler: (String) -> Unit) {
    beginObject()
    while (hasNext()) {
        propertyHandler(nextName())
    }
    endObject()
}

fun JsonReader.readErrorReport(): List<ElmReviewWatchError> {
    val errors = mutableListOf<ElmReviewWatchError>()
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
                                            val elmReviewWatchError = ElmReviewWatchError(path = currentPath)
                                            readProperties { property ->
                                                when (property) {
                                                    "suppressed" -> elmReviewWatchError.suppressed = nextBoolean()
                                                    "rule" -> elmReviewWatchError.rule = nextString()
                                                    "message" -> elmReviewWatchError.message = nextString()
                                                    "region" -> {
                                                        elmReviewWatchError.regionWatch = readRegion()
                                                    }
                                                    "formatted" -> {
                                                        val chunkWatchList = readChunkWatchList()
                                                        elmReviewWatchError.html = chunksToHtml(chunkWatchList)
                                                    }
                                                    else -> {
                                                        // TODO "fix", "details", "ruleLink", "originallySuppressed"
                                                        skipValue()
                                                    }
                                                }
                                            }
                                            errors.add(elmReviewWatchError)
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
                            readProperties { property ->
                                when (property) {
                                    "path" -> currentPath = nextString()
                                    "name" -> skipValue()
                                    "problems" -> {
                                        beginArray()
                                        while (hasNext()) {
                                            val elmReviewWatchError = ElmReviewWatchError(path = currentPath)
                                            readProperties { property ->
                                                when (property) {
                                                    "title" -> elmReviewWatchError.rule = nextString()
                                                    else -> {
                                                        // TODO "fix", "details", "ruleLink", "originallySuppressed"
                                                        skipValue()
                                                    }
                                                }
                                            }
                                            errors.add(elmReviewWatchError)
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
                val elmReviewWatchError = ElmReviewWatchError(rule = nextString())
                while (hasNext()) {
                    when (nextName()) {
                        "path" -> elmReviewWatchError.path = nextString()
                        "message" -> {
                            val chunkWatchList = readChunkWatchList()
                            elmReviewWatchError.message = chunksToLines(chunkWatchList).joinToString("\n")
                        }
                    }
                }
                errors.add(elmReviewWatchError)
            }
        }
    }
    return errors
}

private fun JsonReader.readChunkWatchList(): List<ChunkWatch> {
    val chunkWatchList = mutableListOf<ChunkWatch>()
    beginArray()
    while (hasNext()) {
        if (this.peek() == JsonToken.BEGIN_OBJECT) {
            val chunkWatchStyled = ChunkWatch.Styled()
            beginObject()
            while (hasNext()) {
                when (nextName()) {
                    "string" -> chunkWatchStyled.string = nextString()
                    "color" -> chunkWatchStyled.color = nextString()
                    "href" -> chunkWatchStyled.href = nextString()
                }
            }
            endObject()
            chunkWatchList.add(chunkWatchStyled)
        } else {
            chunkWatchList.add(ChunkWatch.Unstyled(nextString()))
        }
    }
    endArray()
    return chunkWatchList
}

fun JsonReader.readRegion(): RegionWatch {
    val regionWatch = RegionWatch()
    beginObject()
    while (hasNext()) {
        when (nextName()) {
            "start" -> {
                val location = readLocation()
                regionWatch.start = location
            }
            "end" -> {
                val location = readLocation()
                regionWatch.end = location
            }
        }
    }
    endObject()
    return regionWatch
}

fun JsonReader.readLocation(): LocationWatch {
    val locationWatch = LocationWatch()
    beginObject()
    while (hasNext()) {
        when (val prop = nextName()) {
            "line" -> locationWatch.line = nextInt()
            "column" -> locationWatch.column = nextInt()
            else -> TODO("unexpected property $prop")
        }
    }
    endObject()
    return locationWatch
}

fun chunksToHtml(chunkWatches: List<ChunkWatch>): String =
    chunkWatches.joinToString(
        "",
        prefix = "<html><body style=\"font-family: monospace; font-weight: bold\">",
        postfix = "</body></html>"
    ) { chunkToHtml(it) }

fun chunkToHtml(chunkWatch: ChunkWatch): String =
    when (chunkWatch) {
        is ChunkWatch.Unstyled -> toHtmlSpan("color: #4F9DA6", chunkWatch.str)
        is ChunkWatch.Styled -> with(StringBuilder()) {
            append("color: ${chunkWatch.color.adjustForDisplay()};")
            val str = if (chunkWatch.href == null) {
                chunkWatch.string
            } else {
                createHyperlinks(chunkWatch.href!!, chunkWatch.string!!)
            }
            toHtmlSpan(this, str!!)
        }
    }


fun toHtmlSpan(style: CharSequence, text: String) =
    """<span style="$style">${text.convertWhitespaceToHtml().createHyperlinks()}</span>"""

// The Elm compiler emits HTTP URLs with angle brackets around them
val urlPattern = Regex("""<((http|https)://.*?)>""")

fun String.createHyperlinks(): String =
    urlPattern.replace(this) { result ->
        val url = result.groupValues[1]
        "<a href=\"$url\">$url</a>"
    }

fun createHyperlinks(href: String, text: String): String =
    "<a href=\"$href\">$text</a>"

fun String.convertWhitespaceToHtml() =
    replace(" ", "&nbsp;").replace("\n", "<br>")

/**
 * Adjust the colors to make it look good
 */
fun String?.adjustForDisplay(): String =
    when (this) {
        "#FF0000" -> "#FF5959"
        null -> "white"
        else -> this
    }

private fun chunksToLines(chunks: List<ChunkWatch>): List<String> {
    return chunks.asSequence().map {
        when (it) {
            is ChunkWatch.Unstyled -> it.str
            is ChunkWatch.Styled -> it.string
        }
    }.joinToString("").lines()
}
