package org.elm.ide.test.core

import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonSyntaxException
import com.intellij.execution.testframework.sm.runner.events.*
import org.elm.ide.test.core.LabelUtils.commonParent
import org.elm.ide.test.core.LabelUtils.getName
import org.elm.ide.test.core.LabelUtils.subParents
import org.elm.ide.test.core.LabelUtils.toLocationUrl
import org.elm.ide.test.core.json.CompileErrors
import org.elm.ide.test.core.json.Error
import java.nio.file.Path

/**
 * Processes events from a test run by elm-test.
 *
 * @param testsRelativeDirPath The path to the directory containing the tests, relative to the project's root (i.e. the
 * folder containing `elm.json`). All events reported by elm-test will supply paths which are relative to this path.
 */
class ElmTestJsonProcessor(private val testsRelativeDirPath: String) {

    private var currentPath = LabelUtils.EMPTY_PATH

    fun accept(text: String): Sequence<TreeNodeEvent>? {
        try {
            val obj: JsonObject = gson.fromJson(text, JsonObject::class.java) ?: return null
            if (obj.get("type")?.asString == "compile-errors") {
                return accept(toCompileErrors(obj))
            }

            when (obj.get("event")?.asString) {
                "runStart" -> {
                    currentPath = LabelUtils.EMPTY_PATH
                    return emptySequence()
                }
                "runComplete" -> {
                    val closeAll = closeSuitePaths(currentPath, LabelUtils.EMPTY_PATH)
                            .map { newTestSuiteFinishedEvent(it) }
                    currentPath = LabelUtils.EMPTY_PATH
                    return closeAll
                }
                "testCompleted" -> {
                    var path = toPath(obj)
                    if (getStatus(obj) == "todo") {
                        path = path.resolve("todo")
                    }

                    val result = closeSuitePaths(currentPath, path)
                            .map { newTestSuiteFinishedEvent(it) }
                            .plus(openSuitePaths(currentPath, path).map { newTestSuiteStartedEvent(it) })
                            .plus(testEvents(path, obj))

                    currentPath = path
                    return result
                }
                else -> {
                    return emptySequence()
                }
            }

        } catch (e: JsonSyntaxException) {
            if (text.contains("Compilation failed")) {
                val json = text.substring(0, text.lastIndexOf("Compilation failed"))
                val obj = gson.fromJson(json, JsonObject::class.java) ?: return null
                if (obj.get("type")?.asString == "compile-errors") {
                    return accept(toCompileErrors(obj))
                }
            }
            return null
        }
    }

    fun testEvents(path: Path, obj: JsonObject): Sequence<TreeNodeEvent> {
        return when (getStatus(obj)) {
            "pass" -> {
                val duration = java.lang.Long.parseLong(obj.get("duration").asString)
                sequenceOf(newTestStartedEvent(path))
                        .plus(newTestFinishedEvent(path, duration))
            }
            "todo" -> {
                val comment = getComment(obj)
                sequenceOf(newTestIgnoredEvent(path, comment))
            }
            else -> try {
                val message = getMessage(obj)
                val actual = getActual(obj)
                val expected = getExpected(obj)

                sequenceOf(newTestStartedEvent(path))
                        .plus(newTestFailedEvent(path, actual, expected, message
                                ?: ""))
            } catch (e: Throwable) {
                val failures = GsonBuilder().setPrettyPrinting().create().toJson(obj.get("failures"))
                sequenceOf(newTestStartedEvent(path))
                        .plus(newTestFailedEvent(path, null, null, failures))
            }
        }
    }

    private fun newTestStartedEvent(path: Path) =
            TestStartedEvent(getName(path), toLocationUrl(path), testsRelativeDirPath)

    private fun newTestSuiteStartedEvent(path: Path) =
            TestSuiteStartedEvent(getName(path), toLocationUrl(path, isSuite = true), testsRelativeDirPath)

    private fun newTestSuiteFinishedEvent(path: Path) =
            TestSuiteFinishedEvent(getName(path))

    private fun accept(compileErrors: CompileErrors): Sequence<TreeNodeEvent> =
            compileErrors.errors?.asSequence()
                    ?.flatMap { toErrorEvents(it) }
                    ?: emptySequence()

    fun toCompileErrors(obj: JsonObject): CompileErrors =
            gson.fromJson(obj, CompileErrors::class.java)

    private fun toErrorEvents(error: Error): Sequence<TreeNodeEvent> {
        return error.problems
                ?.asSequence()
                ?.flatMap { problem ->
                    sequenceOf(
                            TestStartedEvent(problem.title!!, ErrorLabelLocation(
                                    file = error.path!!,
                                    line = problem.region?.start!!.line,
                                    column = problem.region?.start!!.column
                            ).toUrl()),
                            TestFailedEvent(problem.title!!, null, problem.textMessage, null, true, null, null, null, null, false, false, -1)
                    )
                }
                ?: emptySequence()
    }

    companion object {

        private val gson = GsonBuilder().setPrettyPrinting().create()

        private fun newTestIgnoredEvent(path: Path, comment: String?) =
                TestIgnoredEvent(getName(path), comment ?: "", null)

        private fun newTestFinishedEvent(path: Path, duration: Long) =
                TestFinishedEvent(getName(path), duration)

        private fun newTestFailedEvent(path: Path, actual: String?, expected: String?, message: String) =
                TestFailedEvent(getName(path), message, null, false, actual, expected)

        fun getComment(obj: JsonObject): String? {
            return if (getFirstFailure(obj).isJsonPrimitive)
                getFirstFailure(obj).asString
            else
                null
        }

        fun getMessage(obj: JsonObject): String? {
            return if (getFirstFailure(obj).isJsonObject)
                getFirstFailure(obj).asJsonObject.get("message").asString
            else
                null
        }

        fun getReason(obj: JsonObject): JsonObject? {
            return if (getFirstFailure(obj).isJsonObject)
                getFirstFailure(obj).asJsonObject.get("reason").asJsonObject
            else
                null
        }

        private fun getData(obj: JsonObject): JsonObject? {
            val reason = getReason(obj) ?: return null
            val data = reason.get("data") ?: return null
            return when {
                data.isJsonObject -> data.asJsonObject
                else -> null
            }
        }

        fun getActual(obj: JsonObject): String? =
                pretty(getDataMember(obj, "actual"))

        fun getExpected(obj: JsonObject): String? =
                pretty(getDataMember(obj, "expected"))

        private fun getDataMember(obj: JsonObject, name: String): JsonElement? =
                getData(obj)?.get(name)

        private fun pretty(element: JsonElement?): String? =
                when {
                    element == null -> null
                    element.isJsonPrimitive -> element.asString
                    else -> gson.toJson(element)
                }

        private fun getFirstFailure(obj: JsonObject): JsonElement =
                obj.get("failures").asJsonArray.get(0)

        private fun getStatus(obj: JsonObject): String =
                obj.get("status").asString

        fun toPath(element: JsonObject): Path {
            val labels = element.get("labels").asJsonArray.asSequence()
                    .map { it.asString }
                    .toList()
            return LabelUtils.toPath(*labels.toTypedArray())
        }

        fun closeSuitePaths(from: Path, to: Path): Sequence<Path> {
            val commonParent = commonParent(from, to)
            return subParents(from, commonParent)
        }

        fun openSuitePaths(from: Path, to: Path): Sequence<Path> {
            val commonParent = commonParent(from, to)
            return subParents(to, commonParent).toList().reversed().asSequence()
        }
    }

}
