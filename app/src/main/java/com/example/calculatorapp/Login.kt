package com.example.calculatorapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.autofill.AutofillManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.credentials.CredentialManager
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPasswordOption
import androidx.credentials.PasswordCredential
import androidx.fragment.app.FragmentActivity
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class LoginActivity : AppCompatActivity() {

    private lateinit var etPassKey: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnResetPassKey: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        val sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val passKey = sharedPreferences.getString("pass_key", null)
        val isAuth=sharedPreferences.getString("isAuth","false")
        if (isAuth=="false") {
            if (passKey == null) {

                startActivity(Intent(this, SetupPassKeyActivity::class.java))
                finish()
            }
        }
        val db = Firebase.firestore
        val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        val themeSaving= ThemeSaving()
        var newThemeState:Boolean = false
        ThemeSaving().loadDeviceData(db, deviceId) { themeBoolean ->

            if (themeBoolean) {

                newThemeState=true
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {

                newThemeState=false
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }
        autoLogin(this)
        etPassKey = findViewById(R.id.etPassKey)
        btnLogin = findViewById(R.id.btnLogin)
        btnResetPassKey = findViewById(R.id.btnResetPassKey)
        requestAutofill()
        val credentialManager = CredentialManager.create(this)
        btnLogin.setOnClickListener {
            val enteredKey = etPassKey.text.toString()
            if (validatePassKey(enteredKey)) {
                grantAccess()
            } else {
                showError("Неверный Pass Key")
            }
        }
        Log.d("PassKey", "Поддержка Credential Manager: $credentialManager")
        btnResetPassKey.setOnClickListener {
            setupBiometricAuthentication(this,this) // Используем биометрию для сброса
        }
    }

    private fun requestAutofill() {
        val autofillManager = getSystemService(AutofillManager::class.java)
        autofillManager?.requestAutofill(etPassKey)
    }

    private fun autoLogin(context: Context) {
        val credentialManager = CredentialManager.create(context)

        val request = GetCredentialRequest(
            listOf(GetPasswordOption())
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = credentialManager.getCredential(context, request)
                val credential = result.credential as PasswordCredential
                val storedPassKey = credential.password

                // Проверяем пароль
                if (validatePassKey(storedPassKey)) {
                    grantAccess()
                } else {
                    showError("Pass Key неверный")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun validatePassKey(enteredKey: String): Boolean {
        val storedKey = getStoredPassKey()
        return enteredKey == storedKey
    }

    private fun getStoredPassKey(): String? {
        val sharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        return sharedPreferences.getString("pass_key", null)
    }

    private fun grantAccess() {
        val sharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("isAuth", "true")
        editor.apply()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    private fun setupBiometricAuthentication(activity: FragmentActivity, context:Context) {
        val executor = ContextCompat.getMainExecutor(activity)
        val biometricPrompt = BiometricPrompt(activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    showError("Ошибка аутентификации: $errString")
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    resetPassKey()
                    finish()
                    startActivity(Intent(context, SetupPassKeyActivity::class.java))
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    showError("Аутентификация не удалась")
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Биометрическая аутентификация")
            .setSubtitle("Используйте отпечаток пальца или лицо для сброса Pass Key")
            .setNegativeButtonText("Отмена")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    private fun resetPassKey() {
        val sharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.remove("pass_key")
        editor.apply()
    }
}