package org.elm.ide.test.core

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiTreeUtil.findFirstParent
import org.elm.ide.test.core.LabelUtils.decodeLabel
import org.elm.lang.core.psi.ElmAtomTag
import org.elm.lang.core.psi.ElmOperandTag
import org.elm.lang.core.psi.ElmTypes
import org.elm.lang.core.psi.elements.ElmFunctionCallExpr
import org.elm.lang.core.psi.elements.ElmStringConstantExpr
import java.nio.file.Path
import java.nio.file.Paths

object ElmPluginHelper {

    fun getPsiElement(isDescribe: Boolean, labels: String, file: PsiFile): PsiElement {
        return getPsiElement(isDescribe, Paths.get(labels), file)
    }

    private fun getPsiElement(isDescribe: Boolean, labelPath: Path, file: PsiFile): PsiElement {
        return findPsiElement(isDescribe, labelPath, file)
                ?: labelPath.parent?.let { getPsiElement(isDescribe, it, file) }
                ?: file
    }

    private fun findPsiElement(isDescribe: Boolean, labelPath: Path, file: PsiFile): PsiElement? {
        val labels = labels(labelPath)

        if (labels.isEmpty()) {
            return null
        }

        val topLabel = decodeLabel(labels.first())
        val subLabels = labels.drop(1)

        if (subLabels.isEmpty() && !isDescribe) {
            return allTests(topLabel)(file).firstOrNull(topLevel())
        }

        val topSuites = allSuites(topLabel)(file)
                .filter(topLevel())

        if (isDescribe) {
            return subSuites(subLabels, topSuites).firstOrNull()
        }

        val deepestSuites = subSuites(subLabels.dropLast(1), topSuites)
        val leafLabel = decodeLabel(labels.last())
        return deepestSuites.map(secondOperand())
                .flatMap(allTests(leafLabel))
                .firstOrNull()
    }

    private fun subSuites(labels: List<String>, tops: List<ElmFunctionCallExpr>): List<ElmFunctionCallExpr> {
        return labels
                .fold(tops)
                { acc, label ->
                    acc
                            .map(secondOperand())
                            .flatMap(allSuites(label))
                }
    }

    private fun labels(path: Path): List<String> {
        return (0 until path.nameCount)
                .map { path.getName(it) }
                .map { it.toString() }
                .toList()
    }

    private fun allSuites(label: String): (PsiElement) -> List<ElmFunctionCallExpr> {
        return {
            functionCalls(it, "describe")
                    .filter(firstArgumentIsString(label))
        }
    }

    private fun allTests(label: String): (PsiElement) -> List<ElmFunctionCallExpr> {
        return {
            listOf(
                    functionCalls(it, "test")
                            .filter(firstArgumentIsString(label)),
                    functionCalls(it, "fuzz")
                            .filter(secondArgumentIsString(label))
            ).flatten()
        }
    }

    private fun functionCalls(parent: PsiElement, targetName: String): List<ElmFunctionCallExpr> {
        return PsiTreeUtil.findChildrenOfType(parent, ElmFunctionCallExpr::class.java)
                .filter { it.target.text == targetName }
    }

    private fun topLevel(): (ElmFunctionCallExpr) -> Boolean {
        return { null == findFirstParent(it, true) { element -> isSuite(element) } }
    }

    private fun isSuite(element: PsiElement): Boolean {
        return element is ElmFunctionCallExpr && element.target.text == "describe"
    }

    private fun firstArgumentIsString(value: String): (ElmFunctionCallExpr) -> Boolean {
        return { literalString()(firstOperand()(it)) == value }
    }

    private fun secondArgumentIsString(value: String): (ElmFunctionCallExpr) -> Boolean {
        return { literalString()(secondOperand()(it)) == value }
    }

    private fun firstOperand(): (ElmFunctionCallExpr) -> ElmOperandTag {
        return { it.arguments.first() }
    }

    private fun secondOperand(): (ElmFunctionCallExpr) -> ElmAtomTag {
        return { it.arguments.drop(1).first() }
    }

    private fun literalString(): (ElmOperandTag) -> String {
        return { stringConstant(it) }
    }

    private fun stringConstant(op: ElmOperandTag): String {
        return if (op is ElmStringConstantExpr) {
            PsiTreeUtil.findSiblingForward(op.getFirstChild(), ElmTypes.REGULAR_STRING_PART, null)!!.text
        } else {
            PsiTreeUtil.findChildOfType(op, ElmStringConstantExpr::class.java)!!.text
        }
    }

}

