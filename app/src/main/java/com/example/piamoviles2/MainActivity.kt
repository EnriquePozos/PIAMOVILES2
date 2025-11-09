package com.example.piamoviles2

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.piamoviles2.databinding.ActivityMainBinding
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.example.piamoviles2.data.repositories.UserRepository
import com.example.piamoviles2.utils.UiState
import com.example.piamoviles2.utils.SessionManager
import com.example.piamoviles2.data.models.LoginResponse

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // Propiedades para API
    private lateinit var userRepository: UserRepository
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializar ViewBinding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializar componentes de API
        initializeApiComponents()

        // Encontrar el header incluido y configurarlo
        val headerView = findViewById<View>(R.id.headerApp)
        HeaderUtils.setupBasicHeader(headerView)

        // Verificar si ya está logueado
        checkExistingSession()

        setupClickListeners()
    }

    private fun initializeApiComponents() {
        userRepository = UserRepository()
        sessionManager = SessionManager(this)
    }

    private fun checkExistingSession() {
        if (sessionManager.isLoggedIn()) {
            // Ya está logueado, ir directo al Feed
            val intent = Intent(this, FeedActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun setupClickListeners() {
        // Manejar click del botón "Iniciar sesión"
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (validateLoginForm(email, password)) {
                performLogin(email, password)
            }
        }

        // Manejar click del link "Regístrate aquí"
        binding.tvRegisterLink.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    private fun validateLoginForm(email: String, password: String): Boolean {
        if (email.isEmpty()) {
            binding.etEmail.error = "Por favor ingrese su correo electrónico"
            binding.etEmail.requestFocus()
            return false
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.error = "Por favor ingrese un correo electrónico válido"
            binding.etEmail.requestFocus()
            return false
        }

        if (password.isEmpty()) {
            binding.etPassword.error = "Por favor ingrese su contraseña"
            binding.etPassword.requestFocus()
            return false
        }

        if (password.length < 6) {
            binding.etPassword.error = "La contraseña debe tener al menos 6 caracteres"
            binding.etPassword.requestFocus()
            return false
        }

        return true
    }

    private fun performLogin(email: String, password: String) {
        // ✅ LOGS DETALLADOS DE LOS DATOS
        android.util.Log.d("LOGIN_DEBUG", "=== INICIANDO LOGIN ===")
        android.util.Log.d("LOGIN_DEBUG", "Email RAW: '$email'")
        android.util.Log.d("LOGIN_DEBUG", "Email length: ${email.length}")
        android.util.Log.d("LOGIN_DEBUG", "Email isEmpty: ${email.isEmpty()}")
        android.util.Log.d("LOGIN_DEBUG", "Email isBlank: ${email.isBlank()}")

        android.util.Log.d("LOGIN_DEBUG", "Password RAW: '$password'")
        android.util.Log.d("LOGIN_DEBUG", "Password length: ${password.length}")
        android.util.Log.d("LOGIN_DEBUG", "Password isEmpty: ${password.isEmpty()}")
        android.util.Log.d("LOGIN_DEBUG", "Password isBlank: ${password.isBlank()}")

        // ✅ VERIFICAR SI LOS CAMPOS TIENEN DATOS
        val emailFromField = binding.etEmail.text.toString()
        val passwordFromField = binding.etPassword.text.toString()

        android.util.Log.d("LOGIN_DEBUG", "Email desde campo: '$emailFromField'")
        android.util.Log.d("LOGIN_DEBUG", "Password desde campo: '$passwordFromField'")
        android.util.Log.d("LOGIN_DEBUG", "¿Son iguales email? ${email == emailFromField}")
        android.util.Log.d("LOGIN_DEBUG", "¿Son iguales password? ${password == passwordFromField}")

        android.util.Log.d("LOGIN_DEBUG", "BASE_URL debería ser: https://uneroded-forest-untasked.ngrok-free.dev/")

        lifecycleScope.launch {
            try {
                // Estado Loading
                android.util.Log.d("LOGIN_DEBUG", "Cambiando a estado Loading...")
                updateLoginUI(UiState.Loading)

                android.util.Log.d("LOGIN_DEBUG", "Haciendo llamada a userRepository.loginUsuario...")

                // Llamada real a la API
                val result = userRepository.loginUsuario(email, password)

                android.util.Log.d("LOGIN_DEBUG", "Respuesta recibida. Success: ${result.isSuccess}")

                // Manejar resultado
                if (result.isSuccess) {
                    android.util.Log.d("LOGIN_DEBUG", "Login exitoso!")
                    updateLoginUI(UiState.Success(result.getOrNull()!!))
                } else {
                    val errorMessage = result.exceptionOrNull()?.message ?: "Error de conexión"
                    android.util.Log.e("LOGIN_DEBUG", "Error en login: $errorMessage")
                    android.util.Log.e("LOGIN_DEBUG", "Exception completa: ${result.exceptionOrNull()}")
                    updateLoginUI(UiState.Error(errorMessage))
                }

            } catch (e: Exception) {
                android.util.Log.e("LOGIN_DEBUG", "Exception capturada: ${e.message}")
                android.util.Log.e("LOGIN_DEBUG", "Stack trace: ", e)
                updateLoginUI(UiState.Error("Error inesperado: ${e.message}"))
            }
        }
    }

    private fun updateLoginUI(state: UiState<LoginResponse>) {
        when (state) {
            is UiState.Loading -> {
                // Mostrar estado de carga
                binding.btnLogin.isEnabled = false
                binding.btnLogin.text = "Iniciando sesión..."
            }

            is UiState.Success -> {
                // Login exitoso
                binding.btnLogin.isEnabled = true
                binding.btnLogin.text = "Iniciar sesión"

                val loginResponse = state.data

                // Guardar sesión
                sessionManager.saveLoginData(loginResponse.accessToken, loginResponse.usuario)

                // Mostrar mensaje de bienvenida
                Toast.makeText(this, "¡Bienvenido ${loginResponse.usuario.alias}!", Toast.LENGTH_SHORT).show()

                // Navegar al Feed
                val intent = Intent(this, FeedActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }

            is UiState.Error -> {
                // Error en login
                binding.btnLogin.isEnabled = true
                binding.btnLogin.text = "Iniciar sesión"

                // Mostrar mensaje de error
                Toast.makeText(this, "Error: ${state.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}