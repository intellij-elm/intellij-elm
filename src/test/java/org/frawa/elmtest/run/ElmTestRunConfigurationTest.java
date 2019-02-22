package org.frawa.elmtest.run;

import org.jdom.Element;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ElmTestRunConfigurationTest {

    @Test
    public void writeOptions() {
        Element root = new Element("ROOT");

        ElmTestRunConfiguration.Options options = new ElmTestRunConfiguration.Options();
        options.elmFolder = "folder";

        ElmTestRunConfiguration.writeOptions(options, root);

        assertEquals(1, root.getChildren().size());
        assertEquals(ElmTestRunConfiguration.class.getSimpleName(), root.getChildren().get(0).getName());
        assertEquals(1, root.getChildren().get(0).getAttributes().size());
        assertEquals("elm-folder", root.getChildren().get(0).getAttributes().get(0).getName());
        assertEquals("folder", root.getChildren().get(0).getAttributes().get(0).getValue());
    }

    @Test
    public void roundTrip() {
        Element root = new Element("ROOT");

        ElmTestRunConfiguration.Options options = new ElmTestRunConfiguration.Options();
        options.elmFolder = "folder";

        ElmTestRunConfiguration.writeOptions(options, root);
        ElmTestRunConfiguration.Options options2 = ElmTestRunConfiguration.readOptions(root);

        assertEquals(options.elmFolder, options2.elmFolder);
    }

}