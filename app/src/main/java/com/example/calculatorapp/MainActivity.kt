package com.example.calculatorapp

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.calculatorapp.databinding.ActivityMainBinding
import com.example.calculatorapp.viewmodel.CalculatorViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: CalculatorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.display.setText(viewModel.clearDisplay())

        // Set up button listeners
        val buttons = listOf(
            binding.btn0, binding.btn1, binding.btn2, binding.btn3,
            binding.btn4, binding.btn5, binding.btn6, binding.btn7,
            binding.btn8, binding.btn9, binding.btnAdd, binding.btnSubtract,
            binding.btnMultiply, binding.btnDivide, binding.btnDecimal,
            binding.btnEquals, binding.btnClear, binding.btnBackspace,
            binding.btnSqrt, binding.btnPower, binding.btnPercent, binding.btnPi
        )

        buttons.forEach { button ->
            button.setOnClickListener { onButtonClick(button.text.toString()) }
        }
    }

    private fun onButtonClick(value: String) {
        when (value) {
            "C" -> binding.display.setText(viewModel.clearDisplay())
            "=" -> binding.display.setText(viewModel.calculateResult())
            "+", "-", "*", "/", "√", "^", "%", "sin", "cos", "tg", "ctg"-> binding.display.setText(viewModel.setOperator(value))
            "⌫" -> binding.display.setText(viewModel.backspace())
            "π" -> binding.display.setText(viewModel.appendToDisplay("3.141592"))
            "+-" -> binding.display.setText(viewModel.changeSignForNumeric())
            else -> binding.display.setText(viewModel.appendToDisplay(value))
        }
    }
}