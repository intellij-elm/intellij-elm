package org.elm.lang.core.types

import org.elm.lang.core.psi.OperatorAssociativity
import org.elm.lang.core.psi.OperatorAssociativity.*
import org.elm.lang.core.types.BinaryExprTree.Binary
import org.elm.lang.core.types.BinaryExprTree.Operand
import org.junit.Assert.assertEquals
import org.junit.Test

class BinaryExprTreeTest {
    private fun ops(vararg p: Pair<Int, OperatorAssociativity>): Map<String, OperatorPrecedence> {
        return p.withIndex().associate { (i, it) ->
            "${it.second}-$i" to OperatorPrecedence(it.first, it.second)
        }
    }

    @Test
    fun `no operators`() {
        val expression = listOf(1)
        val actual = BinaryExprTree.parse(expression, emptyMap())
        assertEquals(Operand(1), actual)
    }

    @Test
    fun `single operator`() {
        val ops = ops(1 to NON)
        val op = ops.entries.single().key
        val expression = listOf("1", op, "2")
        val actual = BinaryExprTree.parse(expression, ops)
        assertEquals(Binary(Operand("1"), op, Operand("2")), actual)
    }

    @Test
    fun `left assoc`() {
        val ops = ops(1 to LEFT, 1 to LEFT)
        val (op1, op2) = ops.keys.toList()
        val expression = listOf("1", op1, "2", op2, "3")
        val actual = BinaryExprTree.parse(expression, ops)
        val expected = Binary(
                Binary(Operand("1"), op1, Operand("2")),
                op2,
                Operand("3"))
        assertEquals(expected, actual)
    }

    @Test
    fun `right assoc`() {
        val ops = ops(1 to RIGHT, 1 to RIGHT)
        val (op1, op2) = ops.keys.toList()
        val expression = listOf("1", op1, "2", op2, "3")
        val actual = BinaryExprTree.parse(expression, ops)
        val expected = Binary(
                Operand("1"), op1, Binary(Operand("2"),
                op2,
                Operand(("3"))))
        assertEquals(expected, actual)
    }

    @Test
    fun `high precedence first`() {
        val ops = ops(2 to LEFT, 1 to LEFT)
        val (op1, op2) = ops.keys.toList()
        val expression = listOf("1", op1, "2", op2, "3")
        val actual = BinaryExprTree.parse(expression, ops)
        val expected = Binary(
                Binary(Operand("1"), op1, Operand("2")),
                op2,
                Operand("3"))
        assertEquals(expected, actual)
    }

    @Test
    fun `high precedence last`() {
        val ops = ops(1 to LEFT, 2 to LEFT)
        val (op1, op2) = ops.keys.toList()
        val expression = listOf("1", op1, "2", op2, "3")
        val actual = BinaryExprTree.parse(expression, ops)
        val expected = Binary(
                Operand("1"),
                op1,
                Binary(Operand("2"), op2, Operand(("3"))))
        assertEquals(expected, actual)
    }

    @Test
    fun `left assoc with non assoc`() {
        val ops = ops(2 to NON, 1 to LEFT, 2 to NON)
        val (op1, op2, op3) = ops.keys.toList()
        val expression = listOf("1", op1, "2", op2, "3", op3, "4")
        val actual = BinaryExprTree.parse(expression, ops)
        val expected = Binary(
                Binary(Operand("1"), op1, Operand("2")),
                op2,
                Binary(Operand("3"), op3, Operand("4")))
        assertEquals(expected, actual)
    }

    @Test
    fun `right assoc with non assoc`() {
        val ops = ops(2 to NON, 1 to RIGHT, 2 to NON)
        val (op1, op2, op3) = ops.keys.toList()
        val expression = listOf("1", op1, "2", op2, "3", op3, "4")
        val actual = BinaryExprTree.parse(expression, ops)
        val expected = Binary(
                Binary(Operand("1"), op1, Operand("2")),
                op2,
                Binary(Operand("3"), op3, Operand("4")))
        assertEquals(expected, actual)
    }
}
