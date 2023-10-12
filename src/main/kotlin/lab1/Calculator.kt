package lab1

import kotlin.math.log
import kotlin.math.pow

fun calculate(expression: String): Double {
    var index = 0
    val skipWhile =
        { cond: (Char) -> Boolean -> // передаем в аргументы лямбду условие, по которому будет осуществляться фильтрация
            while (index < expression.length && cond(expression[index])) {
                index++
            }
        }
    val tryRead = { c: Char ->
        (index < expression.length && expression[index] == c).also {
            if (it) {
                index++
            }
        }
    }
    val skipWhitespaces = { skipWhile { it.isWhitespace() } }
    val tryReadOperation =
        { c: Char ->
            skipWhitespaces().run { tryRead(c) }.also {
                if (it) {
                    skipWhitespaces()
                }
            }
        }
    var rootOperation: () -> Double = { 0.0 }

    val num = {
        if (tryReadOperation('(')) {
            rootOperation().also {
                tryReadOperation(')').also {
                    if (!it) throw IllegalExpressionException(
                        "Missing ) on $index position of expression"
                    )
                }
            }
        } else {
            val start = index
            skipWhile { it.isDigit() || it == '.' }
            try {
                val num = expression.substring(start, index).toDouble()
                if (num == Double.POSITIVE_INFINITY || num == Double.NEGATIVE_INFINITY) {
                    throw IllegalExpressionException("Too big num")
                }
                num
            } catch (e: NumberFormatException) {
                throw IllegalExpressionException("Invalid number on $start position of expression", cause = e)
            }
        }
    }

    fun binary(left: () -> Double, op: Char): List<Double> = mutableListOf(left()).apply {
        while (tryReadOperation(op)) {
            addAll(binary(left, op))
        }
    }

    val log = { binary(num, 'l').reduce { a, b -> log(base = a, x = b) } }
    val deg = { binary(log, '^').reduce { a, b -> a.pow(b) } }
    val div = {
        binary(deg, '/').reduce { a, b ->
            if (b != 0.0) {
                a / b
            } else {
                throw IllegalExpressionException("divide by zero")
            }
        }
    }
    val mul = { binary(div, '*').reduce { a, b -> a * b } }
    val sub = { binary(mul, '-').reduce { a, b -> a - b } }
    val add = { binary(sub, '+').reduce { a, b -> a + b } }


    rootOperation = add
    return rootOperation().also {
        if (index < expression.length) {
            throw IllegalExpressionException("Invalid expression")
        }
        if (it == Double.POSITIVE_INFINITY || it == Double.NEGATIVE_INFINITY || it.isNaN()) {
            throw IllegalExpressionException("Answer is too big")
        }
    }
}