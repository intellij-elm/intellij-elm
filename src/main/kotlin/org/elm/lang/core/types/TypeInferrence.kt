package org.elm.lang.core.types

import com.intellij.psi.util.PsiTreeUtil
import org.elm.lang.core.psi.ElmFunctionParamOrPatternChildTag
import org.elm.lang.core.psi.ElmNamedElement
import org.elm.lang.core.psi.ElmTypeRefOrParametricTypeRefParameterTag
import org.elm.lang.core.psi.elements.*

val ElmValueDeclaration.inference: InferenceResult
    get () { // TODO cache
        val ctx = ValueDeclarationInferenceContext()
        ctx.bindParameters(this)
        return InferenceResult(ctx.bindings)
    }

private class ValueDeclarationInferenceContext {
    val bindings: MutableMap<ElmNamedElement, Ty> = mutableMapOf()
    var ty: Ty = TyUnknown
        private set

    fun bindParameters(valueDeclaration: ElmValueDeclaration) {
        if (valueDeclaration.pattern != null) {
            bindings += PsiTreeUtil.collectElementsOfType(valueDeclaration.pattern, ElmLowerPattern::class.java)
                    .associate { it to TyUnknown } // TODO
            return
        }

        val decl = valueDeclaration.functionDeclarationLeft!!
        val typeRef = valueDeclaration.typeAnnotation?.typeRef
        val types = typeRef?.allParameters

        if (typeRef == null || types == null) {
            bindings += decl.namedParameters.associate { it to TyUnknown }
            return
        }

        ty = typeRef.ty
        decl.patterns.zip(types).forEach { (pat, type) -> bindPattern(this, pat, type.ty) }
    }
}


data class InferenceResult(private val bindings: Map<ElmNamedElement, Ty>) {
    fun bindingType(element: ElmNamedElement): Ty = bindings[element] ?: TyUnknown
}

private fun bindPattern(ctx: ValueDeclarationInferenceContext, pat: ElmFunctionParamOrPatternChildTag, ty: Ty) {
    return when (pat) {
        is ElmAnythingPattern -> {
        }
        is ElmConsPattern -> {
        } // TODO This is a partial pattern error in function parameters
        is ElmListPattern -> {
        } // This is a partial pattern error in function parameters
        is ElmLiteral -> {
        } // This is a partial pattern error in function parameters
        is ElmPattern -> {
            bindPattern(ctx, pat.child, ty)
            bindPattern(ctx, pat.patternAs, ty)
        }
        is ElmLowerPattern -> ctx.bindings[pat] = ty
        is ElmRecordPattern -> bindPattern(ctx, pat, ty)
        is ElmTuplePattern -> bindPattern(ctx, pat, ty)
        is ElmUnit -> {
        }
        else -> error("unexpected type $pat")
    }
}

private fun bindPattern(ctx: ValueDeclarationInferenceContext, pat: ElmPatternAs?, ty: Ty) {
    if (pat != null) ctx.bindings[pat] = ty
}

private fun bindPattern(ctx: ValueDeclarationInferenceContext, pat: ElmTuplePattern, ty: Ty) {
    if (ty !is TyTuple) return // TODO: report error
    pat.patternList
            .zip(ty.types)
            .forEach { (pat, type) -> bindPattern(ctx, pat.child, type) }
}

private fun bindPattern(ctx: ValueDeclarationInferenceContext, pat: ElmRecordPattern, ty: Ty) {
    if (ty !is TyRecord) return // TODO: report error
    for (id in pat.lowerPatternList) {
        val fieldTy = ty.fields[id.name] ?: continue // TODO: report error
        bindPattern(ctx, id, fieldTy)
    }
}

/** Get the type for one part of a type ref */
private val ElmTypeRefOrParametricTypeRefParameterTag.ty: Ty
    get() = when (this) {
        is ElmUpperPathTypeRef -> TyPrimitive(text)
        is ElmTypeVariableRef -> TyVar(identifier.text, null) // TODO
        is ElmRecordType -> TyRecord(fieldTypeList.associate { it.lowerCaseIdentifier.text to it.typeRef.ty })
        is ElmTupleType -> if (unit != null) TyUnit else TyTuple(typeRefList.map { it.ty })
        is ElmParametricTypeRef -> TyParametric(upperCaseQID.text, allParameters.map { it.ty }.toList())
        is ElmTypeRef -> ty
        else -> error("unimplemented type $this")
    }

private val ElmTypeRef.ty: Ty
    get() {
        val params = allParameters.toList()
        return when {
            params.size == 1 -> allParameters.first().ty
            else -> TyFunction(params.dropLast(1).map { it.ty }, params.last().ty)
        }
    }
