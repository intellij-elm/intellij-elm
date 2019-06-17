package org.elm.lang.core.types

import org.elm.lang.core.psi.OperatorAssociativity
import org.elm.lang.core.psi.OperatorAssociativity.*

data class OperatorPrecedence(val precedence: Int, val associativity: OperatorAssociativity)

@Suppress("unused") // The type parameter on BinaryExprTree isn't used directly, but allows us to keep nodes consistent.
sealed class BinaryExprTree<T : Any> {
    data class Operand<T : Any>(val operand: T) : BinaryExprTree<T>()
    data class Binary<T : Any>(val left: BinaryExprTree<T>, val operator: T, val right: BinaryExprTree<T>) : BinaryExprTree<T>()

    companion object {
        private const val DEFAULT_PRECEDENCE = -1
        /**
         * Parse a list of operands and operators into a binary tree structured in evaluation
         * order based on precedence and associativity.
         *
         * Note that it is the caller's responsibility to ensure that all functions in the
         * [expression] have an associativity of [LEFT] or [RIGHT] (not [NON]) if there is more than
         * one operator, and that [expression] and [operatorPrecedences] are in the correct format.
         *
         * @param expression A list of [Ty]s representing an expression. The list must have odd length,
         *   and all odd-indexed values must be an operator. All even-indexed values must be operands.
         * @param operatorPrecedences operator precedence information for the operators in
         *   [expression]. Every function in [expression] must have an entry.
         */
        fun <T : Any> parse(expression: List<T>, operatorPrecedences: Map<out T, OperatorPrecedence>): BinaryExprTree<T> {
            return parseExpression(expression, operatorPrecedences, DEFAULT_PRECEDENCE, 0).first
        }

        /*
         * This is a pure functional Pratt parser with optimizations based on the fact that all operators
         * are infix and binary.
         */
        private fun <T : Any> parseExpression(
                expression: List<T>,
                operatorPrecedences: Map<out T, OperatorPrecedence>,
                precedence: Int,
                idx: Int
        ): Pair<BinaryExprTree<T>, Int> {
            var left: BinaryExprTree<T> = Operand(expression[idx])

            if (idx >= expression.lastIndex) {
                return left to idx + 1
            }

            var i = idx + 1
            fun nextPrecedence(): Int = when {
                i >= expression.lastIndex -> DEFAULT_PRECEDENCE
                else -> operatorPrecedences[expression[i]]!!.precedence
            }
            while (precedence < nextPrecedence()) {
                val operator = expression[i]
                val funcPrecedence = operatorPrecedences[operator]!!
                val rightPrecedence = when (funcPrecedence.associativity) {
                    LEFT, NON -> funcPrecedence.precedence
                    RIGHT -> funcPrecedence.precedence - 1
                }
                val result = parseExpression(expression, operatorPrecedences, rightPrecedence, i + 1)
                left = Binary(left, operator, result.first)
                i = result.second
            }
            return left to i
        }
    }
}

