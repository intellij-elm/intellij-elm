package org.elm.workspace.commandLineTools

import com.google.gson.stream.JsonReader
import junit.framework.TestCase
import org.elm.lang.ElmTestBase
import org.intellij.lang.annotations.Language
import org.junit.Test


class ElmReviewStreamModelTest : ElmTestBase() {

    @Test
    fun `test type 'error'`() {
        @Language("JSON")
        val json = """
{
  "type": "error",
  "title": "INCORRECT CONFIGURATION",
  "path": "/home/jw/LamderaProjects/test/elm.json",
  "message": [
    "I could not find a review configuration. I was expecting to find an elm.json file and a ReviewConfig.elm file in /home/jw/LamderaProjects/test/review/.\n\nI can help set you up with an initial configuration if you run elm-review init."
  ]
}
        """.trimIndent()

        val reader = JsonReader(json.byteInputStream().bufferedReader())
        reader.isLenient = true
        val report = reader.readErrorReport()
        TestCase.assertEquals(
            listOf(
                ElmReviewWatchError(
                    path = "/home/jw/LamderaProjects/test/elm.json",
                    rule = "INCORRECT CONFIGURATION",
                    message = "I could not find a review configuration. I was expecting to find an elm.json file and a ReviewConfig.elm file in /home/jw/LamderaProjects/test/review/.\n\nI can help set you up with an initial configuration if you run elm-review init.",
                    regionWatch = null,
                    html = null
                )
            ),
            report
        )
    }
}
