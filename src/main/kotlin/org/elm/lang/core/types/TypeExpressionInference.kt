package org.elm.lang.core.types

import com.intellij.psi.PsiElement
import org.elm.lang.core.diagnostics.BadRecursionError
import org.elm.lang.core.diagnostics.ElmDiagnostic
import org.elm.lang.core.psi.ElmTypeSignatureDeclarationTag
import org.elm.lang.core.psi.elements.*
import org.elm.lang.core.psi.parentOfType
import org.elm.lang.core.resolve.ElmReferenceElement

/**
 * Inference for type declarations and expressions like function annotations and constructor calls.
 *
 * For inference of value declarations and expressions, see [InferenceScope]
 */
class TypeExpression private constructor(
        private val varsByName: MutableMap<String, TyVar>,
        private val diagnostics: MutableList<ElmDiagnostic> = mutableListOf(),
        private val activeAliases: MutableSet<ElmTypeAliasDeclaration> = mutableSetOf()
) {
    companion object {
        fun inferPortAnnotation(annotation: ElmPortAnnotation): InferenceResult {
            val scope = TypeExpression(mutableMapOf())
            val ty = annotation.typeRef?.let { scope.typeRefType(it) } ?: TyUnknown
            return scope.result(ty)
        }

        fun inferTypeRef(typeRef: ElmTypeRef): InferenceResult {
            val scope = TypeExpression(mutableMapOf())
            val ty = scope.typeRefType(typeRef)
            return scope.result(ty)
        }

        fun inferTypeDeclaration(typeDeclaration: ElmTypeDeclaration): InferenceResult {
            val scope = TypeExpression(mutableMapOf())
            val ty = scope.typeDeclarationType(typeDeclaration).second
            return scope.result(ty)
        }

        fun inferUnionConstructor(member: ElmUnionMember): InferenceResult {
            val scope = TypeExpression(mutableMapOf())
            val decl = member.parentOfType<ElmTypeDeclaration>()
                    ?.let { scope.typeDeclarationType(it).second }
                    ?: return scope.result(TyUnknown)
            val params = member.allParameters.map { scope.typeSignatureDeclType(it) }.toList()

            val ty = if (params.isNotEmpty()) {
                // Constructors with parameters are functions returning the type.
                TyFunction(params, decl)
            } else {
                // Constructors without parameters are just instances of the type, since there are no nullary functions.
                decl
            }
            return scope.result(ty)
        }

        fun inferTypeAliasDeclaration(decl: ElmTypeAliasDeclaration): InferenceResult {
            val scope = TypeExpression(mutableMapOf())
            val ty = scope.typeAliasDeclarationType(decl).second
            return scope.result(ty)
        }
    }

    private fun result(ty: Ty) = InferenceResult(emptyMap(), diagnostics, ty)


    /** Get the type for an entire type ref */
    private fun typeRefType(typeRef: ElmTypeRef): Ty {
        val segments = typeRef.allSegments.map { typeSignatureDeclType(it) }.toList()
        val last = segments.last()
        return when {
            segments.size == 1 -> last
            last is TyFunction -> TyFunction(segments.dropLast(1) + last.parameters, last.ret)
            else -> TyFunction(segments.dropLast(1), last)
        }
    }

    /** Get the type for one segment of a type ref */
    private fun typeSignatureDeclType(decl: ElmTypeSignatureDeclarationTag): Ty {
        return when (decl) {
            is ElmUpperPathTypeRef -> upperPathTypeRefType(decl)
            is ElmTypeVariableRef -> getTyVar(decl.identifier.text)
            is ElmRecordType -> recordTypeDeclType(decl, null)
            is ElmTupleType -> if (decl.unitExpr != null) TyUnit else TyTuple(decl.typeRefList.map { typeRefType(it) })
            is ElmParametricTypeRef -> parametricTypeRefType(decl)
            is ElmTypeRef -> typeRefType(decl)
            else -> error("unimplemented type $decl")
        }
    }

    private fun recordTypeDeclType(record: ElmRecordType, alias: TyUnion?): TyRecord {
        val declaredFields = record.fieldTypeList.associate { it.lowerCaseIdentifier.text to typeRefType(it.typeRef) }
        val baseTy = record.baseTypeIdentifier?.referenceName?.let { getTyVar(it) }
        return TyRecord(declaredFields, baseTy, alias)
    }

    private fun parametricTypeRefType(typeRef: ElmParametricTypeRef): Ty {
        val argElements = typeRef.allParameters.toList()
        val args = argElements.map { typeSignatureDeclType(it) }
        return resolvedTypeRefType(args, argElements, typeRef)
    }

    private fun upperPathTypeRefType(typeRef: ElmUpperPathTypeRef): Ty {
        return resolvedTypeRefType(emptyList(), emptyList(), typeRef)
    }

    private fun resolvedTypeRefType(args: List<Ty>, argElements: List<PsiElement>, typeRef: ElmReferenceElement): Ty {
        val ref = typeRef.reference.resolve()
        val (params, declaredTy) = when {
            ref is ElmTypeAliasDeclaration -> {
                val scope = TypeExpression(mutableMapOf(), diagnostics, activeAliases.toMutableSet())
                scope.typeAliasDeclarationType(ref)
            }
            ref is ElmTypeDeclaration -> {
                val scope = TypeExpression(mutableMapOf(), diagnostics, mutableSetOf())
                scope.typeDeclarationType(ref)
            }
            // Unlike all other built-in types, Elm core doesn't define the List type anywhere, so the
            // reference won't resolve. So we check for a reference to that type here. Note that users can
            // create their own List types that shadow the built-in, so we only want to do this check if the
            // reference is null.
            ref == null && typeRef.referenceName == "List" -> {
                val tyVar = TyVar("a")
                listOf(tyVar) to TyList(tyVar)
            }
            // We only get here if the reference doesn't resolve. We could create a TyUnion from the
            // ref name, but we don't know what module it's supposed to be defined in, so that would
            // lead to false positives.
            else -> emptyList<TyVar>() to TyUnknown
        }

        val result = TypeReplacement.replaceCall(typeRef, params, args, argElements, declaredTy)
        diagnostics += result.diagnostics

        return result.ty
    }

    private fun typeAliasDeclarationType(decl: ElmTypeAliasDeclaration): Pair<List<TyVar>, Ty> {
        val record = decl.aliasedRecord
        val params = decl.lowerTypeNameList.map { getTyVar(it.name) }.toList()

        if (decl in activeAliases) {
            diagnostics += BadRecursionError(decl)
            return params to TyUnknown
        }

        activeAliases += decl

        val ty = if (record == null) {
            decl.typeRef?.let { typeRefType(it) } ?: TyUnknown
        } else {
            val aliasTy = TyUnion(decl.moduleName, decl.upperCaseIdentifier.text, params)
            recordTypeDeclType(record, aliasTy)
        }

        return params to ty
    }

    private fun typeDeclarationType(declaration: ElmTypeDeclaration): Pair<List<TyVar>, TyUnion> {
        val params = declaration.lowerTypeNameList.map { getTyVar(it.name) }
        return params to TyUnion(declaration.moduleName, declaration.name, params)
    }

    private fun getTyVar(name: String) = varsByName.getOrPut(name) { TyVar(name) }
}

