package org.elm.lang.core.types

import com.intellij.openapi.util.Key
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.elm.lang.core.diagnostics.BadRecursionError
import org.elm.lang.core.diagnostics.ElmDiagnostic
import org.elm.lang.core.psi.ElmTypeSignatureDeclarationTag
import org.elm.lang.core.psi.elements.*
import org.elm.lang.core.psi.modificationTracker
import org.elm.lang.core.psi.parentOfType


// Changes to type expressions always invalidate the whole project, since they influence inferred
// value types (e.g. removing a field from a record causes usages of that field everywhere to be invalid.)

/** A map of variant names to variant parameter tys, in declaration order */
typealias VariantParameters = Map<String, List<Ty>>

private val TY_UNION_CACHE_KEY: Key<CachedValue<ParameterizedInferenceResult<TyUnion>>> = Key.create("TY_UNION_INFERENCE")
private val TY_CACHE_KEY: Key<CachedValue<ParameterizedInferenceResult<Ty>>> = Key.create("TY_INFERENCE")
private val TY_VARIANT_CACHE_KEY: Key<CachedValue<ParameterizedInferenceResult<VariantParameters>>> = Key.create("TY_VARIANT_INFERENCE")

fun ElmTypeDeclaration.typeExpressionInference(): ParameterizedInferenceResult<TyUnion> =
        CachedValuesManager.getCachedValue(this, TY_UNION_CACHE_KEY) {
            val inferenceResult = TypeExpression().beginTypeDeclarationInference(this)
            CachedValueProvider.Result.create(inferenceResult, project.modificationTracker)
        }

fun ElmTypeAliasDeclaration.typeExpressionInference(): ParameterizedInferenceResult<Ty> =
        CachedValuesManager.getCachedValue(this, TY_CACHE_KEY) {
            val inferenceResult = TypeExpression().beginTypeAliasDeclarationInference(this)
            CachedValueProvider.Result.create(inferenceResult, project.modificationTracker)
        }


fun ElmPortAnnotation.typeExpressionInference(): ParameterizedInferenceResult<Ty> =
        CachedValuesManager.getCachedValue(this, TY_CACHE_KEY) {
            val inferenceResult = TypeExpression().beginPortAnnotationInference(this)
            CachedValueProvider.Result.create(inferenceResult, project.modificationTracker)
        }

fun ElmUnionVariant.typeExpressionInference(): ParameterizedInferenceResult<Ty> =
        CachedValuesManager.getCachedValue(this, TY_CACHE_KEY) {
            val inferenceResult = TypeExpression().beginUnionConstructorInference(this)
            CachedValueProvider.Result.create(inferenceResult, project.modificationTracker)
        }


/** Get the type of the expression in this annotation, or null if the program is incomplete and no expression exists */
fun ElmTypeAnnotation.typeExpressionInference(): ParameterizedInferenceResult<Ty>? {
    val typeRef = typeExpression ?: return null
    return CachedValuesManager.getCachedValue(typeRef, TY_CACHE_KEY) {
        val inferenceResult = TypeExpression().beginTypeRefInference(typeRef)
        CachedValueProvider.Result.create(inferenceResult, project.modificationTracker)
    }
}

/** Get the names and parameter tys for all variants of this union */
fun ElmTypeDeclaration.variantInference(): ParameterizedInferenceResult<VariantParameters> =
        CachedValuesManager.getCachedValue(this, TY_VARIANT_CACHE_KEY) {
            val inferenceResult = TypeExpression().beginUnionVariantsInference(this)
            CachedValueProvider.Result.create(inferenceResult, project.modificationTracker)
        }

/**
 * Inference for type declarations and expressions like function annotations and constructor calls.
 *
 * For inference of value declarations and expressions, see [InferenceScope].
 *
 * ### Algorithm
 *
 * Inference for most type expressions is straight forward, but we have to keep track of type
 * variables in order to correctly infer parameterized type expressions.
 *
 * Vars are scoped to a single declaration or annotation. Within a scope, all vars with the same
 * name must refer to the same object.
 *
 * To infer the types of parameterized type expressions, we infer the referenced target type, which
 * will be either a union type or a type alias, and will have one type unique type variable for each
 * parameter. We then infer the types of the arguments and use [TypeReplacement] to replace the type
 * variables in the parameters with their arguments.
 *
 * This two step process is simpler than trying to pass arguments around while inferring
 * declarations, and opens the door to caching the [Ty]s for declarations and aliases.
 */
class TypeExpression(
        private val varsByName: MutableMap<String, TyVar> = mutableMapOf(),
        private val diagnostics: MutableList<ElmDiagnostic> = mutableListOf(),
        private val activeAliases: MutableSet<ElmTypeAliasDeclaration> = mutableSetOf()
) {
    fun beginPortAnnotationInference(annotation: ElmPortAnnotation): ParameterizedInferenceResult<Ty> {
        val ty = annotation.typeExpression?.let { typeExpresionType(it) } ?: TyUnknown()
        return result(ty)
    }

    fun beginTypeRefInference(typeExpr: ElmTypeExpression): ParameterizedInferenceResult<Ty> {
        val ty = typeExpresionType(typeExpr)
        return result(ty)
    }

    fun beginTypeDeclarationInference(typeDeclaration: ElmTypeDeclaration): ParameterizedInferenceResult<TyUnion> {
        val ty = typeDeclarationType(typeDeclaration)
        return result(ty)
    }

    fun beginUnionVariantsInference(typeDeclaration: ElmTypeDeclaration): ParameterizedInferenceResult<VariantParameters> {
        val variants = typeDeclaration.unionVariantList.associate { it.name to unionVariantParamterTypes(it) }
        return result(variants)
    }

    fun beginUnionConstructorInference(variant: ElmUnionVariant): ParameterizedInferenceResult<Ty> {
        val decl = variant.parentOfType<ElmTypeDeclaration>()
                ?.let { typeDeclarationType(it) }
                ?: return result(TyUnknown())

        val params = variant.allParameters.map { typeSignatureDeclType(it) }.toList()

        val ty: Ty = if (params.isNotEmpty()) {
            // Constructors with parameters are functions returning the type.
            TyFunction(params, decl)
        } else {
            // Constructors without parameters are just instances of the type, since there are no nullary functions.
            decl
        }
        return result(ty)
    }

    fun beginTypeAliasDeclarationInference(decl: ElmTypeAliasDeclaration): ParameterizedInferenceResult<Ty> {
        val record = decl.aliasedRecord
        val params = decl.lowerTypeNameList.map { getTyVar(it.name) }.toList()

        if (decl in activeAliases) {
            diagnostics += BadRecursionError(decl)
            return result(TyUnknown())
        }

        activeAliases += decl

        val ty = if (record == null) {
            decl.typeExpression?.let { typeExpresionType(it) } ?: TyUnknown()
        } else {
            recordTypeDeclType(record)
        }

        val aliasInfo = AliasInfo(decl.moduleName, decl.upperCaseIdentifier.text, params)
        return result(ty.withAlias(aliasInfo))
    }

    private fun <T> result(value: T) = ParameterizedInferenceResult(diagnostics, value)


    /** Get the type for an entire type expression */
    private fun typeExpresionType(typeExpr: ElmTypeExpression): Ty {
        val segments = typeExpr.allSegments.map { typeSignatureDeclType(it) }.toList()
        val last = segments.last()
        return when {
            segments.size == 1 -> last
            last is TyFunction -> TyFunction(segments.dropLast(1) + last.parameters, last.ret)
            else -> TyFunction(segments.dropLast(1), last)
        }
    }

    /** Get the type for one segment of a type expression */
    private fun typeSignatureDeclType(decl: ElmTypeSignatureDeclarationTag): Ty {
        return when (decl) {
            is ElmTypeVariableRef -> getTyVar(decl.identifier.text)
            is ElmRecordType -> recordTypeDeclType(decl)
            is ElmTupleType -> if (decl.unitExpr != null) TyUnit() else TyTuple(decl.typeExpressionList.map { typeExpresionType(it) })
            is ElmTypeRef -> typeRefType(decl)
            is ElmTypeExpression -> typeExpresionType(decl)
            else -> error("unimplemented type $decl")
        }
    }

    private fun recordTypeDeclType(record: ElmRecordType): TyRecord {
        val declaredFields = record.fieldTypeList.associate { it.lowerCaseIdentifier.text to typeExpresionType(it.typeExpression) }
        val baseTy = record.baseTypeIdentifier?.referenceName?.let { getTyVar(it) }
        return TyRecord(declaredFields, baseTy)
    }

    private fun typeRefType(typeRef: ElmTypeRef): Ty {
        val argElements = typeRef.allArguments.toList()
        val args = argElements.map { typeSignatureDeclType(it) }
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
            ref == null && typeRef.referenceName == "List" -> TyList(TyVar("a"))
            // We only get here if the reference doesn't resolve.
            else -> TyUnknown()
        }

        if (!isInferable(declaredTy)) {
            return declaredTy
        }

        // This cast is safe, since parameters of type declarations are always inferred as TyVar.
        // We know the parameters haven't been replaced yet, since we just created the ty ourselves.
        @Suppress("UNCHECKED_CAST")
        val params = when {
            declaredTy.alias != null -> declaredTy.alias!!.parameters
            declaredTy is TyUnion -> declaredTy.parameters
            else -> emptyList()
        } as List<TyVar>

        val result = TypeReplacement.replaceCall(typeRef, declaredTy, params, args, argElements)
        diagnostics += result.diagnostics

        return result.value
    }

    private fun typeDeclarationType(declaration: ElmTypeDeclaration): TyUnion {
        val params = declaration.lowerTypeNameList.map { getTyVar(it.name) }
        return TyUnion(declaration.moduleName, declaration.name, params)
    }

    private fun unionVariantParamterTypes(variant: ElmUnionVariant): List<Ty> {
        return variant.allParameters.map { typeSignatureDeclType(it) }.toList()
    }

    private fun getTyVar(name: String) = varsByName.getOrPut(name) { TyVar(name) }

    private inline fun <T : Ty> inferChild(block: TypeExpression.() -> ParameterizedInferenceResult<T>) =
            TypeExpression(mutableMapOf(), diagnostics, activeAliases.toMutableSet()).block().value
}

