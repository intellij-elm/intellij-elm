package org.elm.workspace.compiler

import org.junit.Test
import java.util.regex.Pattern
import kotlin.test.assertEquals


class ElmJsonReportTest {

    private val elmJsonReport = ElmJsonReport()

    /* TODO :
        val jTextPane = JTextPane()
        jTextPane.contentType = "text/html"
        jTextPane.text = "<html>"
    */

    @Test
    fun test() {
        val json = this.javaClass.getResource("/compiler_json_reports/errors.json").readText()
        val expected = listOf(
                CompilerMessage("src/Main.elm", MessageAndRegion("<html><body style=\"font-family: monospace\">I&nbsp;was&nbsp;not&nbsp;expecting&nbsp;this&nbsp;vertical&nbsp;bar&nbsp;while&nbsp;parsing&nbsp;ype's&nbsp;definition.<br><br>10|&nbsp;ype&nbsp;Msg&nbsp;=&nbsp;Increment&nbsp;|&nbsp;Decrement<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<span style=\"color: red;\">^</span><br>A&nbsp;vertical&nbsp;bar&nbsp;should&nbsp;only&nbsp;appear&nbsp;in&nbsp;type&nbsp;declarations.&nbsp;Maybe&nbsp;you&nbsp;want&nbsp;||<br>instead?</body></html>", Region(End(21,10), Start(21,10)),"PARSE ERROR"))
                , CompilerMessage("src/Helper.elm", MessageAndRegion("<html><body style=\"font-family: monospace\">I&nbsp;cannot&nbsp;find&nbsp;a&nbsp;`Dict.fromLast`&nbsp;variable:<br><br>6|&nbsp;test&nbsp;=&nbsp;Dict.fromLast&nbsp;[]<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<span style=\"color: red;\">^^^^^^^^^^^^^</span><br>I&nbsp;cannot&nbsp;find&nbsp;a&nbsp;`Dict`&nbsp;import.&nbsp;These&nbsp;names&nbsp;seem&nbsp;close&nbsp;though:<br><br>&nbsp;&nbsp;&nbsp;&nbsp;<span style=\"color: yellow;\">String.fromList</span><br>&nbsp;&nbsp;&nbsp;&nbsp;<span style=\"color: yellow;\">List.concat</span><br>&nbsp;&nbsp;&nbsp;&nbsp;<span style=\"color: yellow;\">List.foldl</span><br>&nbsp;&nbsp;&nbsp;&nbsp;<span style=\"color: yellow;\">List.foldr</span><br><br><span style=\"text-decoration: underline;\">Hint</span>:&nbsp;Read&nbsp;<a href=\"https://elm-lang.org/0.19.0/imports\">https://elm-lang.org/0.19.0/imports</a>&nbsp;to&nbsp;see&nbsp;how&nbsp;`import`<br>declarations&nbsp;work&nbsp;in&nbsp;Elm.</body></html>", Region(End(21,6), Start(8,6)), "NAMING ERROR")))

        val actual = elmJsonReport.elmToCompilerMessages(json)

        assertEquals(expected, actual)
    }

}

fun main() {
    val txt = "s<askdfhg g <https://elm-lang.org/0.19.0/imports> weorty"

    val re = ".*<((http|https)(://.*))>.*"

    val p = Pattern.compile(re, Pattern.CASE_INSENSITIVE or Pattern.DOTALL)
    val m = p.matcher(txt)
    if (m.find()) {
        var groupCount = m.groupCount() - 1
        while (groupCount >= 0) {
            println(m.group(groupCount))
            groupCount -= 1
        }
    }
}
