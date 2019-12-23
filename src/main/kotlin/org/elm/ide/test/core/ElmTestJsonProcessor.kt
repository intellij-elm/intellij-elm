package org.elm.ide.test.core

import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonSyntaxException
import com.intellij.execution.testframework.sm.runner.events.*
import org.elm.ide.test.core.LabelUtils.commonParent
import org.elm.ide.test.core.LabelUtils.getName
import org.elm.ide.test.core.LabelUtils.subParents
import org.elm.ide.test.core.LabelUtils.toErrorLocationUrl
import org.elm.ide.test.core.LabelUtils.toSuiteLocationUrl
import org.elm.ide.test.core.LabelUtils.toTestLocationUrl
import org.elm.ide.test.core.json.CompileErrors
import org.elm.ide.test.core.json.Error
import java.nio.file.Path

class ElmTestJsonProcessor {

    private var currentPath = LabelUtils.EMPTY_PATH

    fun accept(text: String): Sequence<TreeNodeEvent>? {
        try {
            val obj: JsonObject = gson.fromJson(text, JsonObject::class.java) ?: return null
            if ("compile-errors" == obj.get("type")?.asString) {
                return accept(toCompileErrors(obj))
            }

            val event = obj.get("event")?.asString
            if ("runStart" == event) {
                currentPath = LabelUtils.EMPTY_PATH
                return emptySequence()
            } else if ("runComplete" == event) {
                val closeAll = closeSuitePaths(currentPath, LabelUtils.EMPTY_PATH)
                        .map { newTestSuiteFinishedEvent(it) }
                currentPath = LabelUtils.EMPTY_PATH
                return closeAll
            } else if ("testCompleted" != event) {
                return emptySequence()
            }

            var path = toPath(obj)
            if ("todo" == getStatus(obj)) {
                path = path.resolve("todo")
            }

            val result: Sequence<TreeNodeEvent> = closeSuitePaths(currentPath, path)
                    .map { this.newTestSuiteFinishedEvent(it) }
                    .plus(openSuitePaths(currentPath, path).map { this.newTestSuiteStartedEvent(it) })
                    .plus(testEvents(path, obj))

            currentPath = path
            return result

        } catch (e: JsonSyntaxException) {
            if (text.contains("Compilation failed")) {
                val json = text.substring(0, text.lastIndexOf("Compilation failed"))
                val obj = gson.fromJson(json, JsonObject::class.java) ?: return null
                if ("compile-errors" == obj.get("type")?.asString) {
                    return accept(toCompileErrors(obj))
                }
            }
            return null
        }

    }

    private fun newTestSuiteStartedEvent(path: Path): TestSuiteStartedEvent {
        return TestSuiteStartedEvent(getName(path), toSuiteLocationUrl(path))
    }

    private fun newTestSuiteFinishedEvent(path: Path): TestSuiteFinishedEvent {
        return TestSuiteFinishedEvent(getName(path))
    }

    private fun accept(compileErrors: CompileErrors): Sequence<TreeNodeEvent> {
        return compileErrors.errors
                ?.asSequence()
                ?.flatMap { this.toErrorEvents(it) }
                ?: emptySequence()
    }

    fun toCompileErrors(obj: JsonObject): CompileErrors {
        return gson.fromJson(obj, CompileErrors::class.java)
    }

    private fun toErrorEvents(error: Error): Sequence<TreeNodeEvent> {
        return error.problems
                ?.asSequence()
                ?.flatMap { problem ->
                    sequenceOf(
                            TestStartedEvent(problem.title!!, toErrorLocationUrl(error.path!!, problem.region?.start!!.line, problem.region?.start!!.column)),
                            TestFailedEvent(problem.title!!, null, problem.textMessage, null, true, null, null, null, null, false, false, -1)
                    )
                }
                ?: emptySequence()
    }

    companion object {

        private val gson = GsonBuilder().setPrettyPrinting().create()

        fun testEvents(path: Path, obj: JsonObject): Sequence<TreeNodeEvent> {
            val status = getStatus(obj)
            if ("pass" == status) {
                val duration = java.lang.Long.parseLong(obj.get("duration").asString)
                return sequenceOf(newTestStartedEvent(path))
                        .plus(newTestFinishedEvent(path, duration))
            } else if ("todo" == status) {
                val comment = getComment(obj)
                return sequenceOf(newTestIgnoredEvent(path, comment))
            }
            try {
                val message = getMessage(obj)
                val actual = getActual(obj)
                val expected = getExpected(obj)

                return sequenceOf(newTestStartedEvent(path))
                        .plus(newTestFailedEvent(path, actual, expected, message
                                ?: ""))
            } catch (e: Throwable) {
                val failures = GsonBuilder().setPrettyPrinting().create().toJson(obj.get("failures"))
                return sequenceOf(newTestStartedEvent(path))
                        .plus(newTestFailedEvent(path, null, null, failures))
            }

        }

        private fun newTestIgnoredEvent(path: Path, comment: String?): TestIgnoredEvent {
            return TestIgnoredEvent(getName(path), sureText(comment), null)
        }

        private fun newTestFinishedEvent(path: Path, duration: Long): TestFinishedEvent {
            return TestFinishedEvent(getName(path), duration)
        }

        private fun newTestFailedEvent(path: Path, actual: String?, expected: String?, message: String): TestFailedEvent {
            return TestFailedEvent(getName(path), sureText(message), null, false, actual, expected)
        }

        private fun newTestStartedEvent(path: Path): TestStartedEvent {
            return TestStartedEvent(getName(path), toTestLocationUrl(path))
        }

        private fun sureText(comment: String?): String {
            return comment ?: ""
        }

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
            val reason = getReason(obj)
            return if (reason != null)
                if (reason.get("data") != null)
                    if (reason.get("data").isJsonObject)
                        reason.get("data").asJsonObject
                    else
                        null
                else
                    null
            else
                null
        }

        fun getActual(obj: JsonObject): String? {
            return pretty(getDataMember(obj, "actual"))
        }

        fun getExpected(obj: JsonObject): String? {
            return pretty(getDataMember(obj, "expected"))
        }

        private fun getDataMember(obj: JsonObject, name: String): JsonElement? {
            val data = getData(obj)
            return data?.get(name)
        }

        private fun pretty(element: JsonElement?): String? {
            return if (element != null)
                if (element.isJsonPrimitive)
                    element.asString
                else
                    gson.toJson(element)
            else
                null
        }

        private fun getFirstFailure(obj: JsonObject): JsonElement {
            return obj.get("failures").asJsonArray.get(0)
        }

        private fun getStatus(obj: JsonObject): String {
            return obj.get("status").asString
        }

        fun toPath(element: JsonObject): Path {
            return LabelUtils.toPath(
                    element.get("labels").asJsonArray.iterator().asSequence()
                            .map { it.asString }
                            .toList()
            )
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
