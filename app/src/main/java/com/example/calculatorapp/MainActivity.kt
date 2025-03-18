@file:Suppress("DEPRECATION")

package com.example.calculatorapp

import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
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
import kotlin.math.abs
import android.os.Vibrator
import android.os.VibrationEffect
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.calculatorapp.model.Operation
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var gestureDetector: GestureDetectorCompat
    private val viewModel: CalculatorViewModel by viewModels()
    private var camera: Camera? = null
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var hands: Hands
    private var lastGestureTime: Long = 0
    private lateinit var vibrator: Vibrator
    private lateinit var db: FirebaseFirestore
    private var isDarkTheme = false

    private val sharedPreferences by lazy {
        getSharedPreferences("app_theme", Context.MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        db = FirebaseFirestore.getInstance()
        isDarkTheme = sharedPreferences.getBoolean("is_dark_theme", false)
        applyTheme()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnThemeSwitch.setOnClickListener {
            isDarkTheme = !isDarkTheme
            sharedPreferences.edit().putBoolean("is_dark_theme", isDarkTheme).apply() // Сохраняем состояние темы
            applyTheme()
            recreate()
        }

        val notificationMessage = intent.getStringExtra("notification_message")
        if (notificationMessage != null) {
            showNotificationDialog(notificationMessage)
        }

        binding.btnFetchOperations.setOnClickListener {
            fetchOperationsAndSendNotification()
        }

        binding.display.setText(viewModel.clearDisplay())

        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

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

    private fun applyTheme() {
        if (isDarkTheme) {
            setTheme(R.style.AppTheme)
        } else {
            setTheme(R.style.Theme_CalculatorApp_Dark)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Обновляем Intent активности
        setIntent(intent)
        // Проверяем, есть ли данные из уведомления
        val notificationMessage = intent?.getStringExtra("notification_message")
        if (notificationMessage != null) {
            showNotificationDialog(notificationMessage)
        }
    }

    private fun showNotificationDialog(message: String) {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Last 3 Operations")
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        dialog.show()
    }

    private fun isInternetAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }

    private fun fetchOperationsAndSendNotification() {
        if (!isInternetAvailable()) {
            Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("calculations")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(3)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Toast.makeText(this, "No operations found", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val operations = documents.map { doc ->
                    val expression = doc.getString("expression") ?: ""
                    val result = doc.getString("result") ?: ""
                    "$expression = $result"
                }

                sendNotification(operations.joinToString("\n"))
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Failed to fetch operations: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun sendNotification(message: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("notification_message", message) // Передаем текст уведомления
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "operations_channel",
                "Operations",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notificationBuilder = NotificationCompat.Builder(this, "operations_channel")
            .setSmallIcon(R.drawable.splash_screen)
            .setContentTitle("Last 3 Operations")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        notificationManager.notify(1, notificationBuilder.build())
    }

    private fun vibrate() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(200)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event)
    }

    private fun onButtonClick(value: String) {
        when (value) {
            "C" -> {
                binding.display.setText(viewModel.clearDisplay())
                vibrate()
            }
            "=" -> {
                val result = viewModel.calculateResult()
                val expression = viewModel.getCurrentExpression()
                saveCalculationToFirestore(expression, result)
                binding.display.setText(result)
                if (result in listOf("empty input", "Division by zero", "error")) {
                    vibrate()
                }
            }
            "+", "-", "*", "/", "^", "%" -> {
                binding.display.setText(viewModel.setOperator(value))
            }
            "√", "sin", "cos", "tg", "ctg" -> {
                val result = viewModel.setOperator(value)
                if (result !in listOf("empty input", "Division by zero", "error")) {
                    saveCalculationToFirestore(viewModel.getCurrentExpressionForUnaryOperation(), result)
                }
                binding.display.setText(result)
            }
            "⌫" -> {
                binding.display.setText(viewModel.backspace())
                vibrate()
            }
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
        // Большой палец считается поднятым, если он отведен в сторону и поднят вверх
        return (tip.x < base.x || tip.x > base.x) && tip.y < base.y
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

    private fun saveCalculationToFirestore(expression: String, result: String) {
        if (expression.isEmpty() or result.isEmpty()) return
        val calculation = Operation(expression, result)

        db.collection("calculations")
            .add(calculation)
            .addOnSuccessListener { documentReference ->
                Log.d("Firestore", "Document added with ID: ${documentReference.id}")
                Toast.makeText(this, "Operation saved!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error adding document", e)
                Toast.makeText(this, "Failed to save operation.", Toast.LENGTH_SHORT).show()
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
