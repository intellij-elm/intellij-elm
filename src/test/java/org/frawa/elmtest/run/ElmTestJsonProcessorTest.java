package org.frawa.elmtest.run;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.intellij.execution.testframework.sm.runner.events.TreeNodeEvent;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class ElmTestJsonProcessorTest {

    private ElmTestJsonProcessor processor = new ElmTestJsonProcessor();

    @Test
    public void junk() {
        List<TreeNodeEvent> list = processor.accept("junk");
        assertNull(list);
    }

    @Test
    public void runStart() {
        List<TreeNodeEvent> list = processor.accept("{\"event\":\"runStart\",\"testCount\":\"9\",\"fuzzRuns\":\"100\",\"paths\":[],\"initialSeed\":\"1448022641\"}\n");
        assertTrue(list.isEmpty());
    }

    @Test
    public void runComplete() {
        List<TreeNodeEvent> list = processor.accept("{\"event\":\"runComplete\",\"passed\":\"8\",\"failed\":\"1\",\"duration\":\"353\",\"autoFail\":null}\n");
        assertTrue(list.isEmpty());
    }

    @Test
    public void testCompleted() {
        List<TreeNodeEvent> list = processor.accept("{\"event\":\"testCompleted\",\"status\":\"pass\",\"labels\":[\"Module\",\"test\"],\"failures\":[],\"duration\":\"1\"}");
        assertEquals(2, list.size());
    }

    @Test
    public void toPath() {
        JsonObject obj = new Gson().fromJson("{\"labels\":[\"Module\",\"test\"]}", JsonObject.class);
        Path path = ElmTestJsonProcessor.toPath(obj);
        assertEquals(2, path.getNameCount());
        assertEquals("Module", path.getName(0).toString());
        assertEquals("test", path.getName(1).toString());
    }

    @Test
    public void diffPathTest() {
        Path from = Paths.get("Module", "suite", "test");
        Path to = Paths.get("Module", "suite", "test2");
        Path diff = ElmTestJsonProcessor.diffPaths(from, to);
        assertEquals("test2", diff.toString());
    }

    @Test
    public void diffPathSuite() {
        Path from = Paths.get("Module", "suite", "test");
        Path to = Paths.get("Module", "suite2", "test2");
        Path diff = ElmTestJsonProcessor.diffPaths(from, to);
        assertEquals("../suite2/test2", diff.toString());
    }

    @Test
    public void closeNoSuites() {
        Path from = Paths.get("Module", "suite", "test");
        Path to = Paths.get("Module", "suite", "test2");
        Path diff = ElmTestJsonProcessor.diffPaths(from, to);
        List<String> names = ElmTestJsonProcessor.closeSuiteNames(diff, from);
        assertEquals(Arrays.asList(), names);
    }

    @Test
    public void closeOneSuite() {
        Path from = Paths.get("Module", "suite", "test");
        Path to = Paths.get("Module", "suite2", "test2");
        Path diff = ElmTestJsonProcessor.diffPaths(from, to);
        List<String> names = ElmTestJsonProcessor.closeSuiteNames(diff, from);
        assertEquals(Arrays.asList("suite"), names);
    }

    @Test
    public void closeTwoSuite() {
        Path from = Paths.get("Module", "suite", "deep", "test");
        Path to = Paths.get("Module", "suite2", "test2");
        Path diff = ElmTestJsonProcessor.diffPaths(from, to);
        List<String> names = ElmTestJsonProcessor.closeSuiteNames(diff, from);
        assertEquals(Arrays.asList("deep", "suite"), names);
    }

    @Test
    public void openNoSuites() {
        Path from = Paths.get("Module", "suite", "test");
        Path to = Paths.get("Module", "suite", "test2");
        Path diff = ElmTestJsonProcessor.diffPaths(from, to);
        List<String> names = ElmTestJsonProcessor.openSuiteNames(diff);
        assertEquals(Arrays.asList(), names);
    }

    @Test
    public void openOneSuite() {
        Path from = Paths.get("Module", "suite", "test");
        Path to = Paths.get("Module", "suite2", "test2");
        Path diff = ElmTestJsonProcessor.diffPaths(from, to);
        List<String> names = ElmTestJsonProcessor.openSuiteNames(diff);
        assertEquals(Arrays.asList("suite2"), names);
    }

    @Test
    public void openTwoSuites() {
        Path from = Paths.get("Module", "suite", "test");
        Path to = Paths.get("Module", "suite2", "deep2", "test2");
        Path diff = ElmTestJsonProcessor.diffPaths(from, to);
        List<String> names = ElmTestJsonProcessor.openSuiteNames(diff);
        assertEquals(Arrays.asList("suite2", "deep2"), names);
    }


    @Test
    public void todo() {
        String text = "{\"event\":\"testCompleted\",\"status\":\"todo\",\"labels\":[\"Exploratory\",\"describe\"],\"failures\":[\"TODO comment\"],\"duration\":\"2\"}";
        JsonObject obj = getObject(text);
        assertEquals("TODO comment", ElmTestJsonProcessor.getComment(obj));
        assertEquals(null, ElmTestJsonProcessor.getMessage(obj));
        assertEquals(null, ElmTestJsonProcessor.getExpected(obj));
        assertEquals(null, ElmTestJsonProcessor.getActual(obj));
    }

    @Test
    public void fail() {
        String text = "{\"event\":\"testCompleted\",\"status\":\"fail\",\"labels\":[\"Exploratory\",\"describe\",\"fail\"],\"failures\":[{\"given\":null,\"message\":\"boom\",\"reason\":{\"type\":\"custom\",\"data\":\"boom\"}}],\"duration\":\"1\"}";
        JsonObject obj = getObject(text);
        assertEquals(null, ElmTestJsonProcessor.getComment(obj));
        assertEquals("boom", ElmTestJsonProcessor.getMessage(obj));
        assertEquals(null, ElmTestJsonProcessor.getExpected(obj));
        assertEquals(null, ElmTestJsonProcessor.getActual(obj));
    }

    @Test
    public void failEqual() {
        String text = "{\"event\":\"testCompleted\",\"status\":\"fail\",\"labels\":[\"Exploratory\",\"describe\",\"duplicate nested\",\"ok1\"],\"failures\":[{\"given\":null,\"message\":\"Expect.equal\",\"reason\":{\"type\":\"custom\",\"data\":{\"expected\":\"\\\"value\\\"\",\"actual\":\"\\\"value2\\\"\",\"comparison\":\"Expect.equal\"}}}],\"duration\":\"2\"}";
        JsonObject obj = getObject(text);
        assertEquals(null, ElmTestJsonProcessor.getComment(obj));
        assertEquals("Expect.equal", ElmTestJsonProcessor.getMessage(obj));
        assertEquals("\"value\"", ElmTestJsonProcessor.getExpected(obj));
        assertEquals("\"value2\"", ElmTestJsonProcessor.getActual(obj));
    }

    @Test
    public void failHtml() {
        String text = "{\"event\":\"testCompleted\",\"status\":\"fail\",\"labels\":[\"Exploratory\",\"Html tests 1\",\"... fails\"],\"failures\":[{\"given\":null,\"message\":\"▼ Query.fromHtml\\n\\n    <div class=\\\"container\\\">\\n        <button>\\n            I'm a button!\\n        </button>\\n    </div>\\n\\n\\n▼ Query.find [ tag \\\"button1\\\" ]\\n\\n0 matches found for this query.\\n\\n\\n✗ Query.find always expects to find 1 element, but it found 0 instead.\",\"reason\":{\"type\":\"custom\",\"data\":\"▼ Query.fromHtml\\n\\n    <div class=\\\"container\\\">\\n        <button>\\n            I'm a button!\\n        </button>\\n    </div>\\n\\n\\n▼ Query.find [ tag \\\"button1\\\" ]\\n\\n0 matches found for this query.\\n\\n\\n✗ Query.find always expects to find 1 element, but it found 0 instead.\"}}],\"duration\":\"15\"}";
        JsonObject obj = getObject(text);
        assertEquals(null, ElmTestJsonProcessor.getComment(obj));
        assertTrue(ElmTestJsonProcessor.getMessage(obj).contains("I'm a button!"));
        assertEquals(null, ElmTestJsonProcessor.getExpected(obj));
        assertEquals(null, ElmTestJsonProcessor.getActual(obj));
    }

    private JsonObject getObject(String text) {
        return new Gson().fromJson(text, JsonObject.class);
    }
}
