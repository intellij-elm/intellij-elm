package org.elm.workspace.compiler

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Klaxon
import com.beust.klaxon.Parser
import java.util.regex.Pattern

class ElmJsonReport {

    private val parser: Parser = Parser.default()

    private val p = Pattern.compile(".*<((http|https)(://.*))>.*", Pattern.CASE_INSENSITIVE or Pattern.DOTALL)

    private val tempAnchorReplacement = "##TEMP##"

    fun elmToCompilerMessages(json: String): List<CompilerMessage> {
        val jsonObject: JsonObject = parser.parse(StringBuilder(json)) as JsonObject
        val errors: JsonArray<JsonObject>? = jsonObject.array("errors")

        val result: MutableList<CompilerMessage> = mutableListOf()
        errors?.forEach { error ->
            run {
                val path = error.string("path") ?: "-no path-"
                val name = error.string("name") ?: "-no name-"
                val list = problemsMessageAndRegion(error).map { CompilerMessage(name, path, it) }
                result.addAll(list)
            }
        }
        return result
    }

    private fun problemsMessageAndRegion(jsonObject: JsonObject): List<MessageAndRegion> {
        val problems: JsonArray<JsonObject>? = jsonObject.array("problems")

        return problems?.map { createHtmlMessageAndRegion(it.array("message"), it.obj("region"), it.string("title")) }.orEmpty()
    }

    private fun createHtmlMessageAndRegion(message: JsonArray<Any>?, region: JsonObject?, title: String?): MessageAndRegion {
        val lines = message?.map { (::itemToString)(it) }
        val body = lines?.joinToString("").orEmpty()
        val reg = Klaxon().parse<Region>(region?.toJsonString().orEmpty()) ?: Region(End(0, 0), Start(0, 0))
        return MessageAndRegion("<html><body style=\"font-family: monospace\">$body</body></html>", reg, title
                ?: "-no title-")
    }

    private fun itemToString(any: Any): String {
        if (any is String) {
            val m = p.matcher(any)
            if (m.find()) {
                val httpurl = m.group(1)
                val anchor = "<a href=\"$httpurl\">$httpurl</a>"
                return any.replace("<$httpurl>", tempAnchorReplacement).replace(" ", "&nbsp;").replace("\n", "<br>").replace(tempAnchorReplacement, anchor)
            }
            return any.replace(" ", "&nbsp;").replace("\n", "<br>")
        } else
            return markupToString(any as JsonObject)
    }

    private fun markupToString(jsonObject: JsonObject): String {
        val styleBuilder = StringBuilder()
        with(jsonObject) {
            boolean("bold")?.let { if (it) styleBuilder.append("font-weight: bold;") }
            boolean("underline")?.let { if (it) styleBuilder.append("text-decoration: underline;") }
            string("color")?.let { styleBuilder.append("color: ${mapColor(it)};") }
            string("string")?.let { return "<span style=\"$styleBuilder\">${it.replace(" ", "&nbsp;")}</span>" }
        }
        return "" // TODO
    }

    private fun mapColor(color: String): String {
        when (color) {
            "yellow" -> return "#666666"
        }
        return color;
    }

}
