package org.elm.lang.core.types

import com.intellij.psi.util.PsiTreeUtil
import org.elm.lang.core.psi.ElmNamedElement
import org.elm.lang.core.psi.elements.*
import org.elm.lang.core.psi.tags.ElmFunctionParamTag
import org.elm.lang.core.psi.tags.ElmParametricTypeRefParameterTag
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
        bindAnnotationPattern(pat, type) // TODO pat.patternAs
    }.reduce { acc, it -> acc + it }
}

private fun bindAnnotationPattern(pat: ElmFunctionParamTag, anno: ElmTypeRefParameterTag): Map<ElmNamedElement, Ty> {
    return when (pat) {
        is ElmAnythingPattern -> emptyMap()
        is ElmListPattern -> emptyMap() // This is a partial pattern error
        is ElmPattern -> bindAnnotationPattern(pat, anno)
        is ElmLiteral -> emptyMap() // This is a partial pattern error
        is ElmUnit -> emptyMap()
        is ElmLowerPattern -> mapOf(pat to anno.ty)
        is ElmRecordPattern -> bindRecordAnnotationPattern(pat, anno)
        is ElmTuplePattern -> bindTupleAnnotationPattern(pat, anno)
        else -> error("unexpected type $pat")
    }
}

private fun bindTupleAnnotationPattern(pat: ElmTuplePattern, anno: ElmTypeRefParameterTag): Map<ElmNamedElement, Ty> {
    if (anno !is ElmTupleType) return emptyMap() // TODO: report error
    return pat.patternList.zip(anno.typeRefList).map { (pat, type) ->
        bindAnnotationPattern(pat, type)
    }.reduce { acc, it -> acc + it }
}

private fun bindRecordAnnotationPattern(pat: ElmRecordPattern, anno: ElmTypeRefParameterTag): Map<ElmNamedElement, Ty> {
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
        else -> error("unexpected type ${this}")
    }

private val ElmParametricTypeRefParameterTag.ty: Ty
    get() = when (this) {
        is ElmUpperPathTypeRef -> ty
        is ElmTypeVariableRef -> ty
        is ElmRecordType -> ty
        is ElmTupleType -> ty
        is ElmTypeRef -> ty
        else -> error("unexpected type ${this}")
    }


private val ElmUpperPathTypeRef.ty: Ty get() = TyPrimitive(text)
private val ElmTypeVariableRef.ty: Ty get() = TyVar(null) // TODO
private val ElmRecordType.ty: Ty get() = TyRecord(fieldTypeList.map { it.lowerCaseIdentifier.text to it.typeRef.ty })
private val ElmTupleType.ty: Ty get() = TyTuple(typeRefList.map { it.ty })
private val ElmParametricTypeRef.ty: Ty get() = TyParametric(allParameters.map { it.ty }.toList())
private val ElmTypeRef.ty: Ty
    get() {
        val params = allParameters.toList()
        return when {
            params.size == 1 -> allParameters.first().ty
            else -> TyFunction(params.dropLast(1).map { it.ty }, params.last().ty)
        }
    }
