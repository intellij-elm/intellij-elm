package org.frawa.elmtest.core;

import com.intellij.psi.PsiElement;
import com.intellij.testFramework.ParsingTestCase;
import org.elm.ide.test.core.LabelUtils;
import org.elm.lang.core.parser.ElmParserDefinition;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Arrays;

import static org.frawa.elmtest.core.ElmPluginHelper.getPsiElement;

public class ElmPluginHelperTest extends ParsingTestCase {

    public ElmPluginHelperTest() {
        super("elmPluginHelper", "elm", new ElmParserDefinition());
    }

    @Override
    protected String getTestDataPath() {
        return "src/test/resources";
    }

    @NotNull
    @Override
    // see file resources/elmPluginHelper/Navigation
    protected String getTestName(boolean lowercaseFirstLetter) {
        return "Navigation";
    }

    public void testTopLevelSuite() {
        doTest(false);
        assertSuite(27, "suite1");
    }

    public void testTestInSuite() {
        doTest(false);
        assertTest(55, "suite1", "test1");
    }

    public void testTopLevelTest() {
        doTest(false);
        assertTest(137, "test1");
    }

    public void testNestedSuitesAndTests() {
        doTest(false);
        assertSuite(207, "suite2");
        assertTest(235, "suite2", "test1");
        assertSuite(291, "suite2", "nested1");
        assertTest(324, "suite2", "nested1", "test1");
    }

    public void testMissingTopLevelSuite() {
        doTest(false);
        assertMissing("suiteMissing");
    }

    public void testMissingTopLevelTest() {
        doTest(false);
        assertMissing("testMissing");
    }

    public void testMissingNestedSuitesAndTests() {
        doTest(false);
        assertSuite(207, "suite2");
        assertMissing("suite2", "testMissing");
        assertFallback("suite2", "suite2", "nestedMissing");
        assertFallback("nested1", "suite2", "nested1", "testMissing");
    }

    private void assertSuite(int offset, String... labels) {
        Path path = LabelUtils.INSTANCE.toPath(Arrays.asList(labels));
        PsiElement element = getPsiElement(true, path.toString(), myFile);

        String expected = String.format("describe \"%s\"", labels[labels.length - 1]);
        assertEquals(expected, firstLine(text(element)));
        assertEquals(offset, element.getNode().getStartOffset());
    }

    private void assertTest(int offset, String... labels) {
        Path path = LabelUtils.INSTANCE.toPath(Arrays.asList(labels));
        PsiElement element = getPsiElement(false, path.toString(), myFile);

        String expected = String.format("test \"%s\"", labels[labels.length - 1]);
        assertEquals(expected, text(element));
        assertEquals(offset, element.getNode().getStartOffset());
    }

    private void assertMissing(String... labels) {
        Path path = LabelUtils.INSTANCE.toPath(Arrays.asList(labels));
        PsiElement element = getPsiElement(false, path.toString(), myFile);
        assertSame(myFile, element);
    }

    private void assertFallback(String fallback, String... labels) {
        Path path = LabelUtils.INSTANCE.toPath(Arrays.asList(labels));
        PsiElement element = getPsiElement(true, path.toString(), myFile);

        String expected = String.format("describe \"%s\"", fallback);
        assertEquals(expected, firstLine(text(element)));
    }

    @NotNull
    private String text(PsiElement element) {
        return element.getText().trim();
    }

    private String firstLine(String text) {
        return text.split("\n")[0];
    }

}