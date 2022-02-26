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

    @Test
    fun `test type 'compile-errors'`() {
        @Language("JSON")
        val json = """{
  "type": "compile-errors",
  "errors": [
    {
      "path": "/home/jw/LamderaProjects/test/review/src/ReviewConfig.elm",
      "name": "ReviewConfig",
      "problems": [
        {
          "title": "UNFINISHED IMPORT",
          "region": {
            "start": {
              "line": 23,
              "column": 9
            },
            "end": {
              "line": 23,
              "column": 9
            }
          },
          "message": [
            "I am partway through parsing an import, but I got stuck here:\n\n23| --     ]\n            ",
            {
              "bold": false,
              "underline": false,
              "color": "RED",
              "string": "^"
            },
            "\nHere are some examples of valid `import` declarations:\n\n    ",
            {
              "bold": false,
              "underline": false,
              "color": "CYAN",
              "string": "import"
            },
            " Html\n    ",
            {
              "bold": false,
              "underline": false,
              "color": "CYAN",
              "string": "import"
            },
            " Html ",
            {
              "bold": false,
              "underline": false,
              "color": "CYAN",
              "string": "as"
            },
            " H\n    ",
            {
              "bold": false,
              "underline": false,
              "color": "CYAN",
              "string": "import"
            },
            " Html ",
            {
              "bold": false,
              "underline": false,
              "color": "CYAN",
              "string": "as"
            },
            " H ",
            {
              "bold": false,
              "underline": false,
              "color": "CYAN",
              "string": "exposing"
            },
            " (..)\n    ",
            {
              "bold": false,
              "underline": false,
              "color": "CYAN",
              "string": "import"
            },
            " Html ",
            {
              "bold": false,
              "underline": false,
              "color": "CYAN",
              "string": "exposing"
            },
            " (Html, div, text)\n\nYou are probably trying to import a different module, but try to make it look\nlike one of these examples!\n\nRead <https://elm-lang.org/0.19.1/imports> to learn more."
          ]
        }
      ]
    }
  ]
}""".trimIndent()

        val reader = JsonReader(json.byteInputStream().bufferedReader())
        reader.isLenient = true
        val report = reader.readErrorReport()
        TestCase.assertEquals(
            listOf(
                ElmReviewWatchError(
                    path = "/home/jw/LamderaProjects/test/review/src/ReviewConfig.elm",
                    rule = "UNFINISHED IMPORT",
                    message = null,
                    regionWatch = null,
                    html = null
                )
            ),
            report
        )
    }
}
