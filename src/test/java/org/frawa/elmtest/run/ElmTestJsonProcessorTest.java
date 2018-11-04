package org.frawa.elmtest.run;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import jetbrains.buildServer.messages.serviceMessages.ServiceMessage;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.Assert.*;

public class ElmTestJsonProcessorTest {

    private ElmTestJsonProcessor processor = new ElmTestJsonProcessor();

    @Test
    public void junk() {
        List<ServiceMessage> list = processor.accept("junk");
        assertNull(list);
    }

    @Test
    public void runStart() {
        List<ServiceMessage> list = processor.accept("{\"event\":\"runStart\",\"testCount\":\"9\",\"fuzzRuns\":\"100\",\"paths\":[],\"initialSeed\":\"1448022641\"}\n");
        assertTrue(list.isEmpty());
    }

    @Test
    public void runComplete() {
        List<ServiceMessage> list = processor.accept("{\"event\":\"runComplete\",\"passed\":\"8\",\"failed\":\"1\",\"duration\":\"353\",\"autoFail\":null}\n");
        assertTrue(list.isEmpty());
    }

    @Test
    public void testCompleted() {
        List<ServiceMessage> list = processor.accept("{\"event\":\"testCompleted\",\"status\":\"pass\",\"labels\":[\"Module\",\"test\"],\"failures\":[],\"duration\":\"1\"}");
        assertEquals(2, list.size());
    }

    @Test
    public void toPath() {
        JsonObject obj = new Gson().fromJson("{\"labels\":[\"Module\",\"test\"]}", JsonObject.class);
        Path path = ElmTestJsonProcessor.toPath(obj);
        assertEquals(2,path.getNameCount());
        assertEquals("Module",path.getName(0).toString());
        assertEquals("test",path.getName(1).toString());
    }

    @Test
    public void diffPathTest() {
        Path from = Paths.get("Module", "suite", "test");
        Path to = Paths.get("Module", "suite", "test2");
        Path diff = ElmTestJsonProcessor.diffPaths(from,to);
        assertEquals(2,diff.getNameCount());
        assertEquals("../test2",diff.toString());
    }

    @Test
    public void diffPathSuite() {
        Path from = Paths.get("Module", "suite", "test");
        Path to = Paths.get("Module", "suite2", "test2");
        Path diff = ElmTestJsonProcessor.diffPaths(from,to);
        assertEquals(4,diff.getNameCount());
        assertEquals("../../suite2/test2",diff.toString());
    }
}
