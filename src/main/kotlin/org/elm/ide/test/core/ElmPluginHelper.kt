package org.elm.ide.test.core

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
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
        val found = findPsiElement(isDescribe, labelPath, file)
        if (found != null) {
            return found
        } else if (labelPath.parent != null) {
            return getPsiElement(isDescribe, labelPath.parent, file)
        }
        return file
    }

    private fun findPsiElement(isDescribe: Boolean, labelPath: Path, file: PsiFile): PsiElement? {
        if (labelPath.nameCount == 0) {
            return null
        }

        val topLabel = LabelUtils.decodeLabel(labelPath.getName(0))
        if (labelPath.nameCount > 1 || isDescribe) {
            var current = allSuites(topLabel)(file)
                    .filter(topLevel())
            for (i in 1 until labelPath.nameCount - 1) {
                val label = LabelUtils.decodeLabel(labelPath.getName(i))
                current = current
                        .map(secondOperand())
                        .flatMap(allSuites(label))
            }

            if (labelPath.nameCount > 1) {
                val leafLabel = LabelUtils.decodeLabel(labelPath.getName(labelPath.nameCount - 1))
                val leaf = if (isDescribe)
                    allSuites(leafLabel)
                else
                    allTests(leafLabel)
                current = current
                        .map(secondOperand())
                        .flatMap(leaf)
            }
            return current.firstOrNull()
        }

        return allTests(topLabel)(file)
                .filter(topLevel())
                .firstOrNull()
    }


    private fun allSuites(label: String): (PsiElement) -> List<ElmFunctionCallExpr> {
        return { psi ->
            functionCalls(psi, "describe")
                    .filter(firstArgumentIsString(label))
        }
    }

    private fun allTests(label: String): (PsiElement) -> List<ElmFunctionCallExpr> {
        return { psi ->
            functionCalls(psi, "test")
                    .filter(firstArgumentIsString(label))
        }
    }

    private fun functionCalls(parent: PsiElement, targetName: String): List<ElmFunctionCallExpr> {
        return PsiTreeUtil.findChildrenOfType(parent, ElmFunctionCallExpr::class.java)
                .filter { call -> call.target.text == targetName }
    }

    private fun topLevel(): (ElmFunctionCallExpr) -> Boolean {
        return { call -> null == PsiTreeUtil.findFirstParent(call, true) { element -> isSuite(element) } }
    }

    private fun isSuite(element: PsiElement): Boolean {
        return element is ElmFunctionCallExpr && element.target.text == "describe"
    }

    private fun firstArgumentIsString(value: String): (ElmFunctionCallExpr) -> Boolean {
        return { call ->
            literalString()(firstOperand()(call)) == value
        }
    }

    private fun firstOperand(): (ElmFunctionCallExpr) -> ElmOperandTag {
        return { call -> call.arguments.iterator().next() }
    }

    private fun secondOperand(): (ElmFunctionCallExpr) -> ElmAtomTag {
        return { call ->
            val iterator = call.arguments.iterator()
            iterator.next()
            iterator.next()
        }
    }

    private fun literalString(): (ElmOperandTag) -> String {
        return { op -> stringConstant(op) }
    }

    private fun stringConstant(op: ElmOperandTag): String {
        return if (op is ElmStringConstantExpr) {
            PsiTreeUtil.findSiblingForward(op.getFirstChild(), ElmTypes.REGULAR_STRING_PART, null)!!.text
        } else PsiTreeUtil.findChildOfType(op, ElmStringConstantExpr::class.java)!!.text
    }

}

