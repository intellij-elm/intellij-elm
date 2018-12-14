package org.elm.lang.core.types

import com.intellij.psi.PsiElement
import org.elm.lang.core.diagnostics.BadRecursionError
import org.elm.lang.core.diagnostics.ElmDiagnostic
import org.elm.lang.core.psi.ElmTypeSignatureDeclarationTag
import org.elm.lang.core.psi.elements.*
import org.elm.lang.core.psi.parentOfType
import org.elm.lang.core.resolve.ElmReferenceElement


// Changes to type expressions aways invalidate the whole project, since they influence inferred
// value types (e.g. removing a field from a record causes usages of that field everywhere to be invalid.)
// These results should be able to be cached, but doing so with the CachedValueManager lead to invalid Psi references.


fun ElmTypeDeclaration.typeExpressionInference(): InferenceResult = TypeExpression(mutableMapOf()).beginTypeDeclarationInference(this)
fun ElmTypeAliasDeclaration.typeExpressionInference(): InferenceResult = TypeExpression(mutableMapOf()).beginTypeAliasDeclarationInference(this)
fun ElmPortAnnotation.typeExpressionInference(): InferenceResult = TypeExpression(mutableMapOf()).beginPortAnnotationInference(this)
fun ElmUnionMember.typeExpressionInference(): InferenceResult = TypeExpression(mutableMapOf()).beginUnionConstructorInference(this)

/** Get the type of the type ref in this annotation, or null if the program is incomplete and no type ref exists*/
fun ElmTypeAnnotation.typeExpressionInference(): InferenceResult? {
    val typeRef = typeRef ?: return null
    return TypeExpression(mutableMapOf()).beginTypeRefInference(typeRef)
}

/**
 * Inference for type declarations and expressions like function annotations and constructor calls.
 *
 * For inference of value declarations and expressions, see [InferenceScope]
 */
class TypeExpression(
        private val varsByName: MutableMap<String, TyVar>,
        private val diagnostics: MutableList<ElmDiagnostic> = mutableListOf(),
        private val activeAliases: MutableSet<ElmTypeAliasDeclaration> = mutableSetOf()
) {
    fun beginPortAnnotationInference(annotation: ElmPortAnnotation): InferenceResult {
        val ty = annotation.typeRef?.let { typeRefType(it) } ?: TyUnknown()
        return result(ty)
    }

    fun beginTypeRefInference(typeRef: ElmTypeRef): InferenceResult {
        val ty = typeRefType(typeRef)
        return result(ty)
    }

    fun beginTypeDeclarationInference(typeDeclaration: ElmTypeDeclaration): InferenceResult {
        val ty = typeDeclarationType(typeDeclaration)
        return result(ty)
    }

    fun beginUnionConstructorInference(member: ElmUnionMember): InferenceResult {
        val decl = member.parentOfType<ElmTypeDeclaration>()
                ?.typeExpressionInference()?.ty
                ?: return result(TyUnknown())

        // Populate this scope's vars from the declaration
        (decl as TyUnion).parameters.associateTo(varsByName) { (it as TyVar).name to it }

        val params = member.allParameters.map { typeSignatureDeclType(it) }.toList()

        val ty: Ty = if (params.isNotEmpty()) {
            // Constructors with parameters are functions returning the type.
            TyFunction(params, decl)
        } else {
            // Constructors without parameters are just instances of the type, since there are no nullary functions.
            decl
        }
        return result(ty)
    }

    fun beginTypeAliasDeclarationInference(decl: ElmTypeAliasDeclaration): InferenceResult {
        val record = decl.aliasedRecord
        val params = decl.lowerTypeNameList.map { getTyVar(it.name) }.toList()

        if (decl in activeAliases) {
            diagnostics += BadRecursionError(decl)
            return result(TyUnknown())
        }

        activeAliases += decl

        val ty = if (record == null) {
            decl.typeRef?.let { typeRefType(it) } ?: TyUnknown()
        } else {
            recordTypeDeclType(record)
        }

        val aliasInfo = AliasInfo(decl.moduleName, decl.upperCaseIdentifier.text, params)
        return result(ty.withAlias(aliasInfo))
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
            is ElmRecordType -> recordTypeDeclType(decl)
            is ElmTupleType -> if (decl.unitExpr != null) TyUnit() else TyTuple(decl.typeRefList.map { typeRefType(it) })
            is ElmParametricTypeRef -> parametricTypeRefType(decl)
            is ElmTypeRef -> typeRefType(decl)
            else -> error("unimplemented type $decl")
        }
    }

    private fun recordTypeDeclType(record: ElmRecordType): TyRecord {
        val declaredFields = record.fieldTypeList.associate { it.lowerCaseIdentifier.text to typeRefType(it.typeRef) }
        val baseTy = record.baseTypeIdentifier?.referenceName?.let { getTyVar(it) }
        return TyRecord(declaredFields, baseTy)
    }

    private fun parametricTypeRefType(typeRef: ElmParametricTypeRef): Ty {
        val argElements = typeRef.allParameters.toList()
        val args = argElements.map { typeSignatureDeclType(it) }
        return resolvedTypeRefType(args, argElements, typeRef)
    }

    private fun upperPathTypeRefType(typeRef: ElmUpperPathTypeRef): Ty {
        // upper path type refs are just parametric type refs without any arguments
        return resolvedTypeRefType(emptyList(), emptyList(), typeRef)
    }

    private fun resolvedTypeRefType(args: List<Ty>, argElements: List<PsiElement>, typeRef: ElmReferenceElement): Ty {
        val ref = typeRef.reference.resolve()
        val declaredTy = when {
            ref is ElmTypeAliasDeclaration -> {
                inferChild { beginTypeAliasDeclarationInference(ref) }
            }
            ref is ElmTypeDeclaration -> {
                inferChild { beginTypeDeclarationInference(ref) }
            }
            // Unlike all other built-in types, Elm core doesn't define the List type anywhere, so the
            // reference won't resolve. So we check for a reference to that type here. Note that users can
            // create their own List types that shadow the built-in, so we only want to do this check if the
            // reference is null.
            ref == null && typeRef.referenceName == "List" -> {
                TyList(TyVar("a"))
            }
            // We only get here if the reference doesn't resolve.
            else -> TyUnknown()
        }

        if (!isInferable(declaredTy)) {
            return declaredTy
        }

        // This cast is safe, since parameters of type declarations are always inferred as TyVar.
        // This function is never called after on a type after its vars have been replaced.
        @Suppress("UNCHECKED_CAST")
        val params = when {
            declaredTy.alias != null -> declaredTy.alias!!.parameters
            declaredTy is TyUnion -> declaredTy.parameters
            else -> emptyList()
        } as List<TyVar>

        val result = TypeReplacement.replaceCall(typeRef, declaredTy, params, args, argElements)
        diagnostics += result.diagnostics

        return result.ty
    }

    private fun typeDeclarationType(declaration: ElmTypeDeclaration): TyUnion {
        val params = declaration.lowerTypeNameList.map { getTyVar(it.name) }
        return TyUnion(declaration.moduleName, declaration.name, params)
    }

    private fun getTyVar(name: String) = varsByName.getOrPut(name) { TyVar(name) }

    private inline fun inferChild(block: TypeExpression.() -> InferenceResult) =
            TypeExpression(mutableMapOf(), diagnostics, activeAliases.toMutableSet()).block().ty
}

