package org.elm.lang.core.psi

import org.elm.lang.core.psi.elements.*
/**
 * An element that is at least one of [ElmUnionVariantParameterTag], [ElmTypeExpressionSegmentTag],
 * or [ElmTypeRefArgumentTag].
 *
 * No elements implement this directly.
 */
interface ElmTypeSignatureDeclarationTag: ElmPsiElement

/** An element that can appear in the parameter list of an [ElmUnionVariant] */
interface ElmUnionVariantParameterTag : ElmTypeSignatureDeclarationTag

/** An Elm expression; either a chain of binary operators, a function call, or an atom */
interface ElmExpressionTag : ElmPsiElement

/** An element that can occur on its own or as an argument to a function or operator */
interface ElmAtomTag : ElmExpressionTag, ElmOperandTag

/** An element that can occur as an argument to an operator */
interface ElmOperandTag : ElmPsiElement, ElmBinOpPartTag

/** An element that can occur in a binary operator expression */
interface ElmBinOpPartTag : ElmPsiElement

/** An element that can be the parameter of an [ElmFunctionDeclarationLeft], [ElmAnonymousFunctionExpr], or [ElmCaseOfBranch] */
interface ElmNameDeclarationPatternTag : ElmNameIdentifierOwner

/** A function being called as the child of a [ElmFunctionCallExpr] */
interface ElmFunctionCallTargetTag : ElmAtomTag

/** An element that is either an [ElmFunctionParamTag], a [ElmPatternChildTag], or both. No elements implement this directly. */
interface ElmFunctionParamOrPatternChildTag : ElmPsiElement

/** An element that can be a top-level parameter to a [ElmFunctionDeclarationLeft] or [ElmOperatorDeclarationLeft] */
interface ElmFunctionParamTag : ElmFunctionParamOrPatternChildTag

/** An element that can be the direct child of an [ElmPattern] */
interface ElmPatternChildTag : ElmFunctionParamOrPatternChildTag

/** A pattern that can appear as the argument list of a UnionPattern */
interface ElmUnionPatternChildTag : ElmPsiElement

/** An element that can be a parameter of an [ElmTypeExpression]*/
interface ElmTypeExpressionSegmentTag : ElmTypeSignatureDeclarationTag

/** An element that can be an argument to an [ElmTypeRef] */
interface ElmTypeRefArgumentTag : ElmTypeSignatureDeclarationTag

/** A value literal. Either a number, string, or char. */
interface ElmConstantTag : ElmAtomTag, ElmFunctionParamTag, ElmPatternChildTag, ElmUnionPatternChildTag

/** An element which can be the target of a field access expression. */
interface ElmFieldAccessTargetTag : ElmPsiElement

/** An element which can occur in a module or import's exposing list */
interface ElmExposedItemTag : ElmPsiElement

/** A named declaration which can be exposed by a module */
interface ElmExposableTag : ElmPsiElement, ElmNameIdentifierOwner

/** The element on the left side of the `=` in a value declaration */
interface ElmValueAssigneeTag : ElmPsiElement
