package com.example.piamoviles2

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.piamoviles2.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializar ViewBinding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Encontrar el header incluido y configurarlo
        val headerView = findViewById<View>(R.id.headerApp)
        HeaderUtils.setupBasicHeader(headerView)

        setupClickListeners()
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
        // Mostrar loading (opcional)
        binding.btnLogin.isEnabled = false
        binding.btnLogin.text = "Iniciando sesión..."

        // Simular autenticación (aquí conectarías con tu API)
        // Por ahora, solo validamos credenciales hardcodeadas
        if (email == "test@sazondetoto.com" && password == "123456") {
            // Login exitoso
            Toast.makeText(this, "¡Bienvenido! Login exitoso", Toast.LENGTH_SHORT).show()

            // TODO: Navegar a FeedActivity (pantalla principal)
            val intent = Intent(this, FeedActivity::class.java)
            startActivity(intent)
            finish() // Cerrar login para que no pueda volver con back

            // Por ahora solo restauramos el botón
            binding.btnLogin.isEnabled = true
            binding.btnLogin.text = "Iniciar sesión"

        } else {
            // Login fallido
            Toast.makeText(this, "Credenciales incorrectas", Toast.LENGTH_SHORT).show()
            binding.btnLogin.isEnabled = true
            binding.btnLogin.text = "Iniciar sesión"
        }
    }
}