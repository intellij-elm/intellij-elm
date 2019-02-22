package org.frawa.elmtest.core;

import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.FileUtil;

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
    public static final String DESCRIBE_PROTOCOL = ELM_TEST_PROTOCOL + "Describe";
    private static final String TEST_PROTOCOL = ELM_TEST_PROTOCOL + "Test";
    public static final String ERROR_PROTOCOL = ELM_TEST_PROTOCOL + "Error";

    final static Path EMPTY_PATH = Paths.get("");

    private static String getModuleName(Path path) {
        return pathString(path.getName(0));
    }

    private static String encodeLabel(String label) {
        try {
            return URLEncoder.encode(label, "utf8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    static String decodeLabel(Path encoded) {
        try {
            return URLDecoder.decode(pathString(encoded), "utf8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    static Path toPath(List<String> labels) {
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

    static String pathString(Path path) {
        return FileUtil.toSystemIndependentName(path.toString());
    }

    public static String getName(Path path) {
        return decodeLabel(path.getFileName());
    }

    static String toSuiteLocationUrl(Path path) {
        return toLocationUrl(DESCRIBE_PROTOCOL, path);
    }

    static String toTestLocationUrl(Path path) {
        return toLocationUrl(TEST_PROTOCOL, path);
    }

    private static String toLocationUrl(String protocol, Path path) {
        return String.format("%s://%s", protocol, pathString(path));
    }

    public static Pair<String, String> fromLocationUrlPath(String path) {
        Path path1 = Paths.get(path);
        String moduleName = getModuleName(path1);
        String moduleFile = String.format("tests/%s.elm", moduleName.replace(".", "/"));
        String label = path1.getNameCount() > 1 ? decodeLabel(path1.subpath(1, path1.getNameCount())) : "";
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

    static String toErrorLocationUrl(String path, int line, int column) {
        return String.format("%s://%s::%d::%d", ERROR_PROTOCOL, path, line, column);
    }

    public static Pair<String, Pair<Integer, Integer>> fromErrorLocationUrlPath(String spec) {
        String[] parts = spec.split("::");
        String file = parts[0];
        int line = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
        int column = parts.length > 2 ? Integer.parseInt(parts[2]) : 1;
        return new Pair<>(file, new Pair<>(line, column));
    }

}
