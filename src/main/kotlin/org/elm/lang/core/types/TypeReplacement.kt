package org.elm.lang.core.types

import com.intellij.psi.PsiElement
import org.elm.lang.core.diagnostics.ElmDiagnostic
import org.elm.lang.core.diagnostics.RecordBaseTypeError
import org.elm.lang.core.diagnostics.TypeArgumentCountError

/**
 * This class performs deep replacement of a set of [TyVar]s in a [Ty] with a set of new types,
 * which could also be [TyVar]s.
 *
 * It relies on the fact that [TyVar]s can be compared by identity.
 */
class TypeReplacement private constructor(
        private val replacements: Map<TyVar, Pair<PsiElement, Ty>>
) {
    companion object {
        /**
         * Replace types in a ty that is the result of a parameterized type reference.
         *
         * @param element the reference element that [ty] was inferred from
         * @param ty the type to perform replacement in
         * @param paramTys the parameters of [ty] that will be replaced
         * @param argTys the arguments that will replaces the parameters.
         * @param argElements the PsiElements that [argTys] were inferred from. Must be the same size as [argTys].
         */
        fun replaceCall(
                element: PsiElement,
                ty: Ty,
                paramTys: List<TyVar>,
                argTys: List<Ty>,
                argElements: List<PsiElement>
        ): InferenceResult {
            require(argTys.size == argElements.size) { "mismatched arg sizes ${argTys.size} != ${argElements.size}" }

            if (paramTys.size != argTys.size) {
                val error = TypeArgumentCountError(element, argTys.size, paramTys.size)
                return InferenceResult(emptyMap(), listOf(error), TyUnknown())
            }

            if (paramTys.isEmpty()) {
                return InferenceResult(emptyMap(), emptyList(), ty)
            }

            val replacements = paramTys.indices.associate { i -> paramTys[i] to (argElements[i] to argTys[i]) }
            val typeReplacement = TypeReplacement(replacements)
            val newTy = typeReplacement.replace(ty)
            return InferenceResult(emptyMap(), typeReplacement.diagnostics, newTy)
        }
    }

    private val diagnostics = mutableListOf<ElmDiagnostic>()

    private fun replace(ty: Ty): Ty = when (ty) {
        is TyVar -> replacements[ty]?.second ?: ty
        is TyTuple -> TyTuple(ty.types.map { replace(it) })
        is TyUnion -> TyUnion(ty.module, ty.name, ty.parameters.map { replace(it) })
        is TyFunction -> TyFunction(ty.parameters.map { replace(it) }, replace(ty.ret))
        is TyRecord -> replaceRecord(ty)
        is TyUnit, is TyUnknown, TyInProgressBinding -> ty
    }

    private fun replaceRecord(ty: TyRecord): Ty {
        val baseTy = replacements[ty.baseTy]?.second
        val baseFields = (baseTy as? TyRecord)?.fields.orEmpty()

        if (baseTy != null && isInferable(baseTy) && baseTy !is TyRecord) {
            diagnostics += RecordBaseTypeError(replacements[ty.baseTy]!!.first, baseTy)
        }

        val declaredFields = ty.fields.mapValues { (_, it) -> replace(it) }
        val alias = ty.alias?.let { replace(it) as TyUnion }
        val newBaseTy = (baseTy as? TyRecord)?.baseTy
        return TyRecord(baseFields + declaredFields, newBaseTy, alias)
    }
}

