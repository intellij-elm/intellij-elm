package org.elm.workspace.compiler

import com.google.gson.*
import java.util.regex.Pattern

private val urlPattern = Pattern.compile(".*<((http|https)(://.*))>.*", Pattern.CASE_INSENSITIVE or Pattern.DOTALL)
private const val nonBreakingSpace = "&nbsp;"
private val tempAnchorReplacement = "##TEMP##"


fun elmJsonToCompilerMessages(json: String): List<CompilerMessage> {
    val gson = GsonBuilder().setPrettyPrinting().create()
    val obj = gson.fromJson<JsonObject>(json, JsonObject::class.java)
    val type = obj.get("type").asString
    if (type == "error") {
        val path = obj.get("path").asString ?: "-no path-"
        val title = obj.get("title").asString
        return listOf(CompilerMessage(title, path, createHtmlMessage(obj.get("message").asJsonArray, title)))
    } else {
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
}

private fun createHtmlMessage(message: JsonArray, title: String): MessageAndRegion {
    val lines = message.map { (::itemToString)(it) }
    val body = lines.joinToString("")
    return MessageAndRegion("<html><body style=\"font-family: monospace\">$body</body></html>", Region(End(0, 0), Start(0, 0)), title)
}

private fun problemsMessageAndRegion(problems: com.google.gson.JsonArray): List<MessageAndRegion> {
    return problems.map {
        createHtmlMessageAndRegion(it.asJsonObject.get("message").asJsonArray, it.asJsonObject.get("region").asJsonObject, it.asJsonObject.get("title").asString)
    }
}

private fun createHtmlMessageAndRegion(message: JsonArray, region: JsonObject, title: String): MessageAndRegion {
    val reg = Gson().fromJson(region.toString(), Region::class.java)
    val lines = message.map { (::itemToString)(it) }
    val body = lines.joinToString("")
    return MessageAndRegion("<html><body style=\"font-family: monospace; font-weight: bold\">$body</body></html>", reg, title)
}

private fun itemToString(jsonElement: JsonElement): String {
    if (jsonElement.isJsonPrimitive) {
        val urlMatcher = urlPattern.matcher(jsonElement.asString)
        if (urlMatcher.find()) {
            val httpurl = urlMatcher.group(1)
            val anchor = "<a href=\"$httpurl\">$httpurl</a>"
            return asTextSpan(jsonElement.asString.replace("<$httpurl>", tempAnchorReplacement).replace(" ", nonBreakingSpace).replace("\n", "<br>").replace(tempAnchorReplacement, anchor))
        }
        return asTextSpan(jsonElement.asString.replace(" ", nonBreakingSpace).replace("\n", "<br>"))
    } else
        return markupToString(jsonElement.asJsonObject)
}

private fun asTextSpan(text: String) = "<span style='color: #4F9DA6'>$text</span>"

private fun markupToString(jsonObject: JsonObject): String {
    val styleBuilder = StringBuilder()
    with(jsonObject) {
        get("bold").let {
            if (!it.isJsonNull && it.asBoolean) styleBuilder.append("font-weight: bold;")
        }
        get("underline").let {
            if (!it.isJsonNull && it.asBoolean) styleBuilder.append("text-decoration: underline;")
        }
        get("color").let {
            if (!it.isJsonNull) styleBuilder.append("color: ${mapColor(it.asString)};") else styleBuilder.append("color: #A1A8B3;")
        }
    }
    return "<span style=\"$styleBuilder\">${jsonObject.get("string").asString.replace(" ", nonBreakingSpace)}</span>"
}

private fun mapColor(color: String): String {
    when (color) {
        "yellow" -> return "#FACF5A"
        "red" -> return "#FF5959"
    }
    return color
}
