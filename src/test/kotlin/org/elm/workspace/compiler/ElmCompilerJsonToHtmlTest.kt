package org.elm.workspace.compiler

import junit.framework.TestCase
import org.elm.lang.ElmTestBase
import org.intellij.lang.annotations.Language


class ElmCompilerJsonToHtmlTest : ElmTestBase() {

    // $ elm --version
    // 0.19
    // $ elm make src/Foo.elm --report=json

    fun `test specific error`() {
        @Language("JSON")
        val json = """
            {
              "type": "compile-errors",
              "errors": [
                {
                  "path": "src/Foo.elm",
                  "name": "Main",
                  "problems": [
                    {
                      "title": "TOO MANY ARGS",
                      "region": {
                        "start": { "line": 1, "column": 8 },
                        "end": { "line": 1, "column": 14 }
                      },
                      "message": [
                        "This value is not a function, but it was given 1 argument.\n\n1| blah = \"blah\" 32\n          ",
                        {
                          "bold": false,
                          "underline": false,
                          "color": "red",
                          "string": "^^^^^^"
                        },
                        "\nAre there any missing commas? Or missing parentheses?"
                      ]
                    }
                  ]
                }
              ]
            }""".trimIndent()

        val expectedHtml = """<html><body style="font-family: monospace; font-weight: bold"><span style='color: #4F9DA6'>This&nbsp;value&nbsp;is&nbsp;not&nbsp;a&nbsp;function,&nbsp;but&nbsp;it&nbsp;was&nbsp;given&nbsp;1&nbsp;argument.<br><br>1|&nbsp;blah&nbsp;=&nbsp;"blah"&nbsp;32<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</span><span style="color: #FF5959;">^^^^^^</span><span style='color: #4F9DA6'><br>Are&nbsp;there&nbsp;any&nbsp;missing&nbsp;commas?&nbsp;Or&nbsp;missing&nbsp;parentheses?</span></body></html>"""

        TestCase.assertEquals(elmJsonToCompilerMessages(json),
                listOf(
                        CompilerMessage("Main", path = "src/Foo.elm", messageWithRegion =
                        MessageAndRegion(expectedHtml, Region(Start(1, 8), End(1, 14)), "TOO MANY ARGS"))
                ))
    }

    fun `test generic error without a path`() {
        @Language("JSON")
        val json = """
            {
              "type": "error",
              "path": null,
              "title": "IMPORT CYCLE",
              "message": [
                "Your module imports form a cycle:\n\n    ┌─────┐\n    │    ",
                {
                  "bold": false,
                  "underline": false,
                  "color": "yellow",
                  "string": "Main"
                },
                "\n    └─────┘\n\nLearn more about why this is disallowed and how to break cycles\nhere:<https://elm-lang.org/0.19.0/import-cycles>"
              ]
            }""".trimIndent()

        val expectedHtml = """<html><body style="font-family: monospace; font-weight: bold"><span style='color: #4F9DA6'>Your&nbsp;module&nbsp;imports&nbsp;form&nbsp;a&nbsp;cycle:<br><br>&nbsp;&nbsp;&nbsp;&nbsp;┌─────┐<br>&nbsp;&nbsp;&nbsp;&nbsp;│&nbsp;&nbsp;&nbsp;&nbsp;</span><span style="color: #FACF5A;">Main</span><span style='color: #4F9DA6'><br>&nbsp;&nbsp;&nbsp;&nbsp;└─────┘<br><br>Learn&nbsp;more&nbsp;about&nbsp;why&nbsp;this&nbsp;is&nbsp;disallowed&nbsp;and&nbsp;how&nbsp;to&nbsp;break&nbsp;cycles<br>here:<a href="https://elm-lang.org/0.19.0/import-cycles">https://elm-lang.org/0.19.0/import-cycles</a></span></body></html>"""

        TestCase.assertEquals(elmJsonToCompilerMessages(json),
                listOf(
                        CompilerMessage("IMPORT CYCLE", path = "", messageWithRegion =
                        MessageAndRegion(expectedHtml, Region(Start(0, 0), End(0, 0)), "IMPORT CYCLE"))
                ))
    }

    fun `test generic error with a path and a null color, crazy`() {
        @Language("JSON")
        val json = """
            {
              "type": "error",
              "path": "src/Helper.elm",
              "title": "UNNAMED MODULE",
              "message": [
                "The `Helper` module must start with a line like this:\n\n    ",
                {
                  "bold": false,
                  "underline": false,
                  "color": "yellow",
                  "string": "module Helper exposing (..)"
                },
                "\n\nTry adding that as the first line of your file!\n\n",
                {
                  "bold": false,
                  "underline": true,
                  "color": null,
                  "string": "Note"
                },
                ": It is best to replace (..) with an explicit list of types and functions\nyou want to expose. If you know a value is only used WITHIN this module, it is\nextra easy to refactor. This kind of information is great, especially as your\nproject grows!"
              ]
            }""".trimIndent()

        val expectedHtml = """<html><body style="font-family: monospace; font-weight: bold"><span style='color: #4F9DA6'>The&nbsp;`Helper`&nbsp;module&nbsp;must&nbsp;start&nbsp;with&nbsp;a&nbsp;line&nbsp;like&nbsp;this:<br><br>&nbsp;&nbsp;&nbsp;&nbsp;</span><span style="color: #FACF5A;">module&nbsp;Helper&nbsp;exposing&nbsp;(..)</span><span style='color: #4F9DA6'><br><br>Try&nbsp;adding&nbsp;that&nbsp;as&nbsp;the&nbsp;first&nbsp;line&nbsp;of&nbsp;your&nbsp;file!<br><br></span><span style="text-decoration: underline;color: white;">Note</span><span style='color: #4F9DA6'>:&nbsp;It&nbsp;is&nbsp;best&nbsp;to&nbsp;replace&nbsp;(..)&nbsp;with&nbsp;an&nbsp;explicit&nbsp;list&nbsp;of&nbsp;types&nbsp;and&nbsp;functions<br>you&nbsp;want&nbsp;to&nbsp;expose.&nbsp;If&nbsp;you&nbsp;know&nbsp;a&nbsp;value&nbsp;is&nbsp;only&nbsp;used&nbsp;WITHIN&nbsp;this&nbsp;module,&nbsp;it&nbsp;is<br>extra&nbsp;easy&nbsp;to&nbsp;refactor.&nbsp;This&nbsp;kind&nbsp;of&nbsp;information&nbsp;is&nbsp;great,&nbsp;especially&nbsp;as&nbsp;your<br>project&nbsp;grows!</span></body></html>"""

        TestCase.assertEquals(elmJsonToCompilerMessages(json),
                listOf(
                        CompilerMessage("UNNAMED MODULE", path = "src/Helper.elm", messageWithRegion =
                        MessageAndRegion(expectedHtml, Region(Start(0, 0), End(0, 0)), "UNNAMED MODULE"))
                ))
    }
}
