package org.frawa.elmtest.run;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.intellij.execution.testframework.sm.runner.events.TreeNodeEvent;
import jetbrains.buildServer.messages.serviceMessages.ServiceMessage;
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

}
