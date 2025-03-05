package com.example.calculatorapp.model

import java.lang.Math.pow
import kotlin.math.pow

class Calculator {

    fun add(a: Double, b: Double): Double = a + b
    fun subtract(a: Double, b: Double): Double = a - b
    fun multiply(a: Double, b: Double): Double = a * b
    fun divide(a: Double, b: Double): Double {
        if (b == 0.0) throw ArithmeticException("Division by zero")
        return a / b
    }

    fun squareRoot(a: Double): Double {
        if (a < 0) throw ArithmeticException("Square root of negative number")
        return kotlin.math.sqrt(a)
    }

    fun power(a: Double, b: Double): Double = a.pow(b)
    fun percent(a: Double, b: Double): Double = (a * b) / 100
    fun pi(): Double = kotlin.math.PI
}