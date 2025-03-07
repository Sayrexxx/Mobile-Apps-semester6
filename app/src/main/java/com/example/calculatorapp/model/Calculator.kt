package com.example.calculatorapp.model

import java.lang.Math.pow
import kotlin.math.pow

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
    fun plus_minus(a: Double): Double = -a
}