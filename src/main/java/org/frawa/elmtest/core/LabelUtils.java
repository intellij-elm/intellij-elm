package org.frawa.elmtest.core;

import com.intellij.openapi.util.Pair;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class LabelUtils {
    public static final String ELM_TEST_PROTOCOL = "elmTest";

    final static Path EMPTY_PATH = Paths.get("");

    static String getModuleName(Path path) {
        return path.getName(0).toString();
    }

    static String encodeLabel(String label) {
        try {
            return URLEncoder.encode(label, "utf8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    static String decodeLabel(Path encoded) {
        try {
            return URLDecoder.decode(encoded.toString(), "utf8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public static Path toPath(List<String> labels) {
        List<String> encoded = labels.stream()
                .map(LabelUtils::encodeLabel)
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

    public static String toLocationUrl(String modelName, String label) {
        return modelName.equals(label)
                ? String.format("%s://%s", ELM_TEST_PROTOCOL, modelName)
                : String.format("%s://%s/%s", ELM_TEST_PROTOCOL, modelName, encodeLabel(label));
    }

    public static Pair<String, String> fromLocationUrlPath(String path) {
        Path path1 = Paths.get(path);
        String moduleName = getModuleName(path1);
        String moduleFile = String.format("tests/%s.elm", moduleName.replace(".", "/"));
        String label = decodeLabel(path1.getFileName());
        return new Pair<>(moduleFile, label);
    }
}
