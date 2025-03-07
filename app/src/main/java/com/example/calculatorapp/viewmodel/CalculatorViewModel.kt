package com.example.calculatorapp.viewmodel

import androidx.lifecycle.ViewModel
import com.example.calculatorapp.model.Calculator

class CalculatorViewModel : ViewModel() {

    private val calculator = Calculator()
    private var currentInput = StringBuilder()
    private var currentOperator: String? = null
    private var firstOperand: Double? = null

    fun appendToDisplay(value: String): String {
        if (currentInput.toString().length > 20) {
            currentInput.clear()
        }
        if (currentInput.toString() == "empty input" || currentInput.toString() == "Division by zero" || currentInput.toString() == "error") {
            clearDisplay()
        }
        if ((value == "." && currentInput.contains('.'))) {
            currentInput = StringBuilder("error")
            return currentInput.toString()
        }
        currentInput.append(value)
        return currentInput.toString()
    }

    fun clearDisplay(): String {
        currentInput.clear()
        firstOperand = null
        currentOperator = null
        return currentInput.toString()
    }

    fun backspace(): String {
        if (currentInput.toString() == "empty input" || currentInput.toString() == "Division by zero" || currentInput.toString() == "error") {
            clearDisplay()
        }
        if (currentInput.isNotEmpty()) {
            currentInput.deleteCharAt(currentInput.length-1)
            return currentInput.toString()
        }
        else {
            return "empty input"
        }
    }

    fun setOperator(operator: String): String {
        if (currentInput.toString() == "empty input" || currentInput.toString() == "Division by zero" || currentInput.toString() == "error") {
            clearDisplay()
        }
        if (currentInput.isNotEmpty()) {
            firstOperand = currentInput.toString().toDouble()
            currentOperator = operator
            currentInput.clear()
        }
        return currentInput.toString()
    }

    fun calculateResult(): String {
        if (currentInput.toString() == "empty input" || currentInput.toString() == "Division by zero" || currentInput.toString() == "error") {
            clearDisplay()
        }
        if (currentInput.isNotEmpty() && firstOperand != null && currentOperator != null) {
            val secondOperand = currentInput.toString().toDouble()
            val result = when (currentOperator) {
                "+" -> calculator.add(firstOperand!!, secondOperand)
                "-" -> calculator.subtract(firstOperand!!, secondOperand)
                "*" -> calculator.multiply(firstOperand!!, secondOperand)
                "/" -> calculator.divide(firstOperand!!, secondOperand)
                "√" -> calculator.squareRoot(secondOperand)
                "sin" -> calculator.sin(secondOperand)
                "cos" -> calculator.cos(secondOperand)
                "tg" -> calculator.tg(secondOperand)
                "ctg" -> calculator.ctg(secondOperand)
                "^" -> calculator.power(firstOperand!!, secondOperand)
                "%" -> calculator.percent(firstOperand!!, secondOperand)
                else -> currentInput.clear();
            }
            currentInput.clear()
            var res = result.toString()
            if (res[res.length - 1] == '0' && res[res.length - 2] == '.') {
                res = res.dropLast(2)

            }
            currentInput.append(res)
            firstOperand = null
            currentOperator = null
        }
        return currentInput.toString()
    }

    fun changeSignForNumeric(): String {
        var operand = currentInput.toString()
        if (operand[0] == '-') {
            operand.drop(1)
        } else if (operand[0] == '0'){
            return operand
        } else {
            operand = "-$operand"
        }
        return operand
    }
}