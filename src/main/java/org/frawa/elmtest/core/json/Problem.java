package org.frawa.elmtest.core.json;

import com.google.gson.JsonElement;

import java.util.List;
import java.util.stream.Collectors;

public class Problem {
    public String title;
    public Region region;
    public List<JsonElement> message;

    public String getTextMessage() {
        return message.stream()
                .filter(JsonElement::isJsonPrimitive)
                .map(JsonElement::getAsString)
                .collect(Collectors.joining("\n"));
    }
}
