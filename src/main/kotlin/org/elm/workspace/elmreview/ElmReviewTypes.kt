package org.elm.workspace.elmreview

import com.google.gson.*
import com.google.gson.annotations.JsonAdapter
import java.lang.reflect.Type

// ELM-REVIEW TYPES
// THe report format is described here: https://github.com/jfmengels/node-elm-review/blob/master/documentation/tooling-integration.md

private val gson = Gson()

@JsonAdapter(ReportDeserializer::class)
sealed class Report {
    data class General(
            val path: String?,
            val title: String,
            val message: List<Chunk>
    ) : Report()

    data class Specific(
            val path: String,
            val errors: List<BadModuleError>
    ) : Report()
}

data class BadModuleError(
        val path: String,
        val errors: List<ElmReviewIntermediateError>
)


class ReportDeserializer : JsonDeserializer<Report> {
    override fun deserialize(element: JsonElement, typeOf: Type, context: JsonDeserializationContext): Report {
        if (!element.isJsonObject) throw JsonParseException("Expected a report object")
        val obj = element.asJsonObject
        val reportType = obj["type"].asString
        return when (reportType) {
            "error" -> gson.fromJson(obj, Report.General::class.java)
            "review-errors" -> gson.fromJson(obj, Report.Specific::class.java)
            else -> error("Unexpected Elm compiler report type '$reportType'")
        }
    }
}

data class ElmReviewError(
        // TODO Add a optional fix field (For later)
        val path: String?,
        val rule: String,
        val message: String,
        val region: Region,
        val html: String
)

data class ElmReviewIntermediateError(
        val rule: String,
        val message: String,
        val region: Region,
        val formatted: List<Chunk>
)

data class Region(val start: Start, val end: End)
data class Start(val line: Int, val column: Int)
data class End(val line: Int, val column: Int)


@JsonAdapter(ChunkDeserializer::class)
sealed class Chunk {
    data class Unstyled(val str: String) : Chunk()
    data class Styled(val bold: Boolean?, val underline: Boolean?, val color: String?, val string: String, val href: String?) : Chunk()
}

class ChunkDeserializer : JsonDeserializer<Chunk> {
    override fun deserialize(element: JsonElement, typeOf: Type, context: JsonDeserializationContext): Chunk =
            when {
                element.isJsonObject -> gson.fromJson(element, Chunk.Styled::class.java)
                element.isJsonPrimitive -> Chunk.Unstyled(element.asString)
                else -> throw JsonParseException("Expected a simple string or a rich-text chunk")
            }
}