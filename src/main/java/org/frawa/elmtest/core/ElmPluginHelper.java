package org.frawa.elmtest.core;

import com.intellij.openapi.util.Condition;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.elm.lang.core.psi.ElmAtomTag;
import org.elm.lang.core.psi.ElmOperandTag;
import org.elm.lang.core.psi.ElmTypes;
import org.elm.lang.core.psi.elements.ElmFunctionCallExpr;
import org.elm.lang.core.psi.elements.ElmStringConstantExpr;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.frawa.elmtest.core.LabelUtils.decodeLabel;

public class ElmPluginHelper {

    public static PsiElement getPsiElement(boolean isDescribe, String labels, PsiFile file) {
        return getPsiElement(isDescribe, Paths.get(labels), file);
    }

    private static PsiElement getPsiElement(boolean isDescribe, Path labelPath, PsiFile file) {
        Optional<? extends PsiElement> found = findPsiElement(isDescribe, labelPath, file);
        if (found.isPresent()) {
            return found.get();
        } else if (labelPath.getParent() != null) {
            return getPsiElement(isDescribe, labelPath.getParent(), file);
        }
        return file;
    }

    private static Optional<? extends PsiElement> findPsiElement(boolean isDescribe, Path labelPath, PsiFile file) {
        if (labelPath.getNameCount() == 0) {
            return Optional.empty();
        }

        String topLabel = decodeLabel(labelPath.getName(0));
        if (labelPath.getNameCount() > 1 || isDescribe) {
            Stream<ElmFunctionCallExpr> current = allSuites(topLabel).apply(file)
                    .filter(topLevel());
            for (int i = 1; i < labelPath.getNameCount() - 1; i++) {
                String label = decodeLabel(labelPath.getName(i));
                current = current
                        .map(secondOperand())
                        .flatMap(allSuites(label));
            }

            if (labelPath.getNameCount() > 1) {
                String leafLabel = decodeLabel(labelPath.getName(labelPath.getNameCount() - 1));
                Function<PsiElement, Stream<ElmFunctionCallExpr>> leaf = isDescribe
                        ? allSuites(leafLabel)
                        : allTests(leafLabel);
                current = current
                        .map(secondOperand())
                        .flatMap(leaf);
            }
            return current
                    .findFirst();
        }

        return allTests(topLabel).apply(file)
                .filter(topLevel())
                .findFirst();
    }


    private static Function<PsiElement, Stream<ElmFunctionCallExpr>> allSuites(String label) {
        return psi -> functionCalls(psi, "describe")
                .filter(firstArgumentIsString(label));
    }

    private static Function<PsiElement, Stream<ElmFunctionCallExpr>> allTests(String label) {
        return psi -> functionCalls(psi, "test")
                .filter(firstArgumentIsString(label));
    }

    private static Stream<ElmFunctionCallExpr> functionCalls(PsiElement parent, String targetName) {
        return PsiTreeUtil.findChildrenOfType(parent, ElmFunctionCallExpr.class)
                .stream()
                .filter(call -> call.getTarget().getText().equals(targetName));
    }

    private static Predicate<ElmFunctionCallExpr> topLevel() {
        return call -> null == PsiTreeUtil.findFirstParent(call, true, new Condition<PsiElement>() {
            @Override
            public boolean value(PsiElement element) {
                return isSuite(element);
            }
        });
    }

    private static boolean isSuite(PsiElement element) {
        return (element instanceof ElmFunctionCallExpr) && ((ElmFunctionCallExpr) element).getTarget().getText().equals("describe");
    }

    private static Predicate<ElmFunctionCallExpr> firstArgumentIsString(String value) {
        return call -> firstOperand()
                .andThen(literalString())
                .andThen(s -> s.equals(value))
                .apply(call);
    }

    private static Function<ElmFunctionCallExpr, ElmOperandTag> firstOperand() {
        return call -> call.getArguments().iterator().next();
    }

    private static Function<ElmFunctionCallExpr, ElmAtomTag> secondOperand() {
        return call -> {
            Iterator<ElmAtomTag> iterator = call.getArguments().iterator();
            iterator.next();
            return iterator.next();
        };
    }

    private static Function<ElmOperandTag, String> literalString() {
        return op -> stringConstant(op);
    }

    private static String stringConstant(ElmOperandTag op) {
        if (op instanceof ElmStringConstantExpr) {
            return PsiTreeUtil.findSiblingForward(op.getFirstChild(), ElmTypes.REGULAR_STRING_PART, null).getText();
        }
        return PsiTreeUtil.findChildOfType(op, ElmStringConstantExpr.class).getText();
    }

}

