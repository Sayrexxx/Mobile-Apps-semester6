@file:Suppress("DEPRECATION")

package com.example.calculatorapp

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.calculatorapp.databinding.ActivityMainBinding
import com.example.calculatorapp.viewmodel.CalculatorViewModel
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.core.view.GestureDetectorCompat

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var gestureDetector: GestureDetectorCompat
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
            binding.btnSqrt, binding.btnPower, binding.btnPercent, binding.btnPi,
            binding.btnSin, binding.btnCos, binding.btnTg, binding.btnCtg, binding.btnPlusMinus
        )

        buttons.forEach { button ->
            button.setOnClickListener { onButtonClick(button.text.toString()) }
        }
        gestureDetector = GestureDetectorCompat(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                // Detect swipe down
                if (e1 != null && e2.y - e1.y > 50 && kotlin.math.abs(velocityY) > 50) {
                    binding.display.setText(viewModel.clearDisplay())  // Clear the display
                    return true
                }
                return false
            }
        })
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event)
    }

    private fun onButtonClick(value: String) {
        when (value) {
            "C" -> binding.display.setText(viewModel.clearDisplay())
            "=" -> binding.display.setText(viewModel.calculateResult())
            "+", "-", "*", "/", "√", "^", "%", "sin", "cos", "tg", "ctg" -> {
                binding.display.setText(viewModel.setOperator(value))
            }
            "⌫" -> binding.display.setText(viewModel.backspace())
            "π" -> {
                viewModel.clearDisplay();
                binding.display.setText(viewModel.appendToDisplay("3.141592"))
            }
            "+-" -> binding.display.setText(viewModel.changeSignForNumeric())
            else -> binding.display.setText(viewModel.appendToDisplay(value))
        }
    }
}