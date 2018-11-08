package org.frawa.elmtest.core;

import com.intellij.openapi.util.Pair;
import org.junit.Test;

import java.nio.file.Path;
import java.util.Arrays;

import static org.frawa.elmtest.core.LabelUtils.*;
import static org.junit.Assert.assertEquals;

public class LabelUtilsTest {

    @Test
    public void diffPathTest() {
        Path from = toPath(Arrays.asList("Module", "suite", "test"));
        Path to = toPath(Arrays.asList("Module", "suite", "test2"));
        Path diff = diffPaths(from, to);
        assertEquals("test2", diff.toString());
    }

    @Test
    public void diffPathSuite() {
        Path from = toPath(Arrays.asList("Module", "suite", "test"));
        Path to = toPath(Arrays.asList("Module", "suite2", "test2"));
        Path diff = diffPaths(from, to);
        assertEquals("../suite2/test2", diff.toString());
    }

    @Test
    public void locationUrl() {
        String url = toLocationUrl("toto", toPath(Arrays.asList("Module", "suite", "test")));
        assertEquals("toto://Module/suite/test", url);
    }

    @Test
    public void locationUrlWithSlash() {
        String url = toLocationUrl("toto", toPath(Arrays.asList("Nested.Module", "suite", "test / stuff")));
        assertEquals("toto://Nested.Module/suite/test+%2F+stuff", url);
    }

    @Test
    public void useLocationUrl() {
        String url = toLocationUrl("toto", toPath(Arrays.asList("Nested.Module", "suite", "test")));
        String urlPath = url.substring(url.indexOf("://") + 3);

        Pair<String, String> pair = fromLocationUrlPath(urlPath);
        assertEquals("tests/Nested/Module.elm", pair.first);
        assertEquals("test", pair.second);
    }

    @Test
    public void useLocationUrlWithSlash() {
        String url = toLocationUrl("toto", toPath(Arrays.asList("Module", "suite", "test / stuff")));
        String urlPath = url.substring(url.indexOf("://") + 3);

        Pair<String, String> pair = fromLocationUrlPath(urlPath);
        assertEquals("tests/Module.elm", pair.first);
        assertEquals("test / stuff", pair.second);
    }
}