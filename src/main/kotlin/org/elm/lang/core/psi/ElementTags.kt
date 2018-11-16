package org.elm.lang.core.psi

import org.elm.lang.core.psi.elements.*
/**
 * An element that is at least one of [ElmUnionMemberParameterTag], [ElmTypeRefSegmentTag],
 * or [ElmParametricTypeRefParameterTag].
 *
 * No elements implement this directly.
 */
interface ElmTypeSignatureDeclarationTag: ElmPsiElement

/** An element that can appear in the parameter list of an [ElmUnionMember] */
interface ElmUnionMemberParameterTag : ElmTypeSignatureDeclarationTag

/** An element that can occur in an [ElmExpression]; either an operator or operand */
interface ElmExpressionPartTag : ElmPsiElement

/** An element that can occur in an [ElmExpression] as the argument to a function or operator */
interface ElmOperandTag : ElmExpressionPartTag

/** An element that can be the parameter of an [ElmFunctionDeclarationLeft], [ElmAnonymousFunction], or [ElmCaseOfBranch] */
interface ElmNameDeclarationPatternTag : ElmNamedElement

/** A function being called as the child of a [ElmFunctionCall] */
interface ElmFunctionCallTargetTag : ElmOperandTag

/** An element that is either an [ElmFunctionParamTag], a [ElmPatternChildTag], or both. No elements implement this directly. */
interface ElmFunctionParamOrPatternChildTag : ElmPsiElement

/** An element that can be a top-level parameter to a [ElmFunctionDeclarationLeft] or [ElmOperatorDeclarationLeft] */
interface ElmFunctionParamTag : ElmFunctionParamOrPatternChildTag

/** An element that can be the direct child of an [ElmPattern] */
interface ElmPatternChildTag : ElmFunctionParamOrPatternChildTag

/** A pattern that can appear as the argument list of a UnionPattern */
interface ElmUnionPatternChildTag : ElmPsiElement

/** An element that can be a parameter of an [ElmTypeRef], but not necessarily an [ElmParametricTypeRef] */
interface ElmTypeRefSegmentTag : ElmTypeSignatureDeclarationTag

/** An element that can be a parameter of an [ElmTypeRef] or an [ElmParametricTypeRef] */
interface ElmParametricTypeRefParameterTag : ElmTypeSignatureDeclarationTag

/** A value literal. Either a number, string, or char. */
interface ElmConstantTag : ElmOperandTag, ElmFunctionParamTag, ElmPatternChildTag, ElmUnionPatternChildTag
