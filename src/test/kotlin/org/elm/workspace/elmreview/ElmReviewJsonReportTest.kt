package org.elm.workspace.elmreview

import com.google.gson.stream.JsonReader
import junit.framework.TestCase
import org.elm.lang.ElmTestBase
import org.elm.workspace.commandLineTools.ElmReviewWatchError
import org.elm.workspace.commandLineTools.LocationWatch
import org.elm.workspace.commandLineTools.RegionWatch
import org.elm.workspace.commandLineTools.readErrorReport
import org.intellij.lang.annotations.Language


class ElmReviewJsonReportTest : ElmTestBase() {

    // $ elm --version
    // 0.19
    // $ elm-review src/Foo.elm --report=json

    fun `parses stream of specific errors`() {
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
                ElmReviewWatchError(
                    suppressed = false,
                    path = "src/Frontend.elm",
                    rule = "NoDebug.Log",
                    message = "Remove the use of `Debug.log` before shipping to production",
                    regionWatch = RegionWatch(LocationWatch(56, 13), LocationWatch(56, 22)),
                    html = """<html><body style="font-family: monospace; font-weight: bold"><span style="color: #33BBC8;">(fix)&nbsp;</span><span style="color: #FF5959;"><a&nbsp;href="https://package.elm-lang.org/packages/jfmengels/elm-review-debug/1.0.6/NoDebug-Log">NoDebug.Log</a></span><span style="color: #4F9DA6">:&nbsp;Remove&nbsp;the&nbsp;use&nbsp;of&nbsp;`Debug.log`&nbsp;before&nbsp;shipping&nbsp;to&nbsp;production<br><br>55|&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;NoOpFrontendMsg&nbsp;-><br>56|&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Debug.log&nbsp;"BBBB"&nbsp;(&nbsp;model,&nbsp;Cmd.none&nbsp;)<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</span><span style="color: #FF5959;">^^^^^^^^^</span><span style="color: #4F9DA6"><br><br>`Debug.log`&nbsp;is&nbsp;useful&nbsp;when&nbsp;developing,&nbsp;but&nbsp;is&nbsp;not&nbsp;meant&nbsp;to&nbsp;be&nbsp;shipped&nbsp;to&nbsp;production&nbsp;or&nbsp;published&nbsp;in&nbsp;a&nbsp;package.&nbsp;I&nbsp;suggest&nbsp;removing&nbsp;its&nbsp;use&nbsp;before&nbsp;committing&nbsp;and&nbsp;attempting&nbsp;to&nbsp;push&nbsp;to&nbsp;production.</span></body></html>"""
                ),
                ElmReviewWatchError(
                    suppressed = false,
                    path = "src/Frontend.elm",
                    rule = "NoDebug.Log",
                    message = "Remove the use of `Debug.log` before shipping to production",
                    regionWatch = RegionWatch(LocationWatch(53, 17), LocationWatch(53, 26)),
                    html = """<html><body style="font-family: monospace; font-weight: bold"><span style="color: #33BBC8;">(fix)&nbsp;</span><span style="color: #FF5959;"><a&nbsp;href="https://package.elm-lang.org/packages/jfmengels/elm-review-debug/1.0.6/NoDebug-Log">NoDebug.Log</a></span><span style="color: #4F9DA6">:&nbsp;Remove&nbsp;the&nbsp;use&nbsp;of&nbsp;`Debug.log`&nbsp;before&nbsp;shipping&nbsp;to&nbsp;production<br><br>52|&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;UrlChanged&nbsp;url&nbsp;-><br>53|&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Debug.log&nbsp;"AAAA"&nbsp;(&nbsp;model,&nbsp;Cmd.none&nbsp;)<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</span><span style="color: #FF5959;">^^^^^^^^^</span><span style="color: #4F9DA6"><br><br>`Debug.log`&nbsp;is&nbsp;useful&nbsp;when&nbsp;developing,&nbsp;but&nbsp;is&nbsp;not&nbsp;meant&nbsp;to&nbsp;be&nbsp;shipped&nbsp;to&nbsp;production&nbsp;or&nbsp;published&nbsp;in&nbsp;a&nbsp;package.&nbsp;I&nbsp;suggest&nbsp;removing&nbsp;its&nbsp;use&nbsp;before&nbsp;committing&nbsp;and&nbsp;attempting&nbsp;to&nbsp;push&nbsp;to&nbsp;production.</span></body></html>"""
                )
            ),
            reader.readErrorReport()
        )
    }

    @Deprecated("stream parser will replace the regular Gson parser for elm-review json output")
    fun `test specific errors`() {
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

        TestCase.assertEquals(
            listOf(
                ElmReviewError(
                    path = "src/Frontend.elm",
                    rule = "NoDebug.Log",
                    message = "Remove the use of `Debug.log` before shipping to production",
                    region = Region(Start(56, 13), End(56, 22)),
                    html = """<html><body style="font-family: monospace; font-weight: bold"><span style="color: #33BBC8;">(fix)&nbsp;</span><span style="color: #FF5959;"><a&nbsp;href="https://package.elm-lang.org/packages/jfmengels/elm-review-debug/1.0.6/NoDebug-Log">NoDebug.Log</a></span><span style="color: #4F9DA6">:&nbsp;Remove&nbsp;the&nbsp;use&nbsp;of&nbsp;`Debug.log`&nbsp;before&nbsp;shipping&nbsp;to&nbsp;production<br><br>55|&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;NoOpFrontendMsg&nbsp;-><br>56|&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Debug.log&nbsp;"BBBB"&nbsp;(&nbsp;model,&nbsp;Cmd.none&nbsp;)<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</span><span style="color: #FF5959;">^^^^^^^^^</span><span style="color: #4F9DA6"><br><br>`Debug.log`&nbsp;is&nbsp;useful&nbsp;when&nbsp;developing,&nbsp;but&nbsp;is&nbsp;not&nbsp;meant&nbsp;to&nbsp;be&nbsp;shipped&nbsp;to&nbsp;production&nbsp;or&nbsp;published&nbsp;in&nbsp;a&nbsp;package.&nbsp;I&nbsp;suggest&nbsp;removing&nbsp;its&nbsp;use&nbsp;before&nbsp;committing&nbsp;and&nbsp;attempting&nbsp;to&nbsp;push&nbsp;to&nbsp;production.</span></body></html>"""
                ),
                ElmReviewError(
                    path = "src/Frontend.elm",
                    rule = "NoDebug.Log",
                    message = "Remove the use of `Debug.log` before shipping to production",
                    region = Region(Start(53, 17), End(53, 26)),
                    html = """<html><body style="font-family: monospace; font-weight: bold"><span style="color: #33BBC8;">(fix)&nbsp;</span><span style="color: #FF5959;"><a&nbsp;href="https://package.elm-lang.org/packages/jfmengels/elm-review-debug/1.0.6/NoDebug-Log">NoDebug.Log</a></span><span style="color: #4F9DA6">:&nbsp;Remove&nbsp;the&nbsp;use&nbsp;of&nbsp;`Debug.log`&nbsp;before&nbsp;shipping&nbsp;to&nbsp;production<br><br>52|&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;UrlChanged&nbsp;url&nbsp;-><br>53|&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Debug.log&nbsp;"AAAA"&nbsp;(&nbsp;model,&nbsp;Cmd.none&nbsp;)<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</span><span style="color: #FF5959;">^^^^^^^^^</span><span style="color: #4F9DA6"><br><br>`Debug.log`&nbsp;is&nbsp;useful&nbsp;when&nbsp;developing,&nbsp;but&nbsp;is&nbsp;not&nbsp;meant&nbsp;to&nbsp;be&nbsp;shipped&nbsp;to&nbsp;production&nbsp;or&nbsp;published&nbsp;in&nbsp;a&nbsp;package.&nbsp;I&nbsp;suggest&nbsp;removing&nbsp;its&nbsp;use&nbsp;before&nbsp;committing&nbsp;and&nbsp;attempting&nbsp;to&nbsp;push&nbsp;to&nbsp;production.</span></body></html>"""
                )
            ),
            elmReviewJsonToMessages(json)
        )
    }

    fun `parse stream of specific errors, one suppressed`() {
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
                ElmReviewWatchError(
                    suppressed = true,
                    path = "src/Frontend.elm",
                    rule = "NoDebug.Log",
                    message = "Remove the use of `Debug.log` before shipping to production",
                    regionWatch = RegionWatch(LocationWatch(56, 13), LocationWatch(56, 22)),
                    html = """<html><body style="font-family: monospace; font-weight: bold"><span style="color: #33BBC8;">(fix)&nbsp;</span><span style="color: #FF5959;"><a&nbsp;href="https://package.elm-lang.org/packages/jfmengels/elm-review-debug/1.0.6/NoDebug-Log">NoDebug.Log</a></span><span style="color: #4F9DA6">:&nbsp;Remove&nbsp;the&nbsp;use&nbsp;of&nbsp;`Debug.log`&nbsp;before&nbsp;shipping&nbsp;to&nbsp;production<br><br>55|&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;NoOpFrontendMsg&nbsp;-><br>56|&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Debug.log&nbsp;"BBBB"&nbsp;(&nbsp;model,&nbsp;Cmd.none&nbsp;)<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</span><span style="color: #FF5959;">^^^^^^^^^</span><span style="color: #4F9DA6"><br><br>`Debug.log`&nbsp;is&nbsp;useful&nbsp;when&nbsp;developing,&nbsp;but&nbsp;is&nbsp;not&nbsp;meant&nbsp;to&nbsp;be&nbsp;shipped&nbsp;to&nbsp;production&nbsp;or&nbsp;published&nbsp;in&nbsp;a&nbsp;package.&nbsp;I&nbsp;suggest&nbsp;removing&nbsp;its&nbsp;use&nbsp;before&nbsp;committing&nbsp;and&nbsp;attempting&nbsp;to&nbsp;push&nbsp;to&nbsp;production.</span></body></html>"""
                ),
                ElmReviewWatchError(
                    suppressed = false,
                    path = "src/Frontend.elm",
                    rule = "NoDebug.Log",
                    message = "Remove the use of `Debug.log` before shipping to production",
                    regionWatch = RegionWatch(LocationWatch(53, 17), LocationWatch(53, 26)),
                    html = """<html><body style="font-family: monospace; font-weight: bold"><span style="color: #33BBC8;">(fix)&nbsp;</span><span style="color: #FF5959;"><a&nbsp;href="https://package.elm-lang.org/packages/jfmengels/elm-review-debug/1.0.6/NoDebug-Log">NoDebug.Log</a></span><span style="color: #4F9DA6">:&nbsp;Remove&nbsp;the&nbsp;use&nbsp;of&nbsp;`Debug.log`&nbsp;before&nbsp;shipping&nbsp;to&nbsp;production<br><br>52|&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;UrlChanged&nbsp;url&nbsp;-><br>53|&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Debug.log&nbsp;"AAAA"&nbsp;(&nbsp;model,&nbsp;Cmd.none&nbsp;)<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</span><span style="color: #FF5959;">^^^^^^^^^</span><span style="color: #4F9DA6"><br><br>`Debug.log`&nbsp;is&nbsp;useful&nbsp;when&nbsp;developing,&nbsp;but&nbsp;is&nbsp;not&nbsp;meant&nbsp;to&nbsp;be&nbsp;shipped&nbsp;to&nbsp;production&nbsp;or&nbsp;published&nbsp;in&nbsp;a&nbsp;package.&nbsp;I&nbsp;suggest&nbsp;removing&nbsp;its&nbsp;use&nbsp;before&nbsp;committing&nbsp;and&nbsp;attempting&nbsp;to&nbsp;push&nbsp;to&nbsp;production.</span></body></html>"""
                )
            ),
            reader.readErrorReport()
        )
    }

    @Deprecated("stream parser will replace the regular Gson parser for elm-review json output")
    fun `test specific errors, one suppressed`() {
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

        TestCase.assertEquals(
            listOf(
                ElmReviewError(
                    path = "src/Frontend.elm",
                    rule = "NoDebug.Log",
                    message = "Remove the use of `Debug.log` before shipping to production",
                    region = Region(Start(53, 17), End(53, 26)),
                    html = """<html><body style="font-family: monospace; font-weight: bold"><span style="color: #33BBC8;">(fix)&nbsp;</span><span style="color: #FF5959;"><a&nbsp;href="https://package.elm-lang.org/packages/jfmengels/elm-review-debug/1.0.6/NoDebug-Log">NoDebug.Log</a></span><span style="color: #4F9DA6">:&nbsp;Remove&nbsp;the&nbsp;use&nbsp;of&nbsp;`Debug.log`&nbsp;before&nbsp;shipping&nbsp;to&nbsp;production<br><br>52|&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;UrlChanged&nbsp;url&nbsp;-><br>53|&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Debug.log&nbsp;"AAAA"&nbsp;(&nbsp;model,&nbsp;Cmd.none&nbsp;)<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</span><span style="color: #FF5959;">^^^^^^^^^</span><span style="color: #4F9DA6"><br><br>`Debug.log`&nbsp;is&nbsp;useful&nbsp;when&nbsp;developing,&nbsp;but&nbsp;is&nbsp;not&nbsp;meant&nbsp;to&nbsp;be&nbsp;shipped&nbsp;to&nbsp;production&nbsp;or&nbsp;published&nbsp;in&nbsp;a&nbsp;package.&nbsp;I&nbsp;suggest&nbsp;removing&nbsp;its&nbsp;use&nbsp;before&nbsp;committing&nbsp;and&nbsp;attempting&nbsp;to&nbsp;push&nbsp;to&nbsp;production.</span></body></html>"""
                )
            ),
            elmReviewJsonToMessages(json)
        )
    }

    fun `test general error`() {
        @Language("JSON")
        val json = """
{"type":"review-errors","errors":[{"path":"src/Backend.elm","errors":[{"rule":"ParsingError","message":"src/Backend.elm is not a correct Elm module","details":["I could not understand the content of this file, and this prevents me from analyzing it. It is highly likely that the contents of the file is not correct Elm code.","I need this file to be fixed before analyzing the rest of the project. If I didn't, I would potentially report incorrect things.","Hint: Try running `elm make`. The compiler should give you better hints on how to resolve the problem."],"region":{"start":{"line":0,"column":0},"end":{"line":0,"column":0}},"formatted":[{"string":"ParsingError","color":"#FF0000"},": src/Backend.elm is not a correct Elm module\n\nI could not understand the content of this file, and this prevents me from analyzing it. It is highly likely that the contents of the file is not correct Elm code.\n\nI need this file to be fixed before analyzing the rest of the project. If I didn't, I would potentially report incorrect things.\n\nHint: Try running `elm make`. The compiler should give you better hints on how to resolve the problem."],"suppressed":false,"originallySuppressed":false}]}]}
        """.trimIndent()

        TestCase.assertEquals(
            listOf(
                ElmReviewError(
                    path = "src/Backend.elm",
                    rule = "ParsingError",
                    message = "src/Backend.elm is not a correct Elm module",
                    region = Region(Start(0, 0), End(0, 0)),
                    html = """<html><body style="font-family: monospace; font-weight: bold"><span style="color: #FF5959;">ParsingError</span><span style="color: #4F9DA6">:&nbsp;src/Backend.elm&nbsp;is&nbsp;not&nbsp;a&nbsp;correct&nbsp;Elm&nbsp;module<br><br>I&nbsp;could&nbsp;not&nbsp;understand&nbsp;the&nbsp;content&nbsp;of&nbsp;this&nbsp;file,&nbsp;and&nbsp;this&nbsp;prevents&nbsp;me&nbsp;from&nbsp;analyzing&nbsp;it.&nbsp;It&nbsp;is&nbsp;highly&nbsp;likely&nbsp;that&nbsp;the&nbsp;contents&nbsp;of&nbsp;the&nbsp;file&nbsp;is&nbsp;not&nbsp;correct&nbsp;Elm&nbsp;code.<br><br>I&nbsp;need&nbsp;this&nbsp;file&nbsp;to&nbsp;be&nbsp;fixed&nbsp;before&nbsp;analyzing&nbsp;the&nbsp;rest&nbsp;of&nbsp;the&nbsp;project.&nbsp;If&nbsp;I&nbsp;didn't,&nbsp;I&nbsp;would&nbsp;potentially&nbsp;report&nbsp;incorrect&nbsp;things.<br><br>Hint:&nbsp;Try&nbsp;running&nbsp;`elm&nbsp;make`.&nbsp;The&nbsp;compiler&nbsp;should&nbsp;give&nbsp;you&nbsp;better&nbsp;hints&nbsp;on&nbsp;how&nbsp;to&nbsp;resolve&nbsp;the&nbsp;problem.</span></body></html>"""
                )
            ),
            elmReviewJsonToMessages(json)
        )
    }
}
