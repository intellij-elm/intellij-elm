package org.elm.lang.core.psi

import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiElement
import org.elm.lang.core.psi.elements.*
import org.elm.lang.core.psi.ext.ElmNameIdentifierOwner

class ElmVisitor : PsiElementVisitor() {

    fun visitAnonymousFunction(o: ElmAnonymousFunction) {
        visitPsiElement(o)
    }

    fun visitAsClause(o: ElmAsClause) {
        visitPsiElement(o)
    }

    fun visitCaseOf(o: ElmCaseOf) {
        visitPsiElement(o)
    }

    fun visitCaseOfBranch(o: ElmCaseOfBranch) {
        visitPsiElement(o)
    }

    fun visitExposedUnion(o: ElmExposedUnion) {
        visitPsiElement(o)
    }

    fun visitExposedUnionConstructors(o: ElmExposedUnionConstructors) {
        visitPsiElement(o)
    }

    fun visitExpression(o: ElmExpression) {
        visitPsiElement(o)
    }

    fun visitField(o: ElmField) {
        visitPsiElement(o)
    }

    fun visitFieldType(o: ElmFieldType) {
        visitPsiElement(o)
    }

    fun visitFunctionDeclarationLeft(o: ElmFunctionDeclarationLeft) {
        visitPsiElement(o)
    }

    fun visitGlslCode(o: ElmGlslCode) {
        visitPsiElement(o)
    }

    fun visitIfElse(o: ElmIfElse) {
        visitPsiElement(o)
    }

    fun visitImportClause(o: ElmImportClause) {
        visitPsiElement(o)
    }

    fun visitInnerTypeAnnotation(o: ElmInnerTypeAnnotation) {
        visitPsiElement(o)
    }

    fun visitInnerValueDeclaration(o: ElmInnerValueDeclaration) {
        visitPsiElement(o)
    }

    fun visitLetIn(o: ElmLetIn) {
        visitPsiElement(o)
    }

    fun visitList(o: ElmList) {
        visitPsiElement(o)
    }

    fun visitListOfOperands(o: ElmListOfOperands) {
        visitPsiElement(o)
    }

    fun visitLowerCaseId(o: ElmLowerCaseId) {
        visitPsiElement(o)
    }

    fun visitModuleDeclaration(o: ElmModuleDeclaration) {
        visitPsiElement(o)
    }

    fun visitNonEmptyTuple(o: ElmNonEmptyTuple) {
        visitPsiElement(o)
    }

    fun visitOperatorAsFunction(o: ElmOperatorAsFunction) {
        visitPsiElement(o)
    }

    fun visitOperatorConfig(o: ElmOperatorConfig) {
        visitPsiElement(o)
    }

    fun visitOperatorDeclarationLeft(o: ElmOperatorDeclarationLeft) {
        visitPsiElement(o)
    }

    fun visitParenthesedExpression(o: ElmParenthesedExpression) {
        visitPsiElement(o)
    }

    fun visitPattern(o: ElmPattern) {
        visitPsiElement(o)
    }

    fun visitRecord(o: ElmRecord) {
        visitPsiElement(o)
    }

    fun visitRecordType(o: ElmRecordType) {
        visitPsiElement(o)
    }

    fun visitTupleConstructor(o: ElmTupleConstructor) {
        visitPsiElement(o)
    }

    fun visitTupleType(o: ElmTupleType) {
        visitPsiElement(o)
    }

    fun visitTypeAliasDeclaration(o: ElmTypeAliasDeclaration) {
        visitPsiElement(o)
    }

    fun visitTypeAnnotation(o: ElmTypeAnnotation) {
        visitPsiElement(o)
    }

    fun visitTypeDeclaration(o: ElmTypeDeclaration) {
        visitNameIdentifierOwner(o)
    }

    fun visitTypeDefinition(o: ElmTypeDefinition) {
        visitPsiElement(o)
    }

    fun visitUnionMember(o: ElmUnionMember) {
        visitPsiElement(o)
    }

    fun visitUnionPattern(o: ElmUnionPattern) {
        visitPsiElement(o)
    }

    fun visitUnit(o: ElmUnit) {
        visitPsiElement(o)
    }

    fun visitUpperCaseId(o: ElmUpperCaseId) {
        visitPsiElement(o)
    }

    fun visitValueDeclaration(o: ElmValueDeclaration) {
        visitPsiElement(o)
    }

    fun visitNameIdentifierOwner(o: ElmNameIdentifierOwner) {
        visitPsiElement(o)
    }

    fun visitExposingClause(o: ElmExposingClause) {
        visitPsiElement(o)
    }

    fun visitPsiElement(o: PsiElement) {
        visitElement(o)
    }

}
