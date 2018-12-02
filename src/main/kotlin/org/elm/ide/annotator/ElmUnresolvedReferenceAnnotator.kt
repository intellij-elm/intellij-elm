package org.elm.ide.annotator

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import org.elm.ide.intentions.ElmImportIntentionAction
import org.elm.ide.intentions.ElmMakeDeclarationIntentionAction
import org.elm.lang.core.psi.ElmFile
import org.elm.lang.core.psi.ElmQID
import org.elm.lang.core.psi.ancestors
import org.elm.lang.core.psi.elements.*
import org.elm.lang.core.resolve.reference.QualifiedConstructorReference
import org.elm.lang.core.resolve.reference.QualifiedTypeReference
import org.elm.lang.core.resolve.reference.QualifiedValueReference
import org.elm.lang.core.resolve.reference.TypeVariableReference
import org.elm.lang.core.resolve.scope.GlobalScope
import org.elm.lang.core.resolve.scope.ImportScope
import org.elm.lang.core.resolve.scope.ModuleScope


/**
 * A handler should return `true` if it handled the error and no further processing needs to be done.
 */
typealias BadRefHandler =
        (ref: PsiReference, element: PsiElement, holder: AnnotationHolder) -> Boolean


class ElmUnresolvedReferenceAnnotator : Annotator {

    // A chain of handlers to be executed sequentially when a reference cannot be resolved.
    // Order matters! Handlers earlier in the chain can short-circuit further evaluation.
    private val handlers = listOf<BadRefHandler>(
            ::handleTypeAnnotation,
            ::handleSafeToIgnore,
            ::handleModuleHiddenByAlias
    )

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        for (ref in element.references) {
            if (ref.resolve() == null) {
                var handled = false
                for (handler in handlers) {
                    if (handler(ref, element, holder)) {
                        handled = true
                        break
                    }
                }

                if (!handled) {
                    // Generic unresolved ref error
                    // TODO [kl] make this smarter in the case of qualified references
                    //           so that we don't report a double error when really the problem
                    //           is with the qualified module name reference.
                    holder.createErrorAnnotation(element, "Unresolved reference '${ref.canonicalText}'")
                            .also { it.registerFix(ElmImportIntentionAction()) }
                }
            }
        }
    }

    private fun handleTypeAnnotation(ref: PsiReference, element: PsiElement, holder: AnnotationHolder): Boolean {
        if (element !is ElmTypeAnnotation) return false

        holder.createWeakWarningAnnotation(element, "'${ref.canonicalText}' does not exist")
                .also { it.registerFix(ElmMakeDeclarationIntentionAction()) }

        return true
    }

    private fun handleSafeToIgnore(ref: PsiReference, element: PsiElement, @Suppress("UNUSED_PARAMETER") holder: AnnotationHolder): Boolean {
        // Ignore refs to built-in types and values
        if (GlobalScope.allBuiltInSymbols.contains(ref.canonicalText))
            return true

        // Ignore refs to Kernel (JavaScript) modules
        if (element is ElmValueExpr && element.upperCaseQID?.isQualifiedNativeRef()
                ?: element.valueQID?.isQualifiedNativeRef() ?: false) {
            return true
        } else if (element is ElmImportClause && element.moduleQID.isQualifiedNativeRef()) {
            return true
        }

        // Ignore refs to type variables in a type annotation
        if (ref is TypeVariableReference && element.ancestors.any { it is ElmTypeAnnotation }) {
            return true
        }

        return false
    }

    // When a module is imported using an alias (e.g. `import Json.Decode as D`),
    // Elm prohibits the use of the original module name in qualified references.
    // So we will try to detect this condition and present a helpful error.
    private fun handleModuleHiddenByAlias(ref: PsiReference, element: PsiElement, holder: AnnotationHolder): Boolean {
        if (element !is ElmValueExpr && element !is ElmUpperPathTypeRef && element !is ElmParametricTypeRef) return false
        val elmFile = element.containingFile as? ElmFile ?: return false

        val qid: ElmQID = when (ref) {
            is QualifiedValueReference -> ref.valueQID
            is QualifiedConstructorReference -> ref.upperCaseQID
            is QualifiedTypeReference -> ref.upperCaseQID
            else -> return false
        }

        if (qid.qualifierPrefix.isEmpty()) return false

        val moduleName = qid.qualifierPrefix
        val importDecl = ModuleScope(elmFile).getImportDecls().find { it.moduleQID.text == moduleName } ?: return false
        val aliasName = importDecl.asClause?.upperCaseIdentifier?.text ?: return false

        val importScope = ImportScope.fromImportDecl(importDecl) ?: return false

        val exposedNames = when (ref) {
            is QualifiedValueReference -> importScope.getExposedValues()
            is QualifiedConstructorReference -> importScope.getExposedConstructors()
            is QualifiedTypeReference -> importScope.getExposedTypes()
            else -> return false
        }

        if (exposedNames.none { it.name == ref.canonicalText })
            return false

        // Success! The reference would have succeeded were it not for the alias.
        holder.createErrorAnnotation(element, "Unresolved reference '${ref.canonicalText}'. " +
                "Module '$moduleName' is imported as '$aliasName' and so you must use the alias here.")
        return true
    }
}

private fun ElmUpperCaseQID.isQualifiedNativeRef() =
        isQualified && isKernelModule(upperCaseIdentifierList)

private fun ElmValueQID.isQualifiedNativeRef() =
        isQualified && isKernelModule(upperCaseIdentifierList)

private fun isKernelModule(identifiers: List<PsiElement>): Boolean {
    val moduleName = identifiers.joinToString(".") { it.text }
    return moduleName.startsWith("Elm.Kernel.")
            || moduleName.startsWith("Native.") // TODO [drop 0.18] remove the "Native" clause
}
