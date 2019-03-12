package org.elm.workspace.compiler

import com.google.gson.*
import java.lang.reflect.Type

// types for Elm's json report
// The naming is based on https://package.elm-lang.org/packages/elm/project-metadata-utils/latest/Elm-Error

fun newGson(): Gson =
        GsonBuilder()
                .registerTypeAdapter(Report::class.java, ReportDeserializer())
                .registerTypeAdapter(Chunk::class.java, ChunkDeserializer())
                .create()

sealed class Report {
    data class General(
            val path: String?,
            val title: String,
            val message: List<Chunk>
    ) : Report()

    data class Specific(
            val errors: List<BadModuleError>
    ) : Report()
}

class ReportDeserializer : JsonDeserializer<Report> {
    override fun deserialize(element: JsonElement, typeOf: Type, context: JsonDeserializationContext): Report {
        val gson = newGson()
        if (!element.isJsonObject) throw JsonParseException("Expected a report object")
        val obj = element.asJsonObject
        val reportType = obj["type"].asString
        return when (reportType) {
            "error" -> gson.fromJson(obj, Report.General::class.java)
            "compile-errors" -> gson.fromJson(obj, Report.Specific::class.java)
            else -> error("Unexpected Elm compiler report type '$reportType'")
        }
    }
}

data class BadModuleError(
        val path: String,
        val name: String,
        val problems: List<Problem>
)

data class Problem(
        val title: String,
        val region: Region,
        val message: List<Chunk>
)

data class Region(val start: Start, val end: End)
data class Start(val column: Int, val line: Int)
data class End(val column: Int, val line: Int)

sealed class Chunk {
    data class Unstyled(val string: String) : Chunk()
    data class Styled(val bold: Boolean, val underline: Boolean, val color: String, val string: String) : Chunk()
}

class ChunkDeserializer : JsonDeserializer<Chunk> {
    override fun deserialize(element: JsonElement, typeOf: Type, context: JsonDeserializationContext): Chunk {
        val gson = newGson()
        return when {
            element.isJsonObject -> gson.fromJson(element, Chunk.Styled::class.java)
            element.isJsonPrimitive -> Chunk.Unstyled(element.asString)
            else -> throw JsonParseException("Expected a simple string or a rich-text chunk")
        }
    }
}


// plugin's types

data class CompilerMessage(val name: String, val path: String, val messageWithRegion: MessageAndRegion)

data class MessageAndRegion(val message: String, val region: Region, val title: String)
