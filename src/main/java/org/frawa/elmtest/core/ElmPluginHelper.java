package org.frawa.elmtest.core;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.elm.lang.core.psi.ElmOperandTag;
import org.elm.lang.core.psi.ElmTypes;
import org.elm.lang.core.psi.elements.ElmFunctionCall;
import org.elm.lang.core.psi.elements.ElmStringConstant;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.frawa.elmtest.core.LabelUtils.decodeLabel;

public class ElmPluginHelper {

    public static Optional<? extends PsiElement> findPsiElement(boolean isDescribe, String labels, PsiFile file) {
        Path labelPath = Paths.get(labels);

        if (labelPath.getNameCount() == 0) {
            return Optional.empty();
        }

        if (isDescribe) {
            // TODO remove duplication
            Stream<ElmFunctionCall> current = allSuites(decodeLabel(labelPath.getName(0))).apply(file);
            for (int i = 1; i < labelPath.getNameCount(); i++) {
                String label = decodeLabel(labelPath.getName(i));
                current = current
                        .map(secondOperand())
                        .flatMap(allSuites(label));
            }
            return current
                    .findFirst();
        }

        if (labelPath.getNameCount() > 1) {
            // TODO remove duplication
            Stream<ElmFunctionCall> current = allSuites(decodeLabel(labelPath.getName(0))).apply(file);
            for (int i = 1; i < labelPath.getNameCount() - 1; i++) {
                String label = decodeLabel(labelPath.getName(i));
                current = current
                        .map(secondOperand())
                        .flatMap(allSuites(label));
            }
            String testLabel = decodeLabel(labelPath.getName(labelPath.getNameCount() - 1));
            return current
                    .map(secondOperand())
                    .flatMap(allTests(testLabel))
                    .findFirst();
        }

        // TODO remove duplication
        return allTests(decodeLabel(labelPath.getName(0))).apply(file)
                .findFirst();
    }


    private static Function<PsiElement, Stream<ElmFunctionCall>> allSuites(String label) {
        return psi -> functionCalls(psi, "describe")
                .filter(firstArgumentIsString(label));
    }

    private static Function<PsiElement, Stream<ElmFunctionCall>> allTests(String label) {
        return psi -> functionCalls(psi, "test")
                .filter(firstArgumentIsString(label));
    }

    private static Stream<ElmFunctionCall> functionCalls(PsiElement parent, String targetName) {
        return PsiTreeUtil.findChildrenOfType(parent, ElmFunctionCall.class)
                .stream()
                .filter(call -> call.getTarget().getText().equals(targetName));
    }

    private static Predicate<ElmFunctionCall> firstArgumentIsString(String value) {
        return call -> firstOperand()
                .andThen(literalString())
                .andThen(s -> s.equals(value))
                .apply(call);
    }

    private static Function<ElmFunctionCall, ElmOperandTag> firstOperand() {
        return call -> call.getArguments().iterator().next();
    }

    private static Function<ElmFunctionCall, ElmOperandTag> secondOperand() {
        return call -> {
            Iterator<ElmOperandTag> iterator = call.getArguments().iterator();
            iterator.next();
            return iterator.next();
        };
    }

    private static Function<ElmOperandTag, String> literalString() {
        return op -> stringConstant(op);
    }

    private static String stringConstant(ElmOperandTag op) {
        if (op instanceof ElmStringConstant) {
            return PsiTreeUtil.findSiblingForward(op.getFirstChild(), ElmTypes.REGULAR_STRING_PART, null).getText();
        }
        return PsiTreeUtil.findChildOfType(op, ElmStringConstant.class).getText();
    }

}

