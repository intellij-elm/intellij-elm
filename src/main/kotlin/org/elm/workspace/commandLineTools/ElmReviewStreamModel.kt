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

fun parseReviewJsonStream(reader: JsonReader, emit: (List<ElmReviewWatchError>) -> Unit) {
    with(reader) {
        while (hasNext()) {
            val errors = mutableListOf<ElmReviewWatchError>()
            beginObject()
            var type: ReviewOutputType? = null
            while (hasNext()) {
                when (val prop = nextName()) {
                    "type" -> {
                        type = ReviewOutputType.fromLabel(nextString())
                    }
                    "errors" -> {
                        when (type) {
                            ReviewOutputType.REVIEW_ERRORS -> {
                                beginArray()
                                while (hasNext()) {
                                    var currentPath: String? = null
                                    beginObject()
                                    while (hasNext()) {
                                        when (nextName()) {
                                            "path" -> currentPath = nextString()
                                            "errors" -> {
                                                beginArray()
                                                while (hasNext()) {
                                                    val elmReviewWatchError = ElmReviewWatchError(path = currentPath)
                                                    beginObject()
                                                    while (hasNext()) {
                                                        when (nextName()) {
                                                            "suppressed" -> elmReviewWatchError.suppressed = nextBoolean()
                                                            "rule" -> elmReviewWatchError.rule = nextString()
                                                            "message" -> elmReviewWatchError.message = nextString()
                                                            "region" -> {
                                                                elmReviewWatchError.regionWatch = readRegion()
                                                            }
                                                            "formatted" -> {
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
                                                                elmReviewWatchError.html = chunksToHtml(chunkWatchList)
                                                            }
                                                            else -> {
                                                                // TODO "fix", "details", "ruleLink", "originallySuppressed"
                                                                reader.skipValue()
                                                            }
                                                        }
                                                    }
                                                    endObject()
                                                    errors.add(elmReviewWatchError)
                                                }
                                                endArray()
                                            }
                                        }
                                    }
                                    endObject()
                                }
                                endArray()
                            }
                            ReviewOutputType.ERROR -> {
                                println("TODO type 'error'")
                            }
                            ReviewOutputType.COMPILE_ERRORS -> {
                                println("TODO type 'compile-errors'")
                            }
                            null -> println("no type")
                        }
                    }
                    else -> println("unexpected property $prop")
                }
            }
            endObject()
            emit(errors)
        }
    }
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

