package org.elm.lang.core.types

import com.intellij.openapi.util.Key
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.ParameterizedCachedValue
import org.elm.lang.core.diagnostics.BadRecursionError
import org.elm.lang.core.diagnostics.ElmDiagnostic
import org.elm.lang.core.diagnostics.TypeArgumentCountError
import org.elm.lang.core.psi.*
import org.elm.lang.core.psi.elements.*


// Changes to type expressions always invalidate the whole project, since they influence inferred
// value types (e.g. removing a field from a record causes usages of that field everywhere to be invalid.)

/** A map of variant names to variant parameter tys, in declaration order */
typealias VariantParameters = Map<String, List<Ty>>

private val TY_UNION_CACHE_KEY: Key<CachedValue<ParameterizedInferenceResult<TyUnion>>> = Key.create("TY_UNION_INFERENCE")
private val TY_CACHE_KEY: Key<CachedValue<ParameterizedInferenceResult<Ty>>> = Key.create("TY_INFERENCE")
private val TY_ANNOTATION_CACHE_KEY: Key<CachedValue<InferenceResult>> = Key.create("TY_ANNOTATION_CACHE_KEY")
private val TY_ALIAS_CACHE_KEY: Key<ParameterizedCachedValue<ParameterizedInferenceResult<Ty>, MutableSet<ElmTypeAliasDeclaration>>> = Key.create("TY_ALIAS_CACHE_KEY")
private val TY_VARIANT_CACHE_KEY: Key<CachedValue<ParameterizedInferenceResult<VariantParameters>>> = Key.create("TY_VARIANT_INFERENCE")

fun ElmTypeDeclaration.typeExpressionInference(): ParameterizedInferenceResult<TyUnion> {
    val cachedValue = CachedValuesManager.getCachedValue(this, TY_UNION_CACHE_KEY) {
        val inferenceResult = TypeExpression(this, rigidVars = false).beginTypeDeclarationInference(this)
        TypeReplacement.freeze(inferenceResult.value)
        CachedValueProvider.Result.create(inferenceResult, globalModificationTracker)
    }
    return cachedValue.copy(value = TypeReplacement.freshenVars(cachedValue.value) as TyUnion)
}

fun ElmTypeAliasDeclaration.typeExpressionInference(): ParameterizedInferenceResult<Ty> = typeExpressionInference(mutableSetOf())

private fun ElmTypeAliasDeclaration.typeExpressionInference(activeAliases: MutableSet<ElmTypeAliasDeclaration>): ParameterizedInferenceResult<Ty> {
    val cachedValue = CachedValuesManager.getManager(project).getParameterizedCachedValue(this, TY_ALIAS_CACHE_KEY, { useActiveAliases ->
        val inferenceResult = TypeExpression(this, rigidVars = false, activeAliases = useActiveAliases).beginTypeAliasDeclarationInference(this)
        TypeReplacement.freeze(inferenceResult.value)
        CachedValueProvider.Result.create(inferenceResult, globalModificationTracker)
    },  /*trackValue*/ false, /*parameter*/ activeAliases)
    return cachedValue.copy(value = TypeReplacement.freshenVars(cachedValue.value))
}


fun ElmPortAnnotation.typeExpressionInference(): ParameterizedInferenceResult<Ty> {
    val cachedValue = CachedValuesManager.getCachedValue(this, TY_CACHE_KEY) {
        val inferenceResult = TypeExpression(this, rigidVars = false).beginPortAnnotationInference(this)
        TypeReplacement.freeze(inferenceResult.value)
        CachedValueProvider.Result.create(inferenceResult, globalModificationTracker)
    }
    return cachedValue.copy(value = TypeReplacement.freshenVars(cachedValue.value))
}

fun ElmUnionVariant.typeExpressionInference(): ParameterizedInferenceResult<Ty> {
    val cachedValue = CachedValuesManager.getCachedValue(this, TY_CACHE_KEY) {
        val inferenceResult = TypeExpression(this, rigidVars = false).beginUnionConstructorInference(this)
        TypeReplacement.freeze(inferenceResult.value)
        CachedValueProvider.Result.create(inferenceResult, globalModificationTracker)
    }
    return cachedValue.copy(value = TypeReplacement.freshenVars(cachedValue.value))
}

/**
 * Get the type of the expression in this annotation, or null if the program is incomplete and no expression exists.
 *
 * @param rigid If true, all all [TyVar]s in the result ty will be rigid.
 */
fun ElmTypeAnnotation.typeExpressionInference(rigid: Boolean = true): InferenceResult? {
    val typeRef = typeExpression ?: return null

    // PSI changes inside a value declaration only invalidate the modification tracker for the
    // outer-most declaration, not the entire project. If this is a nested annotation, we need to
    // find that tracker.
    val parentModificationTracker = outermostDeclaration(strict = true)?.modificationTracker

    val cachedValue = CachedValuesManager.getCachedValue(typeRef, TY_ANNOTATION_CACHE_KEY) {
        val inferenceResult = TypeExpression(this, rigidVars = true).beginTypeRefInference(typeRef)
        val frozenResult =  inferenceResult.copy(ty = TypeReplacement.replace(inferenceResult.ty, emptyMap()))
        TypeReplacement.freeze(inferenceResult.ty)

        val trackers = when (parentModificationTracker) {
            null -> arrayOf(globalModificationTracker)
            else -> arrayOf(globalModificationTracker, parentModificationTracker)
        }

        CachedValueProvider.Result.create(frozenResult, *trackers)
    }
    // As an optimization, we don't freshen the tys here. The `flexify` call takes care of
    // freshening the inferred ty for function calls. Parameter binding needs expression types to
    // _not_ be freshened so that we can keep track of variables that reference outer scopes.
    // Non-inference usages don't care about freshness either way.
    if (!rigid) {
        return cachedValue.copy(ty = TypeReplacement.flexify(cachedValue.ty))
    }
    return cachedValue
}

/** Get the names and parameter tys for all variants of this union */
fun ElmTypeDeclaration.variantInference(): ParameterizedInferenceResult<VariantParameters> =
        CachedValuesManager.getCachedValue(this, TY_VARIANT_CACHE_KEY) {
            val inferenceResult = TypeExpression(this, rigidVars = false).beginUnionVariantsInference(this)
            CachedValueProvider.Result.create(inferenceResult, globalModificationTracker)
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
 * will be either a union type or a type alias, and will have one unique type variable for each
 * parameter. We then infer the types of the arguments and use [TypeReplacement] to replace the type
 * variables in the parameters with their arguments.
 *
 * This two step process is simpler than trying to pass arguments around while inferring
 * declarations, and allows us to cache the inference results for declarations and aliases.
 *
 * @property root The element that will be passed to a `begin*` function
 * @property rigidVars If true, any created [TyVar]s will be rigid
 * @property diagnostics The list to populate with diagnostics during inference
 * @property activeAliases The set of type alias declarations currently being inferred on the stack. Used to detect infinite recursion.
 */
class TypeExpression(
        private val root: ElmPsiElement,
        private val rigidVars: Boolean,
        private val diagnostics: MutableList<ElmDiagnostic> = mutableListOf(),
        private val activeAliases: MutableSet<ElmTypeAliasDeclaration> = mutableSetOf()
) {
    /** Cache of all type variables we've seen */
    private val varsByElement: MutableMap<ElmNamedElement, TyVar> = mutableMapOf()
    /** A subset of [varsByElement] that contains only vars declared in the current expression */
    private val expressionTypes: MutableMap<ElmPsiElement, Ty> = mutableMapOf()

    fun beginPortAnnotationInference(annotation: ElmPortAnnotation): ParameterizedInferenceResult<Ty> {
        val ty = annotation.typeExpression?.let { typeExpressionType(it) } ?: TyUnknown()
        return result(ty)
    }

    fun beginTypeRefInference(typeExpr: ElmTypeExpression): InferenceResult {
        val ty = typeExpressionType(typeExpr)
        return InferenceResult(expressionTypes, diagnostics, emptyMap(), ty)
    }

    fun beginTypeDeclarationInference(typeDeclaration: ElmTypeDeclaration): ParameterizedInferenceResult<TyUnion> {
        val ty = typeDeclarationType(typeDeclaration)
        return result(ty)
    }

    fun beginUnionVariantsInference(typeDeclaration: ElmTypeDeclaration): ParameterizedInferenceResult<VariantParameters> {
        val variants = typeDeclaration.unionVariantList.associate { it.name to unionVariantParameterTypes(it) }
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
        if (decl in activeAliases) {
            diagnostics += BadRecursionError(decl)
            return result(TyUnknown())
        }

        activeAliases += decl

        val ty = decl.typeExpression?.let { typeExpressionType(it) } ?: TyUnknown()
        val params = decl.lowerTypeNameList.map { getTyVar(it) }.toList()
        val aliasInfo = AliasInfo(decl.moduleName, decl.name, params)
        return result(ty.withAlias(aliasInfo))
    }

    private fun <T> result(value: T) = ParameterizedInferenceResult(diagnostics, value)


    /** Get the type for an entire type expression */
    private fun typeExpressionType(typeExpr: ElmTypeExpression): Ty {
        val segments = typeExpr.allSegments.map { typeSignatureDeclType(it) }
        return when (segments.size) {
            1 -> segments.last()
            else -> TyFunction(segments.dropLast(1), segments.last()).uncurry()
        }
    }

    /** Get the type for one segment of a type expression */
    private fun typeSignatureDeclType(decl: ElmTypeSignatureDeclarationTag): Ty {
        return when (decl) {
            is ElmTypeVariable -> typeVariableType(decl)
            is ElmRecordType -> recordTypeDeclType(decl)
            is ElmTupleType -> if (decl.unitExpr != null) TyUnit() else TyTuple(decl.typeExpressionList.map { typeExpressionType(it) })
            is ElmTypeRef -> typeRefType(decl)
            is ElmTypeExpression -> typeExpressionType(decl)
            else -> error("unimplemented type $decl")
        }
    }

    private fun typeVariableType(typeVar: ElmTypeVariable): Ty {
        // type variables only ever reference other vars in the same annotation or a parent
        // annotation; there's no risk of circular references.
        val ref = typeVar.reference.resolve()

        // If the var doesn't reference anything, then we can infer it's ty directly
        if (ref == null || ref == typeVar) {
            val ty = getTyVar(typeVar)
            expressionTypes[typeVar] = ty
            return ty
        }

        val cached = varsByElement[ref]
        if (cached != null) return cached

        val annotation = ref.ancestorsStrict.takeWhile { it !is ElmFile }
                .filterIsInstance<ElmTypeAnnotation>()
                .firstOrNull()
        val expr = annotation?.typeExpression

        // If the reference is to a variable not declared in an annotation, or to another variable
        // in the same annotation we're already working on, we use the ty of the reference.
        if (annotation == null || expr == null || expr == root) {
            val ty = getTyVar(ref)
            varsByElement[typeVar] = ty
            expressionTypes[typeVar] = ty
            return ty
        }

        // If the reference is to a variable declared in a parent annotation, we need to use the ty
        // from that annotation's inference
        val ty = annotation.typeExpressionInference(rigid = true)
                ?.expressionTypes?.get(ref) ?: TyUnknown()
        if (ty is TyVar) {
            varsByElement[ref] = ty
        }

        expressionTypes[typeVar] = ty
        return ty
    }

    private fun recordTypeDeclType(record: ElmRecordType): TyRecord {
        val fieldElements = record.fieldTypeList
        if (fieldElements.isEmpty()) return TyRecord.emptyRecord
        val fieldTys = fieldElements.associate { it.name to typeExpressionType(it.typeExpression) }
        val fieldReferences = RecordFieldReferenceTable.fromElements(fieldElements)
        val baseId = record.baseTypeIdentifier
        val baseTy = baseId?.reference?.resolve()?.let { getTyVar(it) } ?: baseId?.let { TyVar(it.referenceName) }
        return TyRecord(fieldTys, baseTy, fieldReferences = fieldReferences)
    }

    private fun typeRefType(typeRef: ElmTypeRef): Ty {
        val args = typeRef.allArguments.map { typeSignatureDeclType(it) }

        val declaredTy = when (val ref = typeRef.reference.resolve()) {
            is ElmTypeAliasDeclaration -> ref.typeExpressionInference(activeAliases).value
            is ElmTypeDeclaration -> ref.typeExpressionInference().value
            // In 0.19, unlike all other built-in types, Elm core doesn't define the List type anywhere, so the
            // reference won't resolve. So we check for a reference to that type here. Note that users can
            // create their own List types that shadow the built-in, so we only want to do this check if the
            // reference is null.
            null -> {
                when (typeRef.referenceName) {
                    "List" -> TyList(TyVar("a"))
                    else -> TyUnknown()
                }
            }
            else -> error(typeRef, "Unexpected type reference")
        }

        // This cast is safe, since parameters of type declarations are always inferred as TyVar.
        // We know the parameters haven't been replaced yet, since we just created the ty ourselves.
        @Suppress("UNCHECKED_CAST")
        val params = when {
            declaredTy.alias != null -> declaredTy.alias!!.parameters
            declaredTy is TyUnion -> declaredTy.parameters
            else -> emptyList()
        } as List<TyVar>

        if (isInferable(declaredTy) && params.size != args.size) {
            diagnostics += TypeArgumentCountError(typeRef, args.size, params.size)
            return TyUnknown()
        }

        if (params.isEmpty()) {
            return declaredTy
        }

        return TypeReplacement.replace(declaredTy, params.zip(args).toMap())
    }

    private fun typeDeclarationType(declaration: ElmTypeDeclaration): TyUnion {
        val params = declaration.lowerTypeNameList.map { getTyVar(it) }
        return TyUnion(declaration.moduleName, declaration.name, params)
    }

    private fun unionVariantParameterTypes(variant: ElmUnionVariant): List<Ty> {
        return variant.allParameters.map { typeSignatureDeclType(it) }.toList()
    }

    private fun getTyVar(element: ElmNamedElement) = varsByElement.getOrPut(element) {
        TyVar(element.name!!, rigid = rigidVars)
    }
}
