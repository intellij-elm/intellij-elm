package org.elm.lang.core.types

import com.intellij.psi.util.PsiTreeUtil
import org.elm.lang.core.psi.*
import org.elm.lang.core.psi.elements.*

data class InferrenceResult(private val bindings: Map<ElmNamedElement, Ty>) {
    fun bindingType(element: ElmNamedElement): Ty = bindings[element] ?: TyUnknown
}

fun ElmValueDeclaration.bindParameterTypes(): Map<ElmNamedElement, Ty> {
    if (pattern != null) {
        return PsiTreeUtil.collectElementsOfType(pattern, ElmLowerPattern::class.java)
                .associate { it to TyUnknown } // TODO
    }

    val decl = functionDeclarationLeft!!
    val types = typeAnnotation?.typeRef?.allParameters
            ?: return decl.namedParameters.associate { it to TyUnknown }

    return decl.patterns.zip(types).map { (pat, type) ->
        bindPattern(pat, type)
    }.reduce { acc, it -> acc + it }
}

private fun bindPattern(pat: ElmFunctionParamOrPatternChildTag, anno: ElmTypeRefParameterTag): Map<ElmNamedElement, Ty> {
    return when (pat) {
        is ElmAnythingPattern -> emptyMap()
        is ElmConsPattern -> emptyMap() // This is a partial pattern error in function parameters
        is ElmListPattern -> emptyMap() // This is a partial pattern error in function parameters
        is ElmLiteral -> emptyMap() // This is a partial pattern error in function parameters
        is ElmPattern -> bindPattern(pat.child, anno) + bindPattern(pat.patternAs, anno)
        is ElmLowerPattern -> mapOf(pat to anno.ty)
        is ElmRecordPattern -> bindPattern(pat, anno)
        is ElmTuplePattern -> bindPattern(pat, anno)
        is ElmUnit -> emptyMap()
        else -> error("unexpected type $pat")
    }
}

private fun bindPattern(pat: ElmPatternAs?, anno: ElmTypeRefParameterTag): Map<ElmNamedElement, Ty> {
    return when (pat) {
        null -> emptyMap()
        else -> mapOf(pat to anno.ty)
    }
}

private fun bindPattern(pat: ElmTuplePattern, anno: ElmTypeRefParameterTag): Map<ElmNamedElement, Ty> {
    val type: ElmTupleType = when (anno) {
        is ElmTupleType -> anno
        is ElmTypeRef -> anno.allParameters.singleOrNull() as? ElmTupleType ?: return emptyMap()
        else -> return emptyMap() // TODO: report error
    }
    return pat.patternList.zip(type.typeRefList)
            .map { (pat, type) -> bindPattern(pat.child, type) }
            .reduce { acc, it -> acc + it }
}

private fun bindPattern(pat: ElmRecordPattern, anno: ElmTypeRefParameterTag): Map<ElmNamedElement, Ty> {
    if (anno !is ElmRecordType) return emptyMap() // TODO: report error
    val names = pat.lowerPatternList.map { it.name to it }
    val fields = anno.fieldTypeList.associate { it.lowerCaseIdentifier.text to it.typeRef.ty }
    return names.associate { (name, e) -> e to fields[name]!! } // TODO: report error on name mismatch
}

/** Get the type for one part of a type ref */
private val ElmTypeRefOrParametricTypeRefParameterTag.ty: Ty
    get() = when (this) {
        is ElmUpperPathTypeRef -> TyPrimitive(text)
        is ElmTypeVariableRef -> TyVar(identifier.text, null) // TODO
        is ElmRecordType -> TyRecord(fieldTypeList.map { it.lowerCaseIdentifier.text to it.typeRef.ty })
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
