package org.elm.ide.test.core.json

import com.google.gson.JsonElement

class Problem {
    var title: String? = null
    var region: Region? = null
    var message: List<JsonElement>? = null

    val textMessage: String
        get() {
            val hasText = { element:JsonElement -> element.isJsonPrimitive || element.isJsonObject && element.asJsonObject.has("string") }

            val toText = { element:JsonElement ->
                if (element.isJsonPrimitive)
                    element.asString
                else
                    element.asJsonObject.get("string").asString
            }

            return message!!
                    .filter(hasText).joinToString("", transform = toText)
        }


}
