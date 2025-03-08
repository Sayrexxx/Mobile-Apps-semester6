package com.example.calculatorapp.model

import java.lang.Math.pow
import kotlin.math.pow
import kotlin.math.tan

class Calculator {

    fun add(a: Double, b: Double): Double = a + b
    fun subtract(a: Double, b: Double): Double = a - b
    fun multiply(a: Double, b: Double): Double = a * b
    fun divide(a: Double, b: Double): String {
        if (b == 0.0) return ("Division by zero")
        return (a / b).toString()
    }

    fun squareRoot(a: Double): Double {
        if (a < 0) throw ArithmeticException("Square root of negative number")
        return kotlin.math.sqrt(a)
    }

    fun power(a: Double, b: Double): Double = a.pow(b)
    fun percent(a: Double, b: Double): Double = (a * b) / 100

    fun sin(a: Double): Double = kotlin.math.sin(a)
    fun cos(a: Double): Double = kotlin.math.cos(a)
    fun tg (a: Double): String {
        return if (a % (kotlin.math.PI / 2) == 0.0) {
            "error"
        } else {
            tan(a).toString()
        }
    }
    fun ctg(a: Double): String {
        return if (a % kotlin.math.PI == 0.0) {
            "error"
        } else {
            (1 / tan(a)).toString()
        }
    }
}