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
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GestureDetectorCompat
import com.google.mediapipe.formats.proto.LandmarkProto
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import com.google.mediapipe.solutions.hands.Hands
import com.google.mediapipe.solutions.hands.HandsOptions
import com.google.mediapipe.solutions.hands.HandsResult
import com.google.mlkit.vision.common.InputImage
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var gestureDetector: GestureDetectorCompat
    private val viewModel: CalculatorViewModel by viewModels()
    private var camera: Camera? = null
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var hands: Hands
    private var lastGestureTime: Long = 0

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

        val handsOptions = HandsOptions.builder()
            .setStaticImageMode(false) // Режим реального времени
            .setMaxNumHands(2) // Максимум 2 руки
            .setModelComplexity(1) // Сложность модели (1 — базовая, 2 — улучшенная)
            .setMinDetectionConfidence(0.5f) // Минимальная уверенность для обнаружения
            .setMinTrackingConfidence(0.5f) // Минимальная уверенность для отслеживания
            .build()

        hands = Hands(this, handsOptions)
        hands.setResultListener { result ->
            processHandGestures(result)
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

    private fun processHandGestures(result: HandsResult) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastGestureTime < 2000) { // 2000 мс = 2 секунды
            return // Пропустить обработку, если прошло меньше 2 секунд
        }
        lastGestureTime = currentTime

        val landmarks = result.multiHandLandmarks()
        if (landmarks != null) {
            // Собираем landmarks для обеих рук
            val hand1Landmarks = landmarks.getOrNull(0)
            val hand2Landmarks = landmarks.getOrNull(1)

            if (hand1Landmarks != null) {
                val gesture = recognizeGesture(hand1Landmarks, hand2Landmarks)
                runOnUiThread {
                    when (gesture) {
                        "0" -> onButtonClick("0")
                        "1" -> onButtonClick("1")
                        "2" -> onButtonClick("2")
                        "3" -> onButtonClick("3")
                        "4" -> onButtonClick("4")
                        "5" -> onButtonClick("5")
                        "6" -> onButtonClick("6")
                        "7" -> onButtonClick("7")
                        "8" -> onButtonClick("8")
                        "9" -> onButtonClick("9")
                        "10" -> onButtonClick("10") // Если нужно поддерживать числа больше 9
                        else -> {} // Неизвестный жест
                    }
                }

                // Распознавание математических операций
                if (hand2Landmarks != null) {
                    when {
                        isPlusGesture(hand1Landmarks, hand2Landmarks) -> onButtonClick("+")
                        isMinusGesture(hand1Landmarks, hand2Landmarks) -> onButtonClick("-")
                        isMultiplyGesture(hand1Landmarks, hand2Landmarks) -> onButtonClick("*")
                        isDivideGesture(hand1Landmarks, hand2Landmarks) -> onButtonClick("/")
                    }
                }
            }
        }
    }
    private fun recognizeGesture(
        hand1Landmarks: LandmarkProto.NormalizedLandmarkList,
        hand2Landmarks: LandmarkProto.NormalizedLandmarkList?
    ): String {
        // Считаем поднятые пальцы на обеих руках
        val hand1FingersUp = countFingersUp(hand1Landmarks.landmarkList)
        val hand2FingersUp = hand2Landmarks?.let { countFingersUp(it.landmarkList) } ?: 0

        // Сумма поднятых пальцев на обеих руках
        val totalFingersUp = hand1FingersUp + hand2FingersUp

        // Возвращаем жест в зависимости от суммы поднятых пальцев
        return when (totalFingersUp) {
            0 -> "0"
            1 -> "1"
            2 -> "2"
            3 -> "3"
            4 -> "4"
            5 -> "5"
            6 -> "6"
            7 -> "7"
            8 -> "8"
            9 -> "9"
            10 -> "10" // Если нужно поддерживать числа больше 9
            else -> "" // Неизвестный жест
        }
    }

    // Вспомогательная функция для подсчета поднятых пальцев на одной руке
    private fun countFingersUp(landmarks: List<LandmarkProto.NormalizedLandmark>): Int {
        var count = 0

        // Проверяем каждый палец
        if (isFingerUp(landmarks[8], landmarks[6])) count++ // Указательный палец
        if (isFingerUp(landmarks[12], landmarks[10])) count++ // Средний палец
        if (isFingerUp(landmarks[16], landmarks[14])) count++ // Безымянный палец
        if (isFingerUp(landmarks[20], landmarks[18])) count++ // Мизинец
        if (isThumbUp(landmarks[4], landmarks[3])) count++ // Большой палец

        return count
    }

    // Проверка, поднят ли палец
    private fun isFingerUp(tip: LandmarkProto.NormalizedLandmark, base: LandmarkProto.NormalizedLandmark): Boolean {
        return tip.y < base.y // Кончик пальца выше основания
    }

    // Проверка, поднят ли большой палец
    private fun isThumbUp(tip: LandmarkProto.NormalizedLandmark, base: LandmarkProto.NormalizedLandmark): Boolean {
        return tip.x < base.x || tip.x > base.x // Большой палец отведен в сторону
    }


    // Вспомогательная функция для получения поднятого пальца
    private fun getRaisedFinger(landmarks: List<LandmarkProto.NormalizedLandmark>): LandmarkProto.NormalizedLandmark? {
        return when {
            isFingerUp(landmarks[8], landmarks[6]) -> landmarks[8] // Указательный палец
            isFingerUp(landmarks[12], landmarks[10]) -> landmarks[12] // Средний палец
            isFingerUp(landmarks[16], landmarks[14]) -> landmarks[16] // Безымянный палец
            isFingerUp(landmarks[20], landmarks[18]) -> landmarks[20] // Мизинец
            isThumbUp(landmarks[4], landmarks[3]) -> landmarks[4] // Большой палец
            else -> null // Если ни один палец не поднят
        }
    }

    private fun isPlusGesture(
        hand1Landmarks: LandmarkProto.NormalizedLandmarkList,
        hand2Landmarks: LandmarkProto.NormalizedLandmarkList?
    ): Boolean {
        // Проверяем, что на каждой руке поднят ровно один палец
        val hand1FingersUp = countFingersUp(hand1Landmarks.landmarkList)
        val hand2FingersUp = hand2Landmarks?.let { countFingersUp(it.landmarkList) } ?: 0

        if (hand1FingersUp == 1 && hand2FingersUp == 1) {
            // Получаем поднятые пальцы на каждой руке
            val hand1Finger = getRaisedFinger(hand1Landmarks.landmarkList)
            val hand2Finger = hand2Landmarks?.let { getRaisedFinger(it.landmarkList) }

            // Проверяем, что пальцы скрещены (например, указательный и средний)
            return if (hand1Finger != null && hand2Finger != null) {
                areFingersCrossed(hand1Finger, hand2Finger)
            } else {
                false
            }
        }
        return false
    }

    private fun isMinusGesture(
        hand1Landmarks: LandmarkProto.NormalizedLandmarkList,
        hand2Landmarks: LandmarkProto.NormalizedLandmarkList?
    ): Boolean {
        // Проверяем, что на каждой руке поднят ровно один палец
        val hand1FingersUp = countFingersUp(hand1Landmarks.landmarkList)
        val hand2FingersUp = hand2Landmarks?.let { countFingersUp(it.landmarkList) } ?: 0

        if (hand1FingersUp == 1 && hand2FingersUp == 1) {
            // Получаем поднятые пальцы на каждой руке
            val hand1Finger = getRaisedFinger(hand1Landmarks.landmarkList)
            val hand2Finger = hand2Landmarks?.let { getRaisedFinger(it.landmarkList) }

            // Проверяем, что пальцы параллельны (например, указательный и средний)
            return if (hand1Finger != null && hand2Finger != null) {
                areFingersParallel(hand1Finger, hand2Finger)
            } else {
                false
            }
        }
        return false
    }

    private fun isMultiplyGesture(
        hand1Landmarks: LandmarkProto.NormalizedLandmarkList,
        hand2Landmarks: LandmarkProto.NormalizedLandmarkList?
    ): Boolean {
        // Проверяем, что на каждой руке поднят ровно один палец
        val hand1FingersUp = countFingersUp(hand1Landmarks.landmarkList)
        val hand2FingersUp = hand2Landmarks?.let { countFingersUp(it.landmarkList) } ?: 0

        if (hand1FingersUp == 1 && hand2FingersUp == 1) {
            // Получаем поднятые пальцы на каждой руке
            val hand1Finger = getRaisedFinger(hand1Landmarks.landmarkList)
            val hand2Finger = hand2Landmarks?.let { getRaisedFinger(it.landmarkList) }

            // Проверяем, что пальцы разведены в стороны
            return if (hand1Finger != null && hand2Finger != null) {
                areFingersSpread(hand1Finger, hand2Finger)
            } else {
                false
            }
        }
        return false
    }

    private fun isDivideGesture(
        hand1Landmarks: LandmarkProto.NormalizedLandmarkList,
        hand2Landmarks: LandmarkProto.NormalizedLandmarkList?
    ): Boolean {
        // Проверяем, что на каждой руке поднят ровно один палец
        val hand1FingersUp = countFingersUp(hand1Landmarks.landmarkList)
        val hand2FingersUp = hand2Landmarks?.let { countFingersUp(it.landmarkList) } ?: 0

        if (hand1FingersUp == 1 && hand2FingersUp == 1) {
            // Получаем поднятые пальцы на каждой руке
            val hand1Finger = getRaisedFinger(hand1Landmarks.landmarkList)
            val hand2Finger = hand2Landmarks?.let { getRaisedFinger(it.landmarkList) }

            // Проверяем, что пальцы наклонены в разные стороны
            return if (hand1Finger != null && hand2Finger != null) {
                areFingersTilted(hand1Finger, hand2Finger)
            } else {
                false
            }
        }
        return false
    }

    // Вспомогательные функции для проверки положения пальцев
    private fun areFingersCrossed(finger1: LandmarkProto.NormalizedLandmark, finger2: LandmarkProto.NormalizedLandmark): Boolean {
        return abs(finger1.x - finger2.x) < 0.1f && abs(finger1.y - finger2.y) < 0.1f
    }

    private fun areFingersParallel(finger1: LandmarkProto.NormalizedLandmark, finger2: LandmarkProto.NormalizedLandmark): Boolean {
        return abs(finger1.y - finger2.y) < 0.1f
    }

    private fun areFingersSpread(finger1: LandmarkProto.NormalizedLandmark, finger2: LandmarkProto.NormalizedLandmark): Boolean {
        return abs(finger1.x - finger2.x) > 0.2f
    }

    private fun areFingersTilted(finger1: LandmarkProto.NormalizedLandmark, finger2: LandmarkProto.NormalizedLandmark): Boolean {
        return (finger1.x < finger2.x && finger1.y > finger2.y) || (finger1.x > finger2.x && finger1.y < finger2.y)
    }

    @OptIn(ExperimentalGetImage::class)
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView!!.surfaceProvider)
            }

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        val mediaImage = imageProxy.image
                        if (mediaImage != null) {
                            // Преобразуем ImageProxy в Bitmap
                            val bitmap = imageProxy.toBitmap() // Используем расширение для преобразования
                            // Получаем временную метку из imageProxy
                            val timestamp = imageProxy.imageInfo.timestamp
                            // Отправляем Bitmap и временную метку в MediaPipe Hands
                            hands.send(bitmap, timestamp)
                        }
                        imageProxy.close()
                    }
                }

            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalysis
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