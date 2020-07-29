package org.elm.ide.folding

import com.intellij.codeInsight.folding.CodeFoldingSettings
import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.util.PsiTreeUtil
import org.elm.lang.core.psi.ElmFile
import org.elm.lang.core.psi.ElmTypes.*
import org.elm.lang.core.psi.directChildren
import org.elm.lang.core.psi.elementType
import org.elm.lang.core.psi.elements.*

class ElmFoldingBuilder : FoldingBuilderEx(), DumbAware {

    override fun getPlaceholderText(node: ASTNode): String? {
        return when (node.elementType) {
            BLOCK_COMMENT, DOC_COMMENT -> {
                val nl = node.text.indexOf('\n')
                if (nl > 0) node.text.substring(0, nl) + " ...-}"
                else "{-...-}"
            }
            RECORD_EXPR, RECORD_TYPE -> "{...}"
            TYPE_ALIAS_DECLARATION, TYPE_DECLARATION, VALUE_DECLARATION -> " = ..."
            else -> "..."
        }
    }

    override fun isCollapsedByDefault(node: ASTNode): Boolean {
        return with(CodeFoldingSettings.getInstance()) {
            COLLAPSE_DOC_COMMENTS && node.elementType == DOC_COMMENT
                    || COLLAPSE_IMPORTS && node.elementType == IMPORT_CLAUSE
        }
    }

    override fun buildFoldRegions(root: PsiElement, document: Document, quick: Boolean): Array<FoldingDescriptor> {
        if (root !is ElmFile) return emptyArray()

        val visitor = ElmFoldingVisitor()
        PsiTreeUtil.processElements(root) {
            it.accept(visitor)
            true
        }
        return visitor.descriptors.toTypedArray()
    }
}

private class ElmFoldingVisitor : PsiElementVisitor() {
    val descriptors = ArrayList<FoldingDescriptor>()

    override fun visitElement(element: PsiElement) {
        super.visitElement(element)
        when (element) {
            is ElmFile -> {
                val imports = element.directChildren.filterIsInstance<ElmImportClause>().toList()
                if (imports.size < 2) return
                val start = imports.first()
                foldBetween(start, start.moduleQID, imports.last(), true, true)
            }
            is ElmModuleDeclaration -> {
                if (element.exposesAll) return
                val exposingList = element.exposingList ?: return
                foldBetween(element, exposingList.openParen, exposingList.closeParen, false, false)
            }
            is PsiComment -> {
                if (element.elementType == BLOCK_COMMENT || element.elementType == DOC_COMMENT) fold(element)
            }
            is ElmRecordType, is ElmRecordExpr -> {
                fold(element)
            }
            is ElmValueDeclaration -> {
                foldToEnd(element) { functionDeclarationLeft ?: pattern }
            }
            is ElmTypeDeclaration -> {
                foldToEnd(element) { lowerTypeNameList.lastOrNull() ?: nameIdentifier }
            }
            is ElmTypeAliasDeclaration -> {
                foldToEnd(element) { lowerTypeNameList.lastOrNull() ?: nameIdentifier }
            }
            is ElmLetInExpr -> {
                val letKw = element.directChildren.find { it.elementType == LET } ?: return
                val inKw = element.directChildren.find { it.elementType == IN } ?: return
                foldBetween(letKw, letKw, inKw, false, false)
                foldBetween(inKw, inKw, element.lastChild, false, true)
            }
            is ElmCaseOfExpr -> {
                foldToEnd(element) { directChildren.find { it.elementType == OF } }
            }
            is ElmCaseOfBranch -> {
                foldToEnd(element) { directChildren.find { it.elementType == ARROW } }
            }
        }
    }

    private fun fold(element: PsiElement?) {
        if (element == null) return
        descriptors += FoldingDescriptor(element, element.textRange)
    }

    private inline fun <T : PsiElement> foldToEnd(element: T, predicate: T.() -> PsiElement?) {
        val start = predicate(element) ?: return
        foldBetween(element, start, element.lastChild, false, true)
    }

    private fun foldBetween(element: PsiElement, left: PsiElement?, right: PsiElement?,
                            includeStart: Boolean, includeEnd: Boolean) {
        if (left == null || right == null) return
        val start = if (includeStart) left.textRange.startOffset else left.textRange.endOffset
        val end = if (includeEnd) right.textRange.endOffset else right.textRange.startOffset
        if (end <= start) return
        descriptors += FoldingDescriptor(element, TextRange(start, end))
    }
}
