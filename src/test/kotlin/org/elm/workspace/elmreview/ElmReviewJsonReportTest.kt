package org.elm.workspace.elmreview

import com.google.gson.stream.JsonReader
import junit.framework.TestCase
import org.elm.lang.ElmTestBase
import org.intellij.lang.annotations.Language
import org.junit.Ignore
import org.junit.Test


class ElmReviewJsonReportTest : ElmTestBase() {

    // $ elm --version
    // 0.19
    // $ elm-review src/Foo.elm --report=json

    @Test
    fun `parses type review-errors`() {
        @Language("JSON")
        val json = """
        {
          "type": "review-errors",
          "errors": [
            {
              "path": "src/Frontend.elm",
              "errors": [
                {
                  "rule": "NoDebug.Log",
                  "message": "Remove the use of `Debug.log` before shipping to production",
                  "ruleLink": "https://package.elm-lang.org/packages/jfmengels/elm-review-debug/1.0.6/NoDebug-Log",
                  "details": [
                    "`Debug.log` is useful when developing, but is not meant to be shipped to production or published in a package. I suggest removing its use before committing and attempting to push to production."
                  ],
                  "region": {
                    "start": {
                      "line": 56,
                      "column": 13
                    },
                    "end": {
                      "line": 56,
                      "column": 22
                    }
                  },
                  "fix": [
                    {
                      "range": {
                        "start": {
                          "line": 56,
                          "column": 13
                        },
                        "end": {
                          "line": 56,
                          "column": 30
                        }
                      },
                      "string": ""
                    }
                  ],
                  "formatted": [
                    {
                      "string": "(fix) ",
                      "color": "#33BBC8"
                    },
                    {
                      "string": "NoDebug.Log",
                      "color": "#FF0000",
                      "href": "https://package.elm-lang.org/packages/jfmengels/elm-review-debug/1.0.6/NoDebug-Log"
                    },
                    ": Remove the use of `Debug.log` before shipping to production\n\n55|         NoOpFrontendMsg ->\n56|             Debug.log \"BBBB\" ( model, Cmd.none )\n                ",
                    {
                      "string": "^^^^^^^^^",
                      "color": "#FF0000"
                    },
                    "\n\n`Debug.log` is useful when developing, but is not meant to be shipped to production or published in a package. I suggest removing its use before committing and attempting to push to production."
                  ],
                  "suppressed": false,
                  "originallySuppressed": false
                },
                {
                  "rule": "NoDebug.Log",
                  "message": "Remove the use of `Debug.log` before shipping to production",
                  "ruleLink": "https://package.elm-lang.org/packages/jfmengels/elm-review-debug/1.0.6/NoDebug-Log",
                  "details": [
                    "`Debug.log` is useful when developing, but is not meant to be shipped to production or published in a package. I suggest removing its use before committing and attempting to push to production."
                  ],
                  "region": {
                    "start": {
                      "line": 53,
                      "column": 17
                    },
                    "end": {
                      "line": 53,
                      "column": 26
                    }
                  },
                  "fix": [
                    {
                      "range": {
                        "start": {
                          "line": 53,
                          "column": 17
                        },
                        "end": {
                          "line": 53,
                          "column": 34
                        }
                      },
                      "string": ""
                    }
                  ],
                  "formatted": [
                    {
                      "string": "(fix) ",
                      "color": "#33BBC8"
                    },
                    {
                      "string": "NoDebug.Log",
                      "color": "#FF0000",
                      "href": "https://package.elm-lang.org/packages/jfmengels/elm-review-debug/1.0.6/NoDebug-Log"
                    },
                    ": Remove the use of `Debug.log` before shipping to production\n\n52|         UrlChanged url ->\n53|                 Debug.log \"AAAA\" ( model, Cmd.none )\n                    ",
                    {
                      "string": "^^^^^^^^^",
                      "color": "#FF0000"
                    },
                    "\n\n`Debug.log` is useful when developing, but is not meant to be shipped to production or published in a package. I suggest removing its use before committing and attempting to push to production."
                  ],
                  "suppressed": false,
                  "originallySuppressed": false
                }
              ]
            }
          ]
        }""".trimIndent()

        val reader = JsonReader(json.byteInputStream().bufferedReader())
        reader.isLenient = true

        TestCase.assertEquals(
            listOf(
                ElmReviewError(
                    suppressed = false,
                    path = "src/Frontend.elm",
                    rule = "NoDebug.Log",
                    message = "Remove the use of `Debug.log` before shipping to production",
                    region = Region(Location(56, 13), Location(56, 22)),
                    html = """<html><body style="font-family: monospace; font-weight: bold"><span style="color: #33BBC8;">(fix)&nbsp;</span><span style="color: #FF5959;"><a&nbsp;href="https://package.elm-lang.org/packages/jfmengels/elm-review-debug/1.0.6/NoDebug-Log">NoDebug.Log</a></span><span style="color: #4F9DA6">:&nbsp;Remove&nbsp;the&nbsp;use&nbsp;of&nbsp;`Debug.log`&nbsp;before&nbsp;shipping&nbsp;to&nbsp;production<br><br>55|&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;NoOpFrontendMsg&nbsp;-><br>56|&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Debug.log&nbsp;"BBBB"&nbsp;(&nbsp;model,&nbsp;Cmd.none&nbsp;)<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</span><span style="color: #FF5959;">^^^^^^^^^</span><span style="color: #4F9DA6"><br><br>`Debug.log`&nbsp;is&nbsp;useful&nbsp;when&nbsp;developing,&nbsp;but&nbsp;is&nbsp;not&nbsp;meant&nbsp;to&nbsp;be&nbsp;shipped&nbsp;to&nbsp;production&nbsp;or&nbsp;published&nbsp;in&nbsp;a&nbsp;package.&nbsp;I&nbsp;suggest&nbsp;removing&nbsp;its&nbsp;use&nbsp;before&nbsp;committing&nbsp;and&nbsp;attempting&nbsp;to&nbsp;push&nbsp;to&nbsp;production.</span></body></html>"""
                ),
                ElmReviewError(
                    suppressed = false,
                    path = "src/Frontend.elm",
                    rule = "NoDebug.Log",
                    message = "Remove the use of `Debug.log` before shipping to production",
                    region = Region(Location(53, 17), Location(53, 26)),
                    html = """<html><body style="font-family: monospace; font-weight: bold"><span style="color: #33BBC8;">(fix)&nbsp;</span><span style="color: #FF5959;"><a&nbsp;href="https://package.elm-lang.org/packages/jfmengels/elm-review-debug/1.0.6/NoDebug-Log">NoDebug.Log</a></span><span style="color: #4F9DA6">:&nbsp;Remove&nbsp;the&nbsp;use&nbsp;of&nbsp;`Debug.log`&nbsp;before&nbsp;shipping&nbsp;to&nbsp;production<br><br>52|&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;UrlChanged&nbsp;url&nbsp;-><br>53|&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Debug.log&nbsp;"AAAA"&nbsp;(&nbsp;model,&nbsp;Cmd.none&nbsp;)<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</span><span style="color: #FF5959;">^^^^^^^^^</span><span style="color: #4F9DA6"><br><br>`Debug.log`&nbsp;is&nbsp;useful&nbsp;when&nbsp;developing,&nbsp;but&nbsp;is&nbsp;not&nbsp;meant&nbsp;to&nbsp;be&nbsp;shipped&nbsp;to&nbsp;production&nbsp;or&nbsp;published&nbsp;in&nbsp;a&nbsp;package.&nbsp;I&nbsp;suggest&nbsp;removing&nbsp;its&nbsp;use&nbsp;before&nbsp;committing&nbsp;and&nbsp;attempting&nbsp;to&nbsp;push&nbsp;to&nbsp;production.</span></body></html>"""
                )
            ),
            reader.readErrorReport()
        )
    }

    @Test
    fun `parses type review-errors, one suppressed`() {
        @Language("JSON")
        val json = """
        {
          "type": "review-errors",
          "errors": [
            {
              "path": "src/Frontend.elm",
              "errors": [
                {
                  "rule": "NoDebug.Log",
                  "message": "Remove the use of `Debug.log` before shipping to production",
                  "ruleLink": "https://package.elm-lang.org/packages/jfmengels/elm-review-debug/1.0.6/NoDebug-Log",
                  "details": [
                    "`Debug.log` is useful when developing, but is not meant to be shipped to production or published in a package. I suggest removing its use before committing and attempting to push to production."
                  ],
                  "region": {
                    "start": {
                      "line": 56,
                      "column": 13
                    },
                    "end": {
                      "line": 56,
                      "column": 22
                    }
                  },
                  "fix": [
                    {
                      "range": {
                        "start": {
                          "line": 56,
                          "column": 13
                        },
                        "end": {
                          "line": 56,
                          "column": 30
                        }
                      },
                      "string": ""
                    }
                  ],
                  "formatted": [
                    {
                      "string": "(fix) ",
                      "color": "#33BBC8"
                    },
                    {
                      "string": "NoDebug.Log",
                      "color": "#FF0000",
                      "href": "https://package.elm-lang.org/packages/jfmengels/elm-review-debug/1.0.6/NoDebug-Log"
                    },
                    ": Remove the use of `Debug.log` before shipping to production\n\n55|         NoOpFrontendMsg ->\n56|             Debug.log \"BBBB\" ( model, Cmd.none )\n                ",
                    {
                      "string": "^^^^^^^^^",
                      "color": "#FF0000"
                    },
                    "\n\n`Debug.log` is useful when developing, but is not meant to be shipped to production or published in a package. I suggest removing its use before committing and attempting to push to production."
                  ],
                  "suppressed": true,
                  "originallySuppressed": false
                },
                {
                  "rule": "NoDebug.Log",
                  "message": "Remove the use of `Debug.log` before shipping to production",
                  "ruleLink": "https://package.elm-lang.org/packages/jfmengels/elm-review-debug/1.0.6/NoDebug-Log",
                  "details": [
                    "`Debug.log` is useful when developing, but is not meant to be shipped to production or published in a package. I suggest removing its use before committing and attempting to push to production."
                  ],
                  "region": {
                    "start": {
                      "line": 53,
                      "column": 17
                    },
                    "end": {
                      "line": 53,
                      "column": 26
                    }
                  },
                  "fix": [
                    {
                      "range": {
                        "start": {
                          "line": 53,
                          "column": 17
                        },
                        "end": {
                          "line": 53,
                          "column": 34
                        }
                      },
                      "string": ""
                    }
                  ],
                  "formatted": [
                    {
                      "string": "(fix) ",
                      "color": "#33BBC8"
                    },
                    {
                      "string": "NoDebug.Log",
                      "color": "#FF0000",
                      "href": "https://package.elm-lang.org/packages/jfmengels/elm-review-debug/1.0.6/NoDebug-Log"
                    },
                    ": Remove the use of `Debug.log` before shipping to production\n\n52|         UrlChanged url ->\n53|                 Debug.log \"AAAA\" ( model, Cmd.none )\n                    ",
                    {
                      "string": "^^^^^^^^^",
                      "color": "#FF0000"
                    },
                    "\n\n`Debug.log` is useful when developing, but is not meant to be shipped to production or published in a package. I suggest removing its use before committing and attempting to push to production."
                  ],
                  "suppressed": false,
                  "originallySuppressed": false
                }
              ]
            }
          ]
        }""".trimIndent()

        val reader = JsonReader(json.byteInputStream().bufferedReader())
        reader.isLenient = true

        TestCase.assertEquals(
            listOf(
                ElmReviewError(
                    suppressed = true,
                    path = "src/Frontend.elm",
                    rule = "NoDebug.Log",
                    message = "Remove the use of `Debug.log` before shipping to production",
                    region = Region(Location(56, 13), Location(56, 22)),
                    html = """<html><body style="font-family: monospace; font-weight: bold"><span style="color: #33BBC8;">(fix)&nbsp;</span><span style="color: #FF5959;"><a&nbsp;href="https://package.elm-lang.org/packages/jfmengels/elm-review-debug/1.0.6/NoDebug-Log">NoDebug.Log</a></span><span style="color: #4F9DA6">:&nbsp;Remove&nbsp;the&nbsp;use&nbsp;of&nbsp;`Debug.log`&nbsp;before&nbsp;shipping&nbsp;to&nbsp;production<br><br>55|&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;NoOpFrontendMsg&nbsp;-><br>56|&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Debug.log&nbsp;"BBBB"&nbsp;(&nbsp;model,&nbsp;Cmd.none&nbsp;)<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</span><span style="color: #FF5959;">^^^^^^^^^</span><span style="color: #4F9DA6"><br><br>`Debug.log`&nbsp;is&nbsp;useful&nbsp;when&nbsp;developing,&nbsp;but&nbsp;is&nbsp;not&nbsp;meant&nbsp;to&nbsp;be&nbsp;shipped&nbsp;to&nbsp;production&nbsp;or&nbsp;published&nbsp;in&nbsp;a&nbsp;package.&nbsp;I&nbsp;suggest&nbsp;removing&nbsp;its&nbsp;use&nbsp;before&nbsp;committing&nbsp;and&nbsp;attempting&nbsp;to&nbsp;push&nbsp;to&nbsp;production.</span></body></html>"""
                ),
                ElmReviewError(
                    suppressed = false,
                    path = "src/Frontend.elm",
                    rule = "NoDebug.Log",
                    message = "Remove the use of `Debug.log` before shipping to production",
                    region = Region(Location(53, 17), Location(53, 26)),
                    html = """<html><body style="font-family: monospace; font-weight: bold"><span style="color: #33BBC8;">(fix)&nbsp;</span><span style="color: #FF5959;"><a&nbsp;href="https://package.elm-lang.org/packages/jfmengels/elm-review-debug/1.0.6/NoDebug-Log">NoDebug.Log</a></span><span style="color: #4F9DA6">:&nbsp;Remove&nbsp;the&nbsp;use&nbsp;of&nbsp;`Debug.log`&nbsp;before&nbsp;shipping&nbsp;to&nbsp;production<br><br>52|&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;UrlChanged&nbsp;url&nbsp;-><br>53|&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Debug.log&nbsp;"AAAA"&nbsp;(&nbsp;model,&nbsp;Cmd.none&nbsp;)<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</span><span style="color: #FF5959;">^^^^^^^^^</span><span style="color: #4F9DA6"><br><br>`Debug.log`&nbsp;is&nbsp;useful&nbsp;when&nbsp;developing,&nbsp;but&nbsp;is&nbsp;not&nbsp;meant&nbsp;to&nbsp;be&nbsp;shipped&nbsp;to&nbsp;production&nbsp;or&nbsp;published&nbsp;in&nbsp;a&nbsp;package.&nbsp;I&nbsp;suggest&nbsp;removing&nbsp;its&nbsp;use&nbsp;before&nbsp;committing&nbsp;and&nbsp;attempting&nbsp;to&nbsp;push&nbsp;to&nbsp;production.</span></body></html>"""
                )
            ),
            reader.readErrorReport()
        )
    }

    @Test
    fun `parses type error`() {
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
                ElmReviewError(
                    path = "/home/jw/LamderaProjects/test/elm.json",
                    rule = "INCORRECT CONFIGURATION",
                    message = "I could not find a review configuration. I was expecting to find an elm.json file and a ReviewConfig.elm file in /home/jw/LamderaProjects/test/review/.\n\nI can help set you up with an initial configuration if you run elm-review init.",
                    region = null,
                    html = null
                )
            ),
            report
        )
    }

    @Test
    fun `parses type compile-errors`() {
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
                ElmReviewError(
                    path = "/home/jw/LamderaProjects/test/review/src/ReviewConfig.elm",
                    rule = "UNFINISHED IMPORT",
                    message = null,
                    region = null,
                    html = null
                )
            ),
            report
        )
    }

    // TODO
    @Ignore
    fun `parses type 'compile-errors' with errors array`() {
        @Language("JSON")
        val json = """
  [{
    "type": "compile-errors",
    "errors": [
      {
        "path": "/home/jw/LamderaProjects/test/review/src/ReviewConfig.elm",
        "name": "ReviewConfig",
        "problems": [
          {
            "title": "WEIRD DECLARATION",
            "region": {
              "start": {
                "line": 25,
                "column": 1
              },
              "end": {
                "line": 25,
                "column": 1
              }
            },
            "message": [
              "I am trying to parse a declaration, but I am getting stuck here:\n\n25| \n    ",
              {
                "bold": false,
                "underline": false,
                "color": "RED",
                "string": "^"
              },
              "\nWhen a line has no spaces at the beginning, I expect it to be a declaration like\none of these:\n\n    greet : String -> String\n    greet name =\n      ",
              {
                "bold": false,
                "underline": false,
                "color": "yellow",
                "string": "\"Hello \""
              },
              " ++ name ++ ",
              {
                "bold": false,
                "underline": false,
                "color": "yellow",
                "string": "\"!\""
              },
              "\n    \n    ",
              {
                "bold": false,
                "underline": false,
                "color": "CYAN",
                "string": "type"
              },
              " User = Anonymous | LoggedIn String\n\nTry to make your declaration look like one of those? Or if this is not supposed\nto be a declaration, try adding some spaces before it?"
            ]
          }
        ]
      }
    ]
  }
]""".trimIndent()

        val reader = JsonReader(json.byteInputStream().bufferedReader())
        reader.isLenient = true
        val report = reader.readErrorReport()
        TestCase.assertEquals(
            emptyList<ElmReviewError>(),
            report
        )
    }
}
