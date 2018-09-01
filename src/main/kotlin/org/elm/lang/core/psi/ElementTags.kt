package org.elm.lang.core.psi

import org.elm.lang.core.psi.elements.*

/** An element that can appear in the parameter list of an [ElmUnionMember] */
interface ElmUnionMemberParameterTag : ElmPsiElement

/** An element that can occur in an [ElmExpression]; either an operator or operand */
interface ElmExpressionPartTag : ElmPsiElement

/** An element that can occur in an [ElmExpression] as the argument to a function or operator */
interface ElmOperandTag : ElmExpressionPartTag

/** An element that can be the parameter of an [ElmFunctionDeclarationLeft], [ElmAnonymousFunction],
 * or [ElmCaseOfBranch] */
interface ElmNameDeclarationPatternTag : ElmNamedElement

/** An element that is either an [ElmFunctionParamTag], a [ElmPatternChildTag], or both. No elements implement this directly. */
interface ElmFunctionParamOrPatternChildTag : ElmPsiElement

/** An element that can be a top-level parameter to a [ElmFunctionDeclarationLeft] or [ElmOperatorDeclarationLeft] */
interface ElmFunctionParamTag : ElmFunctionParamOrPatternChildTag

/** An element that can be the direct child of an [ElmPattern] */
interface ElmPatternChildTag : ElmFunctionParamOrPatternChildTag

/** An element that can be the direct child of an [ElmConsPattern] */
interface ElmConsPatternChildTag : ElmPsiElement

/** An element that is either an [ElmTypeRefParameterTag], a [ElmParametricTypeRefParameterTag], or both. No elements implement this directly. */
interface ElmTypeRefOrParametricTypeRefParameterTag: ElmPsiElement

/** An element that can be a parameter of an [ElmTypeRef], but not necessarily an [ElmParametricTypeRef] */
interface ElmTypeRefParameterTag : ElmTypeRefOrParametricTypeRefParameterTag

/** An element that can be a parameter of an [ElmTypeRef] or an [ElmParametricTypeRef] */
interface ElmParametricTypeRefParameterTag : ElmTypeRefOrParametricTypeRefParameterTag
