package org.elm.workspace.compiler

import com.google.gson.*
import java.util.regex.Pattern

private val urlPattern = Pattern.compile(".*<((http|https)(://.*))>.*", Pattern.CASE_INSENSITIVE or Pattern.DOTALL)
private const val nonBreakingSpace = "&nbsp;"
private const val tempAnchorReplacement = "##TEMP##"


fun elmJsonToCompilerMessages(json: String): List<CompilerMessage> {
    val gson = GsonBuilder().setPrettyPrinting().create()
    val obj = gson.fromJson<JsonObject>(json, JsonObject::class.java)
    val reportType = obj["type"].asString
    return when (reportType) {
        "error" -> {
            val path = obj["path"].asString
            val title = obj["title"].asString
            listOf(CompilerMessage(title, path,
                    createMessageAndRegion(obj["message"].asJsonArray, title, Region(End(0, 0), Start(0, 0)))))
        }
        "compile-errors" -> {
            obj["errors"].asJsonArray.flatMap { error ->
                val path = error.asJsonObject["path"].asString
                val name = error.asJsonObject["name"].asString
                error.asJsonObject["problems"].asJsonArray.map {
                    CompilerMessage(name, path,
                            createMessageAndRegion(
                                    it.asJsonObject["message"].asJsonArray,
                                    it.asJsonObject["title"].asString,
                                    Gson().fromJson(it.asJsonObject["region"].asJsonObject.toString(), Region::class.java)
                            ))
                }
            }
        }
        else ->
            error("Unexpected Elm compiler report type '$reportType'")
    }
}


private fun createMessageAndRegion(message: JsonArray, title: String, region: Region): MessageAndRegion {
    val html = message.joinToString("",
            prefix = "<html><body style=\"font-family: monospace; font-weight: bold\">",
            postfix = "</body></html>"
    ) { itemToString(it) }
    return MessageAndRegion(html, region, title)
}

private fun itemToString(jsonElement: JsonElement): String {
    // Each item is either a String or an object containing the string as well as some formatting options
    if (jsonElement.isJsonPrimitive) {
        val urlMatcher = urlPattern.matcher(jsonElement.asString)
        if (urlMatcher.find()) {
            val httpUrl = urlMatcher.group(1)
            val anchor = "<a href=\"$httpUrl\">$httpUrl</a>"
            return asTextSpan(jsonElement.asString
                    .replace("<$httpUrl>", tempAnchorReplacement)
                    .replace(" ", nonBreakingSpace)
                    .replace("\n", "<br>")
                    .replace(tempAnchorReplacement, anchor))
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
    return "<span style=\"$styleBuilder\">${jsonObject["string"].asString.replace(" ", nonBreakingSpace)}</span>"
}

private fun mapColor(color: String): String =
        when (color) {
            "yellow" -> "#FACF5A"
            "red" -> "#FF5959"
            else -> color
        }
