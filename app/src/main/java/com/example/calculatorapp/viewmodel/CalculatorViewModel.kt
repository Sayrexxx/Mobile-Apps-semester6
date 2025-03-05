package com.example.calculatorapp.viewmodel

import androidx.lifecycle.ViewModel
import com.example.calculatorapp.model.Calculator

class CalculatorViewModel : ViewModel() {

    private val calculator = Calculator()
    private var currentInput = StringBuilder()
    private var currentOperator: String? = null
    private var firstOperand: Double? = null

    fun appendToDisplay(value: String): String {
        currentInput.append(value)
        return currentInput.toString()
    }

    fun clearDisplay(): String {
        currentInput.clear()
        currentInput.append("0")
        firstOperand = null
        currentOperator = null
        return currentInput.toString()
    }

    fun setOperator(operator: String): String {
        if (currentInput.isNotEmpty()) {
            firstOperand = currentInput.toString().toDouble()
            currentOperator = operator
            currentInput.clear()
        }
        return currentInput.toString()
    }

    fun calculateResult(): String {
        if (currentInput.isNotEmpty() && firstOperand != null && currentOperator != null) {
            val secondOperand = currentInput.toString().toDouble()
            val result = when (currentOperator) {
                "+" -> calculator.add(firstOperand!!, secondOperand)
                "-" -> calculator.subtract(firstOperand!!, secondOperand)
                "*" -> calculator.multiply(firstOperand!!, secondOperand)
                "/" -> calculator.divide(firstOperand!!, secondOperand)
                "√" -> calculator.squareRoot(secondOperand)
                "^" -> calculator.power(firstOperand!!, secondOperand)
                "%" -> calculator.percent(firstOperand!!, secondOperand)
                "π" -> calculator.pi()
                else -> throw IllegalArgumentException("Invalid operator")
            }
            currentInput.clear()
            currentInput.append(result)
            firstOperand = null
            currentOperator = null
        }
        return currentInput.toString()
    }
}