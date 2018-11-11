package org.frawa.elmtest.core;

import com.intellij.openapi.util.Pair;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    public static String getName(Path path) {
        return decodeLabel(path.getFileName());
    }

    public static String toLocationUrl(Path path) {
        return String.format("%s://%s", ELM_TEST_PROTOCOL, path.toString());
    }

    public static Pair<String, String> fromLocationUrlPath(String path) {
        Path path1 = Paths.get(path);
        String moduleName = getModuleName(path1);
        String moduleFile = String.format("tests/%s.elm", moduleName.replace(".", "/"));
        String label = decodeLabel(path1.getFileName());
        return new Pair<>(moduleFile, label);
    }

    static Path commonParent(Path path1, Path path2) {
        if (path1 == null) {
            return EMPTY_PATH;
        }
        if (path1.getNameCount() > path2.getNameCount()) {
            return commonParent(path2, path1);
        }
        if (path2.startsWith(path1)) {
            return path1;
        } else {
            return commonParent(path1.getParent(), path2);
        }
    }

    static Stream<Path> subParents(Path path, Path excludeParent) {
        if (excludeParent == EMPTY_PATH) {
            // TODO remove duplication with below
            List<Path> result = new ArrayList<>();
            for (Path current = path.getParent(); current != null; current = current.getParent()) {
                result.add(current);
            }
            return result.stream();
        }

        if (!path.startsWith(excludeParent)) {
            throw new IllegalStateException("not parent");
        }

        if (path == EMPTY_PATH) {
            return Stream.empty();
        }

        List<Path> result = new ArrayList<>();
        for (Path current = path.getParent(); !current.equals(excludeParent); current = current.getParent()) {
            result.add(current);
        }
        return result.stream();

        // JSK 9
//        return Stream.iterate(path, current -> current != null ? current.getParent() : null)
//                .takeWile(current -> !current.equals(excludeParent));
    }
}
