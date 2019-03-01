package org.elm.workspace.compiler

import com.google.gson.*
import java.util.regex.Pattern

class ElmJsonReport {

    val gson = GsonBuilder().setPrettyPrinting().create()

    private val p = Pattern.compile(".*<((http|https)(://.*))>.*", Pattern.CASE_INSENSITIVE or Pattern.DOTALL)

    private val tempAnchorReplacement = "##TEMP##"

    fun elmToCompilerMessages(json: String): List<CompilerMessage> {
        val obj = gson.fromJson<JsonObject>(json, JsonObject::class.java)
        val errors = obj.get("errors").asJsonArray
        val result: MutableList<CompilerMessage> = mutableListOf()
        errors?.forEach { error ->
            run {
                val path = error.asJsonObject.get("path").asString ?: "-no path-"
                val name = error.asJsonObject.get("name").asString ?: "-no name-"
                val problems = error.asJsonObject.get("problems").asJsonArray
                val list = problemsMessageAndRegion(problems).map { CompilerMessage(name, path, it) }
                result.addAll(list)
            }
        }
        return result
    }

    private fun problemsMessageAndRegion(problems: com.google.gson.JsonArray): List<MessageAndRegion> {
        return problems.map {
            createHtmlMessageAndRegion(it.asJsonObject.get("message").asJsonArray, it.asJsonObject.get("region").asJsonObject, it.asJsonObject.get("title").asString) }
    }

    private fun createHtmlMessageAndRegion(message: JsonArray, region: JsonObject, title: String): MessageAndRegion {
        val reg = Gson().fromJson(region.toString(), Region::class.java)
        val lines = message.map { (::itemToString)(it) }
        val body = lines.joinToString("").orEmpty()
        return MessageAndRegion("<html><body style=\"font-family: monospace\">$body</body></html>", reg, title ?: "-no title-")
    }

    private fun itemToString(jsonElement: JsonElement): String {
        if (jsonElement.isJsonPrimitive) {
            val m = p.matcher(jsonElement.asString)
            if (m.find()) {
                val httpurl = m.group(1)
                val anchor = "<a href=\"$httpurl\">$httpurl</a>"
                return jsonElement.asString.replace("<$httpurl>", tempAnchorReplacement).replace(" ", "&nbsp;").replace("\n", "<br>").replace(tempAnchorReplacement, anchor)
            }
            return jsonElement.asString.replace(" ", "&nbsp;").replace("\n", "<br>")
        } else
            return markupToString(jsonElement.asJsonObject)
    }

    private fun markupToString(jsonObject: JsonObject): String {
        val styleBuilder = StringBuilder()
        with(jsonObject) {
            get("bold").asBoolean.let { if (it) styleBuilder.append("font-weight: bold;") }
            get("underline").asBoolean.let { if (it) styleBuilder.append("text-decoration: underline;") }
            // TODO get("color").let { styleBuilder.append("color: ${mapColor(it.asString)};") }
            get("string").asString.let { return "<span style=\"$styleBuilder\">${it.replace(" ", "&nbsp;")}</span>" }
        }
    }

    private fun mapColor(color: String): String {
        when (color) {
            "yellow" -> return "#666666"
        }
        return color;
    }
}
