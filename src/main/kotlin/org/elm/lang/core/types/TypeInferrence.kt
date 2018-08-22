package org.elm.lang.core.types

import com.intellij.psi.util.PsiTreeUtil
import org.elm.lang.core.psi.ElmNamedElement
import org.elm.lang.core.psi.elements.*
import org.elm.lang.core.psi.tags.ElmFunctionParamTag
import org.elm.lang.core.psi.tags.ElmParametricTypeRefParameterTag
import org.elm.lang.core.psi.tags.ElmPatternChildTag
import org.elm.lang.core.psi.tags.ElmTypeRefParameterTag
import org.elm.lang.core.types.ty.*

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

private fun bindPattern(pat: ElmFunctionParamTag, anno: ElmTypeRefParameterTag): Map<ElmNamedElement, Ty> {
    return when (pat) {
        is ElmAnythingPattern -> bindPattern(pat, anno)
        is ElmListPattern -> bindPattern(pat, anno)
        is ElmPattern -> bindPattern(pat, anno)
        is ElmLiteral -> bindPattern(pat, anno)
        is ElmLowerPattern -> bindPattern(pat, anno)
        is ElmRecordPattern -> bindPattern(pat, anno)
        is ElmTuplePattern -> bindPattern(pat, anno)
        is ElmUnit -> bindPattern(pat, anno)
        else -> error("unexpected type $pat")
    }
}

private fun bindPattern(pat: ElmPatternChildTag, anno: ElmTypeRefParameterTag): Map<ElmNamedElement, Ty> {
    return when (pat) {
        is ElmAnythingPattern -> bindPattern(pat, anno)
        is ElmConsPattern -> bindPattern(pat, anno)
        is ElmListPattern -> bindPattern(pat, anno)
        is ElmPattern -> bindPattern(pat, anno)
        is ElmLiteral -> bindPattern(pat, anno)
        is ElmLowerPattern -> bindPattern(pat, anno)
        is ElmRecordPattern -> bindPattern(pat, anno)
        is ElmTuplePattern -> bindPattern(pat, anno)
        is ElmUnit -> bindPattern(pat, anno)
        else -> error("unexpected type $pat")
    }
}

private fun bindPattern(pat: ElmAnythingPattern, anno: ElmTypeRefParameterTag): Map<ElmNamedElement, Ty> = emptyMap()
private fun bindPattern(pat: ElmConsPattern, anno: ElmTypeRefParameterTag): Map<ElmNamedElement, Ty> = emptyMap() // This is a partial pattern error in function parameters
private fun bindPattern(pat: ElmListPattern, anno: ElmTypeRefParameterTag): Map<ElmNamedElement, Ty> = emptyMap() // This is a partial pattern error in function parameters
private fun bindPattern(pat: ElmLiteral, anno: ElmTypeRefParameterTag): Map<ElmNamedElement, Ty> = emptyMap() // This is a partial pattern error in function parameters
private fun bindPattern(pat: ElmLowerPattern, anno: ElmTypeRefParameterTag): Map<ElmNamedElement, Ty> = mapOf(pat to anno.ty)
private fun bindPattern(pat: ElmUnit, anno: ElmTypeRefParameterTag): Map<ElmNamedElement, Ty> = emptyMap()

private fun bindPattern(pat: ElmPattern, anno: ElmTypeRefParameterTag): Map<ElmNamedElement, Ty> {
    return bindPattern(pat.child, anno) + bindPattern(pat.patternAs, anno)
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
private val ElmTypeRefParameterTag.ty: Ty
    get() = when (this) {
        is ElmUpperPathTypeRef -> ty
        is ElmTypeVariableRef -> ty
        is ElmRecordType -> ty
        is ElmTupleType -> ty
        is ElmParametricTypeRef -> ty
        is ElmTypeRef -> ty
        else -> error("unimplemented type $this")
    }

private val ElmParametricTypeRefParameterTag.ty: Ty
    get() = when (this) {
        is ElmUpperPathTypeRef -> ty
        is ElmTypeVariableRef -> ty
        is ElmRecordType -> ty
        is ElmTupleType -> ty
        is ElmTypeRef -> ty
        else -> error("unimplemented type $this")
    }


private val ElmUpperPathTypeRef.ty: Ty get() = TyPrimitive(text)
private val ElmTypeVariableRef.ty: Ty get() = TyVar(identifier.text, null) // TODO
private val ElmRecordType.ty: Ty get() = TyRecord(fieldTypeList.map { it.lowerCaseIdentifier.text to it.typeRef.ty })
private val ElmTupleType.ty: Ty get() = if (unit != null) TyUnit else TyTuple(typeRefList.map { it.ty })
private val ElmParametricTypeRef.ty: Ty get() = TyParametric(upperCaseQID.text, allParameters.map { it.ty }.toList())
private val ElmTypeRef.ty: Ty
    get() {
        val params = allParameters.toList()
        return when {
            params.size == 1 -> allParameters.first().ty
            else -> TyFunction(params.dropLast(1).map { it.ty }, params.last().ty)
        }
    }
