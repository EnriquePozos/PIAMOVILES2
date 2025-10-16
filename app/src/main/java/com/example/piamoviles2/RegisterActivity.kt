package com.example.piamoviles2

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.piamoviles2.databinding.ActivityRegisterBinding

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Encontrar el header incluido y configurarlo
        val headerView = findViewById<View>(R.id.headerApp)
        HeaderUtils.setupHeaderWithBackButton(this, headerView)
        HeaderUtils.setupBasicHeader(headerView)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        // El botón back ya está manejado por HeaderUtils

        // Selector de imagen de perfil
        binding.ivProfileImage.setOnClickListener {
            // TODO: Implementar selector de imagen
            Toast.makeText(this, "Selector de imagen próximamente", Toast.LENGTH_SHORT).show()
        }

        // Botón registrarse
        binding.btnRegister.setOnClickListener {
            validateAndRegister()
        }
    }

    private fun validateAndRegister() {
        val nombre = binding.etName.text.toString().trim()
        val apellidoPaterno = binding.etLastNamePaternal.text.toString().trim()
        val apellidoMaterno = binding.etLastNameMaternal.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val telefono = binding.etPhone.text.toString().trim()
        val direccion = binding.etAddress.text.toString().trim()
        val alias = binding.etAlias.text.toString().trim()

        // Validaciones básicas
        if (nombre.isEmpty()) {
            binding.etName.error = "El nombre es requerido"
            binding.etName.requestFocus()
            return
        }

        if (apellidoPaterno.isEmpty()) {
            binding.etLastNamePaternal.error = "El apellido paterno es requerido"
            binding.etLastNamePaternal.requestFocus()
            return
        }

        if (apellidoMaterno.isEmpty()) {
            binding.etLastNameMaternal.error = "El apellido materno es requerido"
            binding.etLastNameMaternal.requestFocus()
            return
        }

        if (email.isEmpty()) {
            binding.etEmail.error = "El correo electrónico es requerido"
            binding.etEmail.requestFocus()
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.error = "Por favor ingrese un correo electrónico válido"
            binding.etEmail.requestFocus()
            return
        }

        if (password.isEmpty()) {
            binding.etPassword.error = "La contraseña es requerida"
            binding.etPassword.requestFocus()
            return
        }

        // Validación de contraseña según especificaciones del proyecto:
        // Mínimo 10 caracteres, una mayúscula, una minúscula y un número
        if (!isValidPassword(password)) {
            binding.etPassword.error = "La contraseña debe tener mínimo 10 caracteres, una mayúscula, una minúscula y un número"
            binding.etPassword.requestFocus()
            return
        }

        if (alias.isEmpty()) {
            binding.etAlias.error = "El alias es requerido"
            binding.etAlias.requestFocus()
            return
        }

        // Si todas las validaciones pasan
        performRegistration()
    }

    private fun isValidPassword(password: String): Boolean {
        if (password.length < 10) return false

        var hasUpper = false
        var hasLower = false
        var hasDigit = false

        for (char in password) {
            when {
                char.isUpperCase() -> hasUpper = true
                char.isLowerCase() -> hasLower = true
                char.isDigit() -> hasDigit = true
            }
        }

        return hasUpper && hasLower && hasDigit
    }

    private fun performRegistration() {
        // Mostrar loading
        binding.btnRegister.isEnabled = false
        binding.btnRegister.text = "Registrando..."

        // Simular registro exitoso
        Toast.makeText(this, "¡Registro exitoso! Bienvenido a El sazón de Toto", Toast.LENGTH_LONG).show()

        // TODO: Aquí conectarías con tu API para registrar al usuario

        // Navegar de regreso al login
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
        finish()
    }
}