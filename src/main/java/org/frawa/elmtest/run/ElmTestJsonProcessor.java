package org.frawa.elmtest.run;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import jetbrains.buildServer.messages.serviceMessages.ServiceMessage;
import org.bouncycastle.util.Arrays;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class ElmTestJsonProcessor {
    private Gson gson = new Gson();

    private Path currentPath = null;

    public List<ServiceMessage> accept(String text) {
        try {
            JsonObject obj = gson.fromJson(text, JsonObject.class);
//            Path path = toPath(obj);
//            currentPath = path;
            return Collections.emptyList();
        } catch (JsonSyntaxException e) {
            return null;
        }
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
        return from.relativize(to);
    }
}
