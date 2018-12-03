package org.frawa.elmtest.core;

import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.ParsingTestCase;
import org.elm.lang.core.parser.ElmParserDefinition;
import org.elm.lang.core.psi.ElmAtomTag;
import org.elm.lang.core.psi.ElmOperandTag;
import org.elm.lang.core.psi.ElmTypes;
import org.elm.lang.core.psi.elements.ElmFunctionCallExpr;
import org.elm.lang.core.psi.elements.ElmStringConstantExpr;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DiscoverElmPluginTest extends ParsingTestCase {

    public DiscoverElmPluginTest() {
        super("discoverElmPlugin", "elm", new ElmParserDefinition());
    }

    public void testFirst() {
        // see file resources/discoverElmPlugin/First
        doTest(false);

        List<String> result = functionCalls(myFile, "describe")
                .map(call -> stringConstant(firstArgument(call)))
                .collect(Collectors.toList());
        assertEquals(Arrays.asList("describe1"), result);

        List<String> result2 = functionCalls(myFile, "test")
                .map(call -> stringConstant(firstArgument(call)))
                .collect(Collectors.toList());
        assertEquals(Arrays.asList("test1", "test2"), result2);

        List<String> result3 = functionCalls(myFile, "describe")
                .filter(firstArgumentIsString("describe1"))
                .flatMap(call -> functionCalls(secondArgument(call), "test"))
                .filter(firstArgumentIsString("test2"))
                .map(call -> stringConstant(firstArgument(call)))
                .collect(Collectors.toList());
        assertEquals(Arrays.asList("test2"), result3);

        List<String> result4 = allSuites("describe1")
                .apply(myFile)
                .flatMap(allTests("test2"))
                .map(firstOperand())
                .map(literalString())
                .collect(Collectors.toList());
        assertEquals(Arrays.asList("test2"), result4);
    }

    private static Function<PsiElement, Stream<ElmOperandTag>> allSuites(String label) {
        return psi -> functionCalls(psi, "describe")
                .filter(firstArgumentIsString(label))
                .map(secondOperand());
    }

    private static Function<PsiElement, Stream<ElmFunctionCallExpr>> allTests(String label) {
        return psi -> functionCalls(psi, "test")
                .filter(firstArgumentIsString(label));
    }

    private static Predicate<ElmFunctionCallExpr> firstArgumentIsString(String value) {
        return call -> firstOperand()
                .andThen(literalString())
                .andThen(s -> s.equals(value))
                .apply(call);
    }

    private static ElmOperandTag firstArgument(ElmFunctionCallExpr call) {
        return call.getArguments().iterator().next();
    }

    private static Function<ElmFunctionCallExpr, ElmOperandTag> firstOperand() {
        return call -> call.getArguments().iterator().next();
    }

    private static ElmAtomTag secondArgument(ElmFunctionCallExpr call) {
        Iterator<ElmAtomTag> iterator = call.getArguments().iterator();
        iterator.next();
        return iterator.next();
    }


    private static Function<ElmFunctionCallExpr, ElmAtomTag> secondOperand() {
        return call -> {
            Iterator<ElmAtomTag> iterator = call.getArguments().iterator();
            iterator.next();
            return iterator.next();
        };
    }

    private static Stream<ElmFunctionCallExpr> functionCalls(PsiElement parent, String targetName) {
        return PsiTreeUtil.findChildrenOfType(parent, ElmFunctionCallExpr.class)
                .stream()
                .filter(call -> call.getTarget().getText().equals(targetName));
    }

    private static String stringConstant(ElmOperandTag op) {
        if (op instanceof ElmStringConstantExpr) {
            return PsiTreeUtil.findSiblingForward(op.getFirstChild(), ElmTypes.REGULAR_STRING_PART, null).getText();
        }
        return PsiTreeUtil.findChildOfType(op, ElmStringConstantExpr.class).getText();
    }

    private static Function<ElmOperandTag, String> literalString() {
        return op -> stringConstant(op);
    }


// TODO move to a real class

// TODO Done


    @Override
    protected String getTestDataPath() {
        return "src/test/resources";
    }

    @Override
    protected boolean skipSpaces() {
        return false;
    }

    @Override
    protected boolean includeRanges() {
        return true;
    }
}
