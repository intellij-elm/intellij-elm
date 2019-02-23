package org.frawa.elmtest.core.json;

import com.google.gson.JsonElement;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Problem {
    public String title;
    public Region region;
    public List<JsonElement> message;

    public String getTextMessage() {
        Predicate<JsonElement> hasText = element ->
                element.isJsonPrimitive()
                        || (element.isJsonObject() && element.getAsJsonObject().has("string"));

        Function<JsonElement, String> toText = element ->
                element.isJsonPrimitive()
                        ? element.getAsString()
                        : element.getAsJsonObject().get("string").getAsString();

        return message.stream()
                .filter(hasText)
                .map(toText)
                .collect(Collectors.joining());
    }


}
