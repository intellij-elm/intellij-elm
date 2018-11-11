package org.frawa.elmtest.core;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.intellij.execution.testframework.sm.runner.events.*;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.frawa.elmtest.core.LabelUtils.*;

public class ElmTestJsonProcessor {

    private Gson gson = new Gson();

    private Path currentPath = LabelUtils.EMPTY_PATH;

    public List<TreeNodeEvent> accept(String text) {
        try {
            JsonObject obj = gson.fromJson(text, JsonObject.class);

            String event = obj.get("event").getAsString();

            Function<Path, TreeNodeEvent> toFinishSuiteEvent = path1 ->
                    new TestSuiteFinishedEvent(getName(path1));

            if ("runStart".equals(event)) {
                currentPath = LabelUtils.EMPTY_PATH;
                return Collections.emptyList();
            } else if ("runComplete".equals(event)) {
                List<TreeNodeEvent> closeAll = closeSuitePaths(currentPath, EMPTY_PATH)
                        .map(toFinishSuiteEvent)
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

            Function<Path, TreeNodeEvent> toStartSuiteEvent = path1 ->
                    new TestSuiteStartedEvent(getName(path1), toLocationUrl(path1));

            List<TreeNodeEvent> result = Stream.of(
                    closeSuitePaths(currentPath, path).map(toFinishSuiteEvent),
                    openSuitePaths(currentPath, path).map(toStartSuiteEvent),
                    testEvents(path, obj)
            )
//                    .flatMap(Functions.identity())
                    .flatMap(s -> s)
                    .collect(Collectors.toList());

            currentPath = path;
            return result;
        } catch (JsonSyntaxException e) {
            return null;
        }
    }

    private Stream<TreeNodeEvent> testEvents(Path path, JsonObject obj) {
        String name = getName(path);
        String status = getStatus(obj);
        if ("pass".equals(status)) {
            long duration = Long.parseLong(obj.get("duration").getAsString());
            return Stream.of(
                    new TestStartedEvent(name, toLocationUrl(path)),
                    new TestFinishedEvent(name, duration)
            );
        } else if ("todo".equals(status)) {
            String comment = getComment(obj);
            return Stream.of(
                    new TestIgnoredEvent(name, comment != null ? comment : "", null)
            );
        }
        String message = getMessage(obj);
        String actual = getActual(obj);
        String expected = getExpected(obj);

        return Stream.of(
                new TestStartedEvent(name, toLocationUrl(path)),
                new TestFailedEvent(name, message != null ? message : "", null, false, actual, expected)
        );
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
        return getData(obj) != null
                ? getData(obj).get("actual").getAsString()
                : null;
    }

    static String getExpected(JsonObject obj) {
        return getData(obj) != null
                ? getData(obj).get("expected").getAsString()
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
