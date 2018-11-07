package org.frawa.elmtest.run;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.intellij.execution.testframework.sm.runner.events.*;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

class ElmTestJsonProcessor {
    final static Path EMPTY_PATH = Paths.get("");

    private Gson gson = new Gson();

    private Path currentPath = EMPTY_PATH;

    List<TreeNodeEvent> accept(String text) {
        try {
            JsonObject obj = gson.fromJson(text, JsonObject.class);

            String event = obj.get("event").getAsString();

            if ("runStart".equals(event)) {
                currentPath = EMPTY_PATH;
                return Collections.emptyList();
            } else if ("runComplete".equals(event)) {
                Path diff = diffPaths(currentPath, EMPTY_PATH);
                List<TreeNodeEvent> closeAll = closeSuiteNames(diff, currentPath).stream()
                        .map(toFinishSuiteEvent)
                        .collect(Collectors.toList());
                currentPath = EMPTY_PATH;
                return closeAll;
            } else if (!"testCompleted".equals(event)) {
                return Collections.emptyList();
            }

            Path path = toPath(obj);
            if ("todo".equals(getStatus(obj))) {
                path = path.resolve("todo");
            }

            Path diff = diffPaths(currentPath, path);

            Stream<TreeNodeEvent> suiteEvents = diff.toString().isEmpty()
                    ? Stream.empty()
                    : Stream.concat(
                    closeSuiteNames(diff, currentPath).stream().map(toFinishSuiteEvent),
                    openSuiteNames(diff).stream().map(toStartSuiteEvent)
            );

            List<TreeNodeEvent> result = Stream.concat(
                    suiteEvents,
                    testEvents(decodeLabel(path.getFileName()), obj)
            ).collect(Collectors.toList());

            currentPath = path;
            return result;
        } catch (JsonSyntaxException e) {
            return null;
        }
    }

    private Function<String, TreeNodeEvent> toStartSuiteEvent = name -> new TestSuiteStartedEvent(name, null);
    private Function<String, TreeNodeEvent> toFinishSuiteEvent = TestSuiteFinishedEvent::new;

    private Stream<TreeNodeEvent> testEvents(String name, JsonObject obj) {
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
                    new TestIgnoredEvent(name, comment != null ? comment : "", null)
            );
        }
        String message = getMessage(obj);
        String actual = getActual(obj);
        String expected = getExpected(obj);

        return Stream.of(
                new TestStartedEvent(name, null),
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

    static private String encodeLabel(String label) {
        try {
            return URLEncoder.encode(label, "utf8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    static private String decodeLabel(Path encoded) {
        try {
            return URLDecoder.decode(encoded.toString(), "utf8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    static Path toPath(JsonObject element) {
        return toPath(StreamSupport.stream(element.get("labels").getAsJsonArray().spliterator(), false)
                .map(JsonElement::getAsString)
                .collect(Collectors.toList())
        );
    }

    static Path toPath(List<String> labels) {
        List<String> encoded = labels.stream()
                .map(ElmTestJsonProcessor::encodeLabel)
                .collect(Collectors.toList());
        if (encoded.isEmpty()) {
            return EMPTY_PATH;
        }
        return Paths.get(
                encoded.get(0),
                encoded.subList(1, encoded.size()).toArray(new String[0])
        );
    }

    static Path diffPaths(Path from, Path to) {
        return from.getParent() != null
                ? from.getParent().relativize(to)
                : to;
    }

    static List<String> closeSuiteNames(Path diff, Path from) {
        List<String> result = new ArrayList<>();
        int dirIndex = from.getNameCount() - 2;
        for (int i = 0; i < diff.getNameCount(); i++) {
            if (diff.getName(i).toString().equals("..")) {
                result.add(decodeLabel(from.getName(dirIndex - i)));
            } else {
                break;
            }
        }
        return result;
    }

    static List<String> openSuiteNames(Path diff) {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < diff.getNameCount() - 1; i++) {
            String name = decodeLabel(diff.getName(i));
            if (!name.equals("..")) {
                result.add(name);
            }
        }
        return result;
    }
}
