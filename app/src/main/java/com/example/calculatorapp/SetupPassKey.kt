package com.example.calculatorapp

package com.example.calculator111

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import android.widget.Button
import android.widget.EditText
import androidx.credentials.CreatePasswordRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.exceptions.CreateCredentialException
import com.google.android.gms.common.GoogleApiAvailability
import java.security.KeyStore
import javax.crypto.KeyGenerator

class SetupPassKeyActivity : AppCompatActivity() {

    private lateinit var etPassKey: EditText
    private lateinit var etConfirmPassKey: EditText
    private lateinit var btnSetPassKey: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup_pass_key)

        etPassKey = findViewById(R.id.etPassKey)
        etConfirmPassKey = findViewById(R.id.etConfirmPassKey)
        btnSetPassKey = findViewById(R.id.btnSetPassKey)


        btnSetPassKey.setOnClickListener {
            val passKey = etPassKey.text.toString()
            val confirmPassKey = etConfirmPassKey.text.toString()

            if (passKey == confirmPassKey) {
                savePassKey(this, passKey)

            } else {
                showError("Pass Key не совпадает")
            }
        }
    }

    private fun deletePassKey(context: Context) {
        val credentialManager = CredentialManager.create(context)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
                Log.d("PassKey", "Старый Passkey удален")
            } catch (e: Exception) {
                Log.e("PassKey", "Ошибка при удалении Passkey: ${e.message}")
            }
        }
    }

    fun generatePassKey() {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore"
        )

        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            "pass_key_alias",
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setUserAuthenticationRequired(true)
            .setUserAuthenticationValidityDurationSeconds(30)
            .build()

        keyGenerator.init(keyGenParameterSpec)
        keyGenerator.generateKey()
    }

    private fun savePassKey(context: Context, key: String) {
        deletePassKey(context)
        val credentialManager = CredentialManager.create(context)
        val request = CreatePasswordRequest(
            id = "calculator",
            password = key
        )
        if (credentialManager == null) {
            Log.e("PassKey", "Credential Manager НЕ поддерживается на этом устройстве!")
        } else {
            Log.d("PassKey", "Credential Manager доступен")
        }
        val status = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context)
        if (status != com.google.android.gms.common.ConnectionResult.SUCCESS) {
            Log.e("PassKey", "Google Play Services недоступен: код ошибки $status")
        } else {
            Log.d("PassKey", "Google Play Services работает")
        }
        CoroutineScope(Dispatchers.IO).launch {
            Log.d("PassKey", "Перед вызовом credentialManager.createCredential()")
            try {
                credentialManager.createCredential(context, request)
                Log.d("PassKey", "Passkey успешно сохранен в Google Password Manager")
                CoroutineScope(Dispatchers.Main).launch {
                    generatePassKey()
                    showMessage("Pass Key успешно установлен")
                    startActivity(Intent(context, LoginActivity::class.java))
                    finish()
                }
            } catch (e: CreateCredentialException) {
                Log.e("PassKey", "Ошибка Credential Manager: ${e.message}")
            } catch (e: SecurityException) {
                Log.e("PassKey", "Ошибка безопасности (включен ли Google Play Services?): ${e.message}")
            } catch (e: IllegalArgumentException) {
                Log.e("PassKey", "Ошибка параметров запроса (проверь id/password): ${e.message}")
            } catch (e: Exception) {
                Log.e("PassKey", "Другая ошибка: ${e.message}")
            }
        }

        val sharedPreferences = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("pass_key", key)
        editor.apply()
        Log.d("PassKey", "✅ Passkey сохранен в SharedPreferences")
    }

    private fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}