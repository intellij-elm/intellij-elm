package org.elm.lang.core.types

import org.elm.lang.core.diagnostics.ElmDiagnostic
import org.elm.lang.core.diagnostics.TypeArgumentCountError
import org.elm.lang.core.psi.ElmNamedElement
import org.elm.lang.core.psi.ElmPsiElement
import org.elm.lang.core.psi.ElmTypeSignatureDeclarationTag
import org.elm.lang.core.psi.elements.*
import org.elm.lang.core.psi.parentOfType

/**
 * Inference for type declarations and expressions like function annotations and constructor calls.
 *
 * For inference of value declarations and expressions, see [InferenceScope]
 */
class TypeExpression private constructor(
        private val subs: SubstitutionTable,
        private val diagnostics: MutableList<ElmDiagnostic> = mutableListOf()
) {
    companion object {
        fun inferPortAnnotation(annotation: ElmPortAnnotation): InferenceResult {
            val scope = TypeExpression(SubstitutionTable.empty())
            val ty = annotation.typeRef?.let { scope.typeRefType(it) } ?: TyUnknown
            return scope.result(ty)
        }

        fun inferTypeRef(typeRef: ElmTypeRef): InferenceResult {
            val scope = TypeExpression(SubstitutionTable.empty())
            val ty = scope.typeRefType(typeRef)
            return scope.result(ty)
        }

        fun inferTypeDeclaration(typeDeclaration: ElmTypeDeclaration): InferenceResult {
            val scope = TypeExpression(SubstitutionTable.empty())
            val ty = scope.typeDeclarationType(typeDeclaration, null)
            return scope.result(ty)
        }

        fun inferUnionConstructor(member: ElmUnionMember): InferenceResult {
            val scope = TypeExpression(SubstitutionTable.empty())
            val decl = member.parentOfType<ElmTypeDeclaration>()
                    ?.let { scope.typeDeclarationType(it, null) }
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
            val scope = TypeExpression(SubstitutionTable.empty())
            val ty = scope.typeAliasDeclarationType(decl, null)
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
            is ElmTypeVariableRef -> subs.resolve(TyVar(decl.identifier.text))
            is ElmRecordType -> recordTypeDeclType(decl, null)
            is ElmTupleType -> if (decl.unit != null) TyUnit else TyTuple(decl.typeRefList.map { typeRefType(it) })
            is ElmParametricTypeRef -> parametricTypeRefType(decl)
            is ElmTypeRef -> typeRefType(decl)
            else -> error("unimplemented type $decl")
        }
    }

    private fun recordTypeDeclType(record: ElmRecordType, alias: TyUnion?): TyRecord {
        val declaredFields = record.fieldTypeList.associate { it.lowerCaseIdentifier.text to typeRefType(it.typeRef) }
        val baseName = record.baseTypeIdentifier?.referenceName
        val baseTy = subs.tysByName[baseName]
        // TODO diagnostic if base is not a record
        val baseFields = (baseTy as? TyRecord)?.fields.orEmpty()
        return TyRecord(baseFields + declaredFields, baseName, alias)
    }

    private fun parametricTypeRefType(typeRef: ElmParametricTypeRef): Ty {
        val ref = typeRef.reference.resolve()
        val args = typeRef.allParameters.map { typeSignatureDeclType(it) }.toList()
        val caller = Caller(typeRef, args)
        // Unlike all other built-in types, Elm core doesn't define the List type anywhere, so the
        // reference won't resolve. So we check for reference to that type here. Note that users can
        // create their own List types that shadow the built-in, so we only want to do this check if the
        // reference is null.
        if (ref == null && typeRef.upperCaseQID.text == "List") {
            return TyList(replaceParamsWithArgs(listOf(TyVar("a")), caller).first())
        }

        return resolvedTypeRefType(ref, caller)
    }

    private fun upperPathTypeRefType(typeRef: ElmUpperPathTypeRef): Ty {
        val ref = typeRef.reference.resolve()
        if (ref == null && typeRef.upperCaseQID.text == "List") {
            diagnostics += TypeArgumentCountError(typeRef, 0, 1)
            return TyList(TyVar("a"))
        }
        return resolvedTypeRefType(ref, Caller(typeRef, emptyList()))
    }

    private fun resolvedTypeRefType(ref: ElmNamedElement?, caller: Caller): Ty {
        return when (ref) {
            is ElmTypeAliasDeclaration -> typeAliasDeclarationType(ref, caller)
            is ElmTypeDeclaration -> typeDeclarationType(ref, caller)
            // We only get here if the reference doesn't resolve. We could create a TyUnion from the
            // ref name, but we don't know what module it's supposed to be defined in, so that would
            // lead to false positives.
            else -> TyUnknown
        }
    }

    private fun typeAliasDeclarationType(decl: ElmTypeAliasDeclaration, caller: Caller?): Ty {
        val record = decl.aliasedRecord
        val params = decl.lowerTypeNameList.map { TyVar(it.name) }.toList()
        val childTable = SubstitutionTable.fromVars(params, subs.resolveAll(caller?.args ?: emptyList()))
        val childScope = TypeExpression(childTable, diagnostics)

        if (record != null) {
            val aliasTy = TyUnion(decl.moduleName, decl.upperCaseIdentifier.text, replaceParamsWithArgs(params, caller))
            return childScope.recordTypeDeclType(record, aliasTy)
        }
        return decl.typeRef?.let { childScope.typeRefType(it) } ?: return TyUnknown
    }

    private fun typeDeclarationType(declaration: ElmTypeDeclaration, caller: Caller?): Ty {
        val params = declaration.lowerTypeNameList.map { TyVar(it.name) }
        val resolvedCaller = caller?.let { it.copy(args = subs.resolveAll(it.args)) }
        val actualParams = replaceParamsWithArgs(params, resolvedCaller)
        return TyUnion(declaration.moduleName, declaration.name, actualParams)
    }

    private fun replaceParamsWithArgs(params: List<Ty>, caller: Caller?): List<Ty> {
        return when {
            caller != null && params.size == caller.args.size -> caller.args
            else -> {
                if (caller != null) {
                    diagnostics += TypeArgumentCountError(caller.element, caller.args.size, params.size)
                }
                params
            }
        }
    }
}

/**
 * Keeps track of the current type of type variables, and can substitute a variable for it's current type.
 *
 * @property tysByName A map of the names of TyVars present in parameters to the types of arguments
 *   provided to those parameters.
 */
private data class SubstitutionTable(val tysByName: Map<String, Ty>) {
    companion object {
        fun fromVars(vars: List<TyVar>, args: List<Ty>?) =
                SubstitutionTable(vars.zip(args ?: emptyList()).associate { (v, t) -> v.name to t })

        fun empty() = SubstitutionTable(emptyMap())
    }

    fun resolve(it: Ty): Ty = (it as? TyVar)?.let { tysByName[it.name] } ?: it
    fun resolveAll(params: List<Ty>): List<Ty> = params.map { resolve(it) }
}

private data class Caller(val element: ElmPsiElement, val args: List<Ty>)
