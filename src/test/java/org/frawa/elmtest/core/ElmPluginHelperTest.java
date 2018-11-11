package org.frawa.elmtest.core;

import com.intellij.psi.PsiElement;
import com.intellij.testFramework.ParsingTestCase;
import org.elm.lang.core.parser.ElmParserDefinition;

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

    public void testFirst() {
        // see file resources/elmPluginHelper/First
        doTest(false);

        Optional<? extends PsiElement> element1 = findPsiElement(true, "describe1", myFile);
        assertTrue(element1.isPresent());
        assertEquals("describe \"describe1\"", element1.get().getText().split("\n")[0]);
        assertEquals(25, element1.get().getNode().getStartOffset());

        Optional<? extends PsiElement> element2 = findPsiElement(false, "describe1/test1", myFile);
        assertTrue(element2.isPresent());
        assertEquals("test \"test1\"", element2.get().getText().trim());
        assertEquals(56, element2.get().getNode().getStartOffset());
    }
}