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
        if (currentInput.toString() in listOf("empty input", "Division by zero", "error", ".")) {
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
        if (currentInput.toString() in listOf("empty input", "Division by zero", "error", ".")) {
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

    private fun formatResult(result: String): String {
        return if (result.endsWith(".0")) {
            result.dropLast(2)
        } else result
    }

    private fun handleUnaryOperator(operator: String) {
        val operand = currentInput.toString().toDoubleOrNull() ?: return
        val result = when (operator) {
            "sin" -> calculator.sin(operand).toString()
            "cos" -> calculator.cos(operand).toString()
            "tg" -> calculator.tg(operand)
            "ctg" -> calculator.ctg(operand)
            "√" -> {
                if (operand >= 0) {
                    calculator.squareRoot(operand).toString()
                } else {
                    "error"
                }
            }
            else -> return
        }
        currentInput.clear()
        currentInput.append(result)
        if (result == "error") {
            currentOperator = null
        }
    }

    fun setOperator(operator: String): String {
        if (currentInput.toString() in listOf("empty input", "Division by zero", "error", ".")) {
            clearDisplay()
        }
        if (currentInput.isNotEmpty()) {
            when (operator) {
                "sin", "cos", "tg", "ctg", "√" -> {
                    handleUnaryOperator(operator)
                }
                else -> {
                    firstOperand = currentInput.toString().toDouble()
                    currentOperator = operator
                    currentInput.clear()
                }
            }
        }
        return currentInput.toString()
    }

    fun calculateResult(): String {
        if (currentInput.toString() in listOf("empty input", "Division by zero", "error", ".")) {
            clearDisplay()
        }
        if (currentInput.isNotEmpty()  && currentOperator != null) {
            val secondOperand = currentInput.toString().toDouble()
            val result = when (currentOperator) {
                "+" -> calculator.add(firstOperand!!, secondOperand)
                "-" -> calculator.subtract(firstOperand!!, secondOperand)
                "*" -> calculator.multiply(firstOperand!!, secondOperand)
                "/" -> calculator.divide(firstOperand!!, secondOperand)
                "√" -> calculator.squareRoot(firstOperand!!)
                "sin" -> calculator.sin(firstOperand!!)
                "cos" -> calculator.cos(firstOperand!!)
                "tg" -> calculator.tg(firstOperand!!)
                "ctg" -> calculator.ctg(firstOperand!!)
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
        if (currentInput.isEmpty()) return "0"
        val operand = currentInput.toString()
        if (operand == "0") return operand
        val newOperand = if (operand.startsWith("-")) {
            operand.substring(1)
        } else {
            "-$operand"
        }
        currentInput = StringBuilder(newOperand)
        return currentInput.toString()
    }
}