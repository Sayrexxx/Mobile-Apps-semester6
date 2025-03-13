@file:Suppress("DEPRECATION")

package com.example.calculatorapp

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.calculatorapp.databinding.ActivityMainBinding
import com.example.calculatorapp.viewmodel.CalculatorViewModel
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GestureDetectorCompat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var gestureDetector: GestureDetectorCompat
    private val viewModel: CalculatorViewModel by viewModels()
    private var camera: Camera? = null
    private lateinit var cameraExecutor: ExecutorService

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
            binding.btnSin, binding.btnCos, binding.btnTg, binding.btnCtg, binding.btnPlusMinus,
            binding.btnCamera
        )

        buttons.forEach { button ->
            button?.setOnClickListener { onButtonClick(button.text.toString()) }
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

        cameraExecutor = Executors.newSingleThreadExecutor()

        binding.btnCamera?.setOnClickListener {
            if (binding.previewView!!.visibility == View.GONE) {
                if (allPermissionsGranted()) {
                    binding.previewView!!.visibility = View.VISIBLE
                    startCamera()
                } else {
                    ActivityCompat.requestPermissions(
                        this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
                    )
                }
            } else {
                binding.previewView!!.visibility = View.GONE
                stopCamera()
            }
        }
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

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView!!.surfaceProvider)
            }

            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview
                )
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun stopCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            cameraProvider.unbindAll()
        }, ContextCompat.getMainExecutor(this))
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all { permission ->
        ContextCompat.checkSelfPermission(baseContext, permission) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "CameraXApp"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(android.Manifest.permission.CAMERA)
    }

}