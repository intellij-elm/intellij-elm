package org.frawa.elmtest.run;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.intellij.execution.testframework.sm.runner.events.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class ElmTestJsonProcessor {
    private Gson gson = new Gson();

    private Path currentPath = null;

    public List<TreeNodeEvent> accept(String text) {
        try {
            JsonObject obj = gson.fromJson(text, JsonObject.class);

            if (!"testCompleted".equals(obj.get("event").getAsString())) {
                return Collections.emptyList();
            }

            Path path = toPath(obj);
            if ("todo".equals(getStatus(obj))) {
                path = path.resolve("todo");
            }
            if (currentPath == null) {
                currentPath = path;
            }
            Path diff = diffPaths(currentPath, path);

            Function<String, TreeNodeEvent> toStartSuiteEvent = name -> new TestSuiteStartedEvent(name, null);
            Function<String, TreeNodeEvent> toFinishSuiteEvent = name -> new TestSuiteFinishedEvent(name);

            Stream<TreeNodeEvent> suiteEvents = diff.toString().isEmpty()
                    ? Stream.empty()
                    : Stream.concat(
                    closeSuiteNames(diff, currentPath).stream().map(toFinishSuiteEvent),
                    openSuiteNames(diff).stream().map(toStartSuiteEvent)
            );

            List<TreeNodeEvent> result = Stream.concat(
                    suiteEvents,
                    testEvents(path.getFileName().toString(), obj)
            ).collect(Collectors.toList());

            currentPath = path;
            return result;
        } catch (JsonSyntaxException e) {
            return null;
        }
    }

    Stream<TreeNodeEvent> testEvents(String name, JsonObject obj) {


        String status = getStatus(obj);
        if ("pass".equals(status)) {
            long duration = Long.parseLong(obj.get("duration").getAsString());
            return Stream.of(
                    new TestStartedEvent(name, null),
                    new TestFinishedEvent(name, duration)
            );
        } else if ("todo".equals(status)) {
            String comment = getComment(obj);
            return Stream.of(
                    new TestIgnoredEvent(name, comment, null)
            );
        }
        String message = getMessage(obj);
        String actual = getActual(obj);
        String expected = getExpected(obj);

        return Stream.of(
                new TestStartedEvent(name, null),
                new TestFailedEvent(name, message, null, false, actual, expected)
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
                ? getReason(obj).get("data").isJsonObject()
                ? getReason(obj).get("data").getAsJsonObject()
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
        List<String> labels = StreamSupport.stream(element.get("labels").getAsJsonArray().spliterator(), false)
                .map(JsonElement::getAsString)
                .collect(Collectors.toList());
        if (labels.isEmpty()) {
            return Paths.get("");
        }
        return Paths.get(
                labels.get(0),
                labels.subList(1, labels.size()).toArray(new String[0])
        );
    }

    static Path diffPaths(Path from, Path to) {
        return from.getParent().relativize(to);
    }

    static List<String> closeSuiteNames(Path diff, Path from) {
        List<String> result = new ArrayList<>();
        int dirIndex = from.getNameCount() - 2;
        for (int i = 0; i < diff.getNameCount(); i++) {
            if (diff.getName(i).toString().equals("..")) {
                result.add(from.getName(dirIndex - i).toString());
            } else {
                break;
            }
        }
        return result;
    }

    static List<String> openSuiteNames(Path diff) {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < diff.getNameCount() - 1; i++) {
            String name = diff.getName(i).toString();
            if (name.equals("..")) {
                continue;
            } else {
                result.add(name);
            }
        }
        return result;
    }
}
