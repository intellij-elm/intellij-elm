package org.elm.ide.test.core

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.intellij.execution.testframework.sm.runner.events.TestFailedEvent
import com.intellij.execution.testframework.sm.runner.events.TestFinishedEvent
import com.intellij.execution.testframework.sm.runner.events.TestStartedEvent
import com.intellij.execution.testframework.sm.runner.events.TestSuiteStartedEvent
import org.elm.ide.test.core.ElmTestJsonProcessor.Companion.closeSuitePaths
import org.elm.ide.test.core.ElmTestJsonProcessor.Companion.getActual
import org.elm.ide.test.core.ElmTestJsonProcessor.Companion.getComment
import org.elm.ide.test.core.ElmTestJsonProcessor.Companion.getExpected
import org.elm.ide.test.core.ElmTestJsonProcessor.Companion.getMessage
import org.elm.ide.test.core.ElmTestJsonProcessor.Companion.openSuitePaths
import org.elm.ide.test.core.LabelUtils.pathString
import org.elm.ide.test.core.LabelUtils.toPath
import org.junit.Assert.*
import org.junit.Test

class ElmTestJsonProcessorTest {

    private val processor = ElmTestJsonProcessor("tests")

    @Test
    fun junk() {
        val list = processor.accept("junk")
        assertNull(list)
    }

    @Test
    fun runStart() {
        val list = processor.accept("{\"event\":\"runStart\",\"testCount\":\"9\",\"fuzzRuns\":\"100\",\"paths\":[],\"initialSeed\":\"1448022641\"}\n")
        assertTrue(list!!.count() == 0)
    }

    @Test
    fun runComplete() {
        val list = processor
                .accept("{\"event\":\"runComplete\",\"passed\":\"8\",\"failed\":\"1\",\"duration\":\"353\",\"autoFail\":null}\n")
                ?.toList()
        assertTrue(list!!.isEmpty())
    }

    @Test
    fun testCompleted() {
        val list = processor
                .accept("{\"event\":\"testCompleted\",\"status\":\"pass\",\"labels\":[\"Module\",\"test\"],\"failures\":[],\"duration\":\"1\"}")
                ?.toList()
        assertEquals(3, list!!.size)
        assertTrue(list[0] is TestSuiteStartedEvent)
        assertTrue(list[1] is TestStartedEvent)
        assertTrue(list[2] is TestFinishedEvent)
        assertEquals("Module", list[0].name)
        assertEquals("test", list[1].name)
        assertEquals("test", list[2].name)
    }

    @Test
    fun testCompletedWithSlashes() {
        val list = processor
                .accept("{\"event\":\"testCompleted\",\"status\":\"pass\",\"labels\":[\"Module\",\"test / stuff\"],\"failures\":[],\"duration\":\"1\"}")
                ?.toList()
        assertEquals(3, list!!.size)
        assertTrue(list[0] is TestSuiteStartedEvent)
        assertTrue(list[1] is TestStartedEvent)
        assertTrue(list[2] is TestFinishedEvent)
        assertEquals("Module", list[0].name)
        assertEquals("test / stuff", list[1].name)
        assertEquals("test / stuff", list[2].name)
    }

    @Test
    fun toPath1() {
        val obj = Gson().fromJson("{\"labels\":[\"Module\",\"test\"]}", JsonObject::class.java)
        val path = ElmTestJsonProcessor.toPath(obj)
        assertEquals(2, path.nameCount.toLong())
        assertEquals("Module", pathString(path.getName(0)))
        assertEquals("test", pathString(path.getName(1)))
    }

    @Test
    fun toPathWithSlashes() {
        val obj = Gson().fromJson("{\"labels\":[\"Module\",\"test / stuff\"]}", JsonObject::class.java)
        val path = ElmTestJsonProcessor.toPath(obj)
        assertEquals(2, path.nameCount.toLong())
        assertEquals("Module", pathString(path.getName(0)))
        assertEquals("test+%2F+stuff", pathString(path.getName(1)))
    }

    @Test
    fun closeNoSuites() {
        val from = toPath("Module", "suite", "test")
        val to = toPath("Module", "suite", "test2")
        val paths = closeSuitePaths(from, to)
                .toList()
        assertEquals(emptyList<String>(), paths)
    }

    @Test
    fun closeOneSuite() {
        val from = toPath("Module", "suite", "test")
        val to = toPath("Module", "suite2", "test2")
        val paths = closeSuitePaths(from, to)
                .map { pathString(it) }
                .toList()
        assertEquals(listOf("Module/suite"), paths)
    }

    @Test
    fun closeTwoSuites() {
        val from = toPath("Module", "suite", "deep", "test")
        val to = toPath("Module", "suite2", "test2")
        val paths = closeSuitePaths(from, to)
                .map { pathString(it) }
                .toList()
        assertEquals(listOf("Module/suite/deep", "Module/suite"), paths)
    }

    @Test
    fun closeInitialSuite() {
        val to = toPath("Module", "suite", "test")
        val paths = closeSuitePaths(LabelUtils.EMPTY_PATH, to)
                .map { pathString(it) }
                .toList()
        assertEquals(emptyList<String>(), paths)
    }

    @Test
    fun openNoSuites() {
        val from = toPath("Module", "suite", "test")
        val to = toPath("Module", "suite", "test2")
        val paths = openSuitePaths(from, to)
                .map { pathString(it) }
                .toList()
        assertEquals(emptyList<String>(), paths)
    }

    @Test
    fun openOneSuite() {
        val from = toPath("Module", "suite", "test")
        val to = toPath("Module", "suite2", "test2")
        val paths = openSuitePaths(from, to)
                .map { pathString(it) }
                .toList()
        assertEquals(listOf("Module/suite2"), paths)
    }

    @Test
    fun openTwoSuites() {
        val from = toPath("Module", "suite", "test")
        val to = toPath("Module", "suite2", "deep2", "test2")
        val paths = openSuitePaths(from, to)
                .map { pathString(it) }
                .toList()
        assertEquals(listOf("Module/suite2", "Module/suite2/deep2"), paths)
    }

    @Test
    fun openInitialSuites() {
        val to = toPath("Module", "suite", "test")
        val paths = openSuitePaths(LabelUtils.EMPTY_PATH, to)
                .map { pathString(it) }
                .toList()
        assertEquals(listOf("Module", "Module/suite"), paths)
    }

    @Test
    fun openSuiteWithSlash() {
        val from = toPath("Module")
        val to = toPath("Module", "suite / stuff", "test")
        val paths = openSuitePaths(from, to)
                .map { pathString(it) }
                .toList()
        assertEquals(listOf("Module/suite+%2F+stuff"), paths)
    }

    @Test
    fun todo() {
        val text = "{\"event\":\"testCompleted\",\"status\":\"todo\",\"labels\":[\"Exploratory\",\"describe\"],\"failures\":[\"TODO comment\"],\"duration\":\"2\"}"
        val obj = getObject(text)
        assertEquals("TODO comment", getComment(obj))
        assertNull(getMessage(obj))
        assertNull(getExpected(obj))
        assertNull(getActual(obj))
    }

    @Test
    fun fail() {
        val text = "{\"event\":\"testCompleted\",\"status\":\"fail\",\"labels\":[\"Exploratory\",\"describe\",\"fail\"],\"failures\":[{\"given\":null,\"message\":\"boom\",\"reason\":{\"type\":\"custom\",\"data\":\"boom\"}}],\"duration\":\"1\"}"
        val obj = getObject(text)
        assertNull(getComment(obj))
        assertEquals("boom", getMessage(obj))
        assertNull(getExpected(obj))
        assertNull(getActual(obj))
    }

    @Test
    fun failEqual() {
        val text = "{\"event\":\"testCompleted\",\"status\":\"fail\",\"labels\":[\"Exploratory\",\"describe\",\"duplicate nested\",\"ok1\"],\"failures\":[{\"given\":null,\"message\":\"Expect.equal\",\"reason\":{\"type\":\"custom\",\"data\":{\"expected\":\"\\\"value\\\"\",\"actual\":\"\\\"value2\\\"\",\"comparison\":\"Expect.equal\"}}}],\"duration\":\"2\"}"
        val obj = getObject(text)
        assertNull(getComment(obj))
        assertEquals("Expect.equal", getMessage(obj))
        assertEquals("\"value\"", getExpected(obj))
        assertEquals("\"value2\"", getActual(obj))
    }

    @Test
    fun failHtml() {
        val text = "{\"event\":\"testCompleted\",\"status\":\"fail\",\"labels\":[\"Exploratory\",\"Html tests 1\",\"... fails\"],\"failures\":[{\"given\":null,\"message\":\"▼ Query.fromHtml\\n\\n    <div class=\\\"container\\\">\\n        <button>\\n            I'm a button!\\n        </button>\\n    </div>\\n\\n\\n▼ Query.find [ tag \\\"button1\\\" ]\\n\\n0 matches found for this query.\\n\\n\\n✗ Query.find always expects to find 1 element, but it found 0 instead.\",\"reason\":{\"type\":\"custom\",\"data\":\"▼ Query.fromHtml\\n\\n    <div class=\\\"container\\\">\\n        <button>\\n            I'm a button!\\n        </button>\\n    </div>\\n\\n\\n▼ Query.find [ tag \\\"button1\\\" ]\\n\\n0 matches found for this query.\\n\\n\\n✗ Query.find always expects to find 1 element, but it found 0 instead.\"}}],\"duration\":\"15\"}"
        val obj = getObject(text)
        assertNull(getComment(obj))

        val message = getMessage(obj)
        assertNotNull(message)
        assertTrue(message!!.contains("I'm a button!"))

        assertNull(getExpected(obj))
        assertNull(getActual(obj))
    }

    @Test
    fun failEqualLists() {
        val text = "{\"event\":\"testCompleted\",\"status\":\"fail\",\"labels\":[\"Deep.Exploratory\",\"Variuous Fails\",\"equalLists\"],\"failures\":[{\"given\":null,\"message\":\"Expect.equalLists\",\"reason\":{\"type\":\"custom\",\"data\":{\"expected\":[\"\\\"one\\\"\",\"\\\"expected\\\"\"],\"actual\":[\"\\\"one\\\"\",\"\\\"actual\\\"\"]}}}],\"duration\":\"1\"}\n"
        val obj = getObject(text)
        assertNull(getComment(obj))
        assertNull(getComment(obj))
        assertEquals("Expect.equalLists", getMessage(obj))
        assertEquals("[\n" +
                "  \"\\\"one\\\"\",\n" +
                "  \"\\\"expected\\\"\"\n" +
                "]", getExpected(obj))
        assertEquals("[\n" +
                "  \"\\\"one\\\"\",\n" +
                "  \"\\\"actual\\\"\"\n" +
                "]", getActual(obj))
    }

    @Test
    fun failFallback() {
        val text = "{\"event\":\"testCompleted\",\"status\":\"fail\",\"labels\":[\"Module\",\"Fails\"],\"failures\":[{\"unknown\": \"format\"}],\"duration\":\"1\"}\n"
        val obj = getObject(text)
        val path = ElmTestJsonProcessor.toPath(obj)

        val list = processor.testEvents(path, obj).toList()

        assertEquals(2, list.size.toLong())
        assertTrue(list[1] is TestFailedEvent)
        assertEquals("[\n" +
                "  {\n" +
                "    \"unknown\": \"format\"\n" +
                "  }\n" +
                "]", (list[1] as TestFailedEvent).localizedFailureMessage)
    }

    private fun getObject(text: String): JsonObject {
        return Gson().fromJson(text, JsonObject::class.java)
    }

    @Test
    fun testCompletedWithLocation() {
        val list = processor
                .accept("{\"event\":\"testCompleted\",\"status\":\"pass\",\"labels\":[\"Module\",\"test\"],\"failures\":[],\"duration\":\"1\"}")
                ?.toList()
        assertEquals(3, list!!.size)
        assertTrue(list[0] is TestSuiteStartedEvent)
        assertTrue(list[1] is TestStartedEvent)
        assertTrue(list[2] is TestFinishedEvent)
        assertEquals("elmTestDescribe://Module", (list[0] as TestSuiteStartedEvent).locationUrl)
        assertEquals("elmTestTest://Module/test", (list[1] as TestStartedEvent).locationUrl)
    }

    @Test
    fun testCompletedWithLocationNested() {
        val list = processor
                .accept("{\"event\":\"testCompleted\",\"status\":\"pass\",\"labels\":[\"Nested.Module\",\"test\"],\"failures\":[],\"duration\":\"1\"}")
                ?.toList()
        assertEquals(3, list!!.size)
        assertTrue(list[0] is TestSuiteStartedEvent)
        assertTrue(list[1] is TestStartedEvent)
        assertTrue(list[2] is TestFinishedEvent)
        assertEquals("elmTestDescribe://Nested.Module", (list[0] as TestSuiteStartedEvent).locationUrl)
        assertEquals("elmTestTest://Nested.Module/test", (list[1] as TestStartedEvent).locationUrl)
    }

    @Test
    fun testCompletedFailedWithLocation() {
        val list = processor
                .accept("{\"event\":\"testCompleted\",\"status\":\"fail\",\"labels\":[\"Exploratory\",\"describe\",\"fail\"],\"failures\":[{\"given\":null,\"message\":\"boom\",\"reason\":{\"type\":\"custom\",\"data\":\"boom\"}}],\"duration\":\"1\"}")
                ?.toList()
        assertEquals(4, list!!.size)
        assertTrue(list[0] is TestSuiteStartedEvent)
        assertTrue(list[1] is TestSuiteStartedEvent)
        assertTrue(list[2] is TestStartedEvent)
        assertTrue(list[3] is TestFailedEvent)
        assertEquals("elmTestDescribe://Exploratory", (list[0] as TestSuiteStartedEvent).locationUrl)
        assertEquals("elmTestDescribe://Exploratory/describe", (list[1] as TestSuiteStartedEvent).locationUrl)
        assertEquals("elmTestTest://Exploratory/describe/fail", (list[2] as TestStartedEvent).locationUrl)
    }

    @Test
    fun parseCompileErrors() {
        val json = "{\n" +
                "    \"type\": \"compile-errors\",\n" +
                "    \"errors\": [\n" +
                "        {\n" +
                "            \"path\": \"PATH/tests/UiTests.elm\",\n" +
                "            \"name\": \"UiTests\",\n" +
                "            \"problems\": [\n" +
                "                {\n" +
                "                    \"title\": \"TOO FEW ARGS\",\n" +
                "                    \"region\": {\n" +
                "                        \"start\": {\n" +
                "                            \"line\": 131,\n" +
                "                            \"column\": 33\n" +
                "                        },\n" +
                "                        \"end\": {\n" +
                "                            \"line\": 131,\n" +
                "                            \"column\": 39\n" +
                "                        }\n" +
                "                    },\n" +
                "                    \"message\": [\n" +
                "                        \"The `Msg` type needs 1 argument, but I see 0 instead:\\n\\n131| update : Highlighter MyStyle -> IT.Msg -> Model MyStyle -> Model MyStyle\\n                                     \",\n" +
                "                        {\n" +
                "                            \"bold\": false,\n" +
                "                            \"underline\": false,\n" +
                "                            \"color\": \"red\",\n" +
                "                            \"string\": \"^^^^^^\"\n" +
                "                        },\n" +
                "                        \"\\nWhat is missing? Are some parentheses misplaced?\"\n" +
                "                    ]\n" +
                "                }\n" +
                "            ]\n" +
                "        }\n" +
                "    ]\n" +
                "}"
        val gson = GsonBuilder().setPrettyPrinting().create()
        val obj = gson.fromJson(json, JsonObject::class.java)
        val compileErrors = processor.toCompileErrors(obj)
        //        assertEquals("",gson.toJson(compileErrors));

        assertNotNull(compileErrors)
        assertEquals(1, compileErrors.errors?.size)

        val error = compileErrors.errors!![0]
        assertEquals("PATH/tests/UiTests.elm", error.path)
        assertEquals(1, error.problems?.size)

        val problem = error.problems!![0]
        assertEquals("TOO FEW ARGS", problem.title)
        assertEquals(131, problem.region?.start?.line)
        assertEquals(33, problem.region?.start?.column)

        val expectedMessage = "The `Msg` type needs 1 argument, but I see 0 instead:\n" +
                "\n" +
                "131| update : Highlighter MyStyle -> IT.Msg -> Model MyStyle -> Model MyStyle\n" +
                "                                     ^^^^^^\n" +
                "What is missing? Are some parentheses misplaced?"
        assertEquals(expectedMessage, problem.textMessage)
    }

    @Test
    fun acceptCompileErrors() {
        val json = "{\n" +
                "    \"type\": \"compile-errors\",\n" +
                "    \"errors\": [\n" +
                "        {\n" +
                "            \"path\": \"PATH/tests/UiTests.elm\",\n" +
                "            \"name\": \"UiTests\",\n" +
                "            \"problems\": [\n" +
                "                {\n" +
                "                    \"title\": \"TOO FEW ARGS\",\n" +
                "                    \"region\": {\n" +
                "                        \"start\": {\n" +
                "                            \"line\": 131,\n" +
                "                            \"column\": 33\n" +
                "                        },\n" +
                "                        \"end\": {\n" +
                "                            \"line\": 131,\n" +
                "                            \"column\": 39\n" +
                "                        }\n" +
                "                    },\n" +
                "                    \"message\": [\n" +
                "                        \"The `Msg` type needs 1 argument, but I see 0 instead:\\n\\n131| update : Highlighter MyStyle -> IT.Msg -> Model MyStyle -> Model MyStyle\\n                                     \",\n" +
                "                        {\n" +
                "                            \"bold\": false,\n" +
                "                            \"underline\": false,\n" +
                "                            \"color\": \"red\",\n" +
                "                            \"string\": \"^^^^^^\"\n" +
                "                        },\n" +
                "                        \"\\nWhat is missing? Are some parentheses misplaced?\"\n" +
                "                    ]\n" +
                "                }\n" +
                "            ]\n" +
                "        }\n" +
                "    ]\n" +
                "}"

        val list = processor.accept(json)?.toList()
        assertEquals(2, list!!.size)

        assertTrue(list[0] is TestStartedEvent)
        assertTrue(list[1] is TestFailedEvent)
        assertEquals("elmTestError://PATH/tests/UiTests.elm::131::33", (list[0] as TestStartedEvent).locationUrl)
        assertEquals("TOO FEW ARGS", list[0].name)
        assertEquals("TOO FEW ARGS", list[1].name)
    }

    @Test
    fun acceptEmptyText() {
        val list = processor.accept("")
        assertNull(list)
    }

    @Test
    fun acceptCompileErrorWithoutJson() {
        val list = processor.accept("Compilation failed BLA")
        assertNull(list)
    }
}
