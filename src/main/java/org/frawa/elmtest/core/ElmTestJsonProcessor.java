package org.frawa.elmtest.core;

import com.google.gson.*;
import com.intellij.execution.testframework.sm.runner.events.*;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.frawa.elmtest.core.LabelUtils.*;

public class ElmTestJsonProcessor {

    private static Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private Path currentPath = LabelUtils.EMPTY_PATH;

    public List<TreeNodeEvent> accept(String text) {
        try {
            JsonObject obj = gson.fromJson(text, JsonObject.class);

            String event = obj.get("event").getAsString();

            if ("runStart".equals(event)) {
                currentPath = LabelUtils.EMPTY_PATH;
                return Collections.emptyList();
            } else if ("runComplete".equals(event)) {
                List<TreeNodeEvent> closeAll = closeSuitePaths(currentPath, EMPTY_PATH)
                        .map((Function<Path, TreeNodeEvent>) this::newTestSuiteFinishedEvent)
                        .collect(Collectors.toList());
                currentPath = LabelUtils.EMPTY_PATH;
                return closeAll;
            } else if (!"testCompleted".equals(event)) {
                return Collections.emptyList();
            }

            Path path = toPath(obj);
            if ("todo".equals(getStatus(obj))) {
                path = path.resolve("todo");
            }

            List<TreeNodeEvent> result = Stream.of(
                    closeSuitePaths(currentPath, path).map((Function<Path, TreeNodeEvent>) this::newTestSuiteFinishedEvent),
                    openSuitePaths(currentPath, path).map((Function<Path, TreeNodeEvent>) this::newTestSuiteStartedEvent),
                    testEvents(path, obj)
            )
                    .flatMap(Function.identity())
                    .collect(Collectors.toList());

            currentPath = path;
            return result;
        } catch (JsonSyntaxException e) {
            return null;
        }
    }

    static Stream<TreeNodeEvent> testEvents(Path path, JsonObject obj) {
        String status = getStatus(obj);
        if ("pass".equals(status)) {
            long duration = Long.parseLong(obj.get("duration").getAsString());
            return Stream.of(
                    newTestStartedEvent(path),
                    newTestFinishedEvent(path, duration)
            );
        } else if ("todo".equals(status)) {
            String comment = getComment(obj);
            return Stream.of(
                    newTestIgnoredEvent(path, comment)
            );
        }
        try {
            String message = getMessage(obj);
            String actual = getActual(obj);
            String expected = getExpected(obj);

            return Stream.of(
                    newTestStartedEvent(path),
                    newTestFailedEvent(path, actual, expected, message != null ? message : "")
            );
        } catch (Throwable e) {
            String failures = new GsonBuilder().setPrettyPrinting().create().toJson(obj.get("failures"));
            return Stream.of(
                    newTestStartedEvent(path),
                    newTestFailedEvent(path, null, null, failures)
            );
        }
    }

    @NotNull
    private TestSuiteStartedEvent newTestSuiteStartedEvent(Path path) {
        return new TestSuiteStartedEvent(getName(path), toSuiteLocationUrl(path));
    }

    @NotNull
    private TestSuiteFinishedEvent newTestSuiteFinishedEvent(Path path) {
        return new TestSuiteFinishedEvent(getName(path));
    }

    @NotNull
    private static TestIgnoredEvent newTestIgnoredEvent(Path path, String comment) {
        return new TestIgnoredEvent(getName(path), sureText(comment), null);
    }

    @NotNull
    private static TestFinishedEvent newTestFinishedEvent(Path path, long duration) {
        return new TestFinishedEvent(getName(path), duration);
    }

    @NotNull
    private static TestFailedEvent newTestFailedEvent(Path path, String actual, String expected, String message) {
        return new TestFailedEvent(getName(path), sureText(message), null, false, actual, expected);
    }

    @NotNull
    private static TestStartedEvent newTestStartedEvent(Path path) {
        return new TestStartedEvent(getName(path), toTestLocationUrl(path));
    }

    @NotNull
    private static String sureText(String comment) {
        return comment != null ? comment : "";
    }

    static String getComment(JsonObject obj) {
        return getFirstFailure(obj).isJsonPrimitive()
                ? getFirstFailure(obj).getAsString()
                : null;
    }

    static String getMessage(JsonObject obj) {
        return getFirstFailure(obj).isJsonObject()
                ? getFirstFailure(obj).getAsJsonObject().get("message").getAsString()
                : null;
    }

    static private JsonObject getReason(JsonObject obj) {
        return getFirstFailure(obj).isJsonObject()
                ? getFirstFailure(obj).getAsJsonObject().get("reason").getAsJsonObject()
                : null;
    }

    static private JsonObject getData(JsonObject obj) {
        return getReason(obj) != null
                ? getReason(obj).get("data") != null
                ? getReason(obj).get("data").isJsonObject()
                ? getReason(obj).get("data").getAsJsonObject()
                : null
                : null
                : null;
    }

    static String getActual(JsonObject obj) {
        return pretty(getDataMember(obj, "actual"));
    }

    static String getExpected(JsonObject obj) {
        return pretty(getDataMember(obj, "expected"));
    }

    private static JsonElement getDataMember(JsonObject obj, String name) {
        return getData(obj) != null
                ? getData(obj).get(name)
                : null;
    }

    private static String pretty(JsonElement element) {
        return element != null
                ? element.isJsonPrimitive()
                ? element.getAsString()
                : gson.toJson(element)
                : null;
    }

    private static JsonElement getFirstFailure(JsonObject obj) {
        return obj.get("failures").getAsJsonArray().get(0);
    }

    static private String getStatus(JsonObject obj) {
        return obj.get("status").getAsString();
    }

    static Path toPath(JsonObject element) {
        return LabelUtils.toPath(StreamSupport.stream(element.get("labels").getAsJsonArray().spliterator(), false)
                .map(JsonElement::getAsString)
                .collect(Collectors.toList())
        );
    }

    static Stream<Path> closeSuitePaths(Path from, Path to) {
        Path commonParent = commonParent(from, to);
        return subParents(from, commonParent);
    }

    static Stream<Path> openSuitePaths(Path from, Path to) {
        Path commonParent = commonParent(from, to);
        List<Path> parents = subParents(to, commonParent).collect(Collectors.toList());
        Collections.reverse(parents);
        return parents.stream();
    }
}
