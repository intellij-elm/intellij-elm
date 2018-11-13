package org.frawa.elmtest.core;

import com.intellij.psi.PsiElement;
import com.intellij.testFramework.ParsingTestCase;
import org.elm.lang.core.parser.ElmParserDefinition;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;

import static org.frawa.elmtest.core.ElmPluginHelper.findPsiElement;

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

    public void testNavigation() {
        doTest(false);

        assertSuite(27, "suite1");
        assertTest(55, "suite1", "test1");

        assertTest(137, "test1");

        assertSuite(207, "suite2");
        assertTest(235, "suite2", "test1");
        assertSuite(291, "suite2", "nested1");
        assertTest(324, "suite2", "nested1", "test1");
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

    private void assertSuite(int offset, String... labels) {
        Path path = LabelUtils.toPath(Arrays.asList(labels));
        Optional<? extends PsiElement> element = findPsiElement(true, path.toString(), myFile);
        assertTrue(element.isPresent());

        String expected = String.format("describe \"%s\"", labels[labels.length - 1]);
        assertEquals(expected, firstLine(text(element)));
        assertEquals(offset, element.get().getNode().getStartOffset());
    }

    private void assertTest(int offset, String... labels) {
        Path path = LabelUtils.toPath(Arrays.asList(labels));
        Optional<? extends PsiElement> element = findPsiElement(false, path.toString(), myFile);
        assertTrue(element.isPresent());

        String expected = String.format("test \"%s\"", labels[labels.length - 1]);
        assertEquals(expected, text(element));
        assertEquals(offset, element.get().getNode().getStartOffset());
    }

    @NotNull
    private String text(Optional<? extends PsiElement> element) {
        return element.get().getText().trim();
    }

    private String firstLine(String text) {
        return text.split("\n")[0];
    }

}