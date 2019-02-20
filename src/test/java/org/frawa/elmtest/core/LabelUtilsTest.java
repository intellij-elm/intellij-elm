package org.frawa.elmtest.core;

import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFileManager;
import org.junit.Test;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.frawa.elmtest.core.LabelUtils.*;
import static org.junit.Assert.assertEquals;

public class LabelUtilsTest {

    @Test
    public void locationUrl() {
        String url = toTestLocationUrl(toPath(Arrays.asList("Module", "test")));
        assertEquals("elmTestTest://Module/test", url);
    }

    @Test
    public void suiteLocationUrl() {
        String url = toSuiteLocationUrl(toPath(Arrays.asList("Module", "suite")));
        assertEquals("elmTestDescribe://Module/suite", url);
    }

    @Test
    public void locationUrlWithSlash() {
        String url = toTestLocationUrl(toPath(Arrays.asList("Nested.Module", "test / stuff")));
        assertEquals("elmTestTest://Nested.Module/test+%2F+stuff", url);
    }

    @Test
    public void useLocationUrl() {
        String url = toTestLocationUrl(toPath(Arrays.asList("Nested.Module", "test")));
        String urlPath = url.substring(url.indexOf("://") + 3);

        Pair<String, String> pair = fromLocationUrlPath(urlPath);
        assertEquals("tests/Nested/Module.elm", pair.first);
        assertEquals("test", pair.second);
    }

    @Test
    public void useNestedLocationUrl() {
        String url = toTestLocationUrl(toPath(Arrays.asList("Nested.Module", "suite", "test")));
        String urlPath = url.substring(url.indexOf("://") + 3);

        Pair<String, String> pair = fromLocationUrlPath(urlPath);
        assertEquals("tests/Nested/Module.elm", pair.first);
        assertEquals("suite/test", pair.second);
    }

    @Test
    public void useLocationUrlWithSlash() {
        String url = toTestLocationUrl(toPath(Arrays.asList("Module", "test / stuff")));
        String urlPath = url.substring(url.indexOf("://") + 3);

        Pair<String, String> pair = fromLocationUrlPath(urlPath);
        assertEquals("tests/Module.elm", pair.first);
        assertEquals("test / stuff", pair.second);
    }

    @Test
    public void commonParentSameSuite() {
        Path from = toPath(Arrays.asList("Module", "suite", "test"));
        Path to = toPath(Arrays.asList("Module", "suite", "test2"));

        Path parent = commonParent(from, to);
        assertEquals("Module/suite", pathString(parent));
    }

    @Test
    public void commonParentDifferentSuite() {
        Path from = toPath(Arrays.asList("Module", "suite", "test"));
        Path to = toPath(Arrays.asList("Module", "suite2", "test2"));

        Path parent = commonParent(from, to);
        assertEquals("Module", pathString(parent));

        Path parent2 = commonParent(to, from);
        assertEquals("Module", pathString(parent2));
    }

    @Test
    public void commonParentDifferentSuite2() {
        Path from = toPath(Arrays.asList("Module", "suite", "deep", "test"));
        Path to = toPath(Arrays.asList("Module", "suite2", "test2"));

        Path parent = commonParent(from, to);
        assertEquals("Module", pathString(parent));

        Path parent2 = commonParent(to, from);
        assertEquals("Module", pathString(parent2));
    }

    @Test
    public void commonParentNoParent() {
        Path from = toPath(Arrays.asList("Module", "suite", "test"));
        Path to = toPath(Arrays.asList("Module2", "suite2", "test2"));

        Path parent = commonParent(from, to);
        assertEquals("", pathString(parent));
    }

    @Test
    public void parentPaths() {
        Path path = toPath(Arrays.asList("Module", "suite", "test"));
        Path parent = toPath(Collections.singletonList("Module"));

        List<String> parents = subParents(path, parent)
                .map(LabelUtils::pathString)
                .collect(Collectors.toList());
        assertEquals(Collections.singletonList("Module/suite"), parents);
    }

    @Test
    public void errorLocationUrl() {
        String url = toErrorLocationUrl("my/path/file", 1313, 13);
        assertEquals("elmTestError://my/path/file::1313::13", url);

        String path = VirtualFileManager.extractPath(url);
        Pair<String, Pair<Integer, Integer>> pair = fromErrorLocationUrlPath(path);

        String file = pair.first;
        int line = pair.second.first;
        int column = pair.second.second;

        assertEquals("my/path/file", file);
        assertEquals(1313, line);
        assertEquals(13, column);
    }

}