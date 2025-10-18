package com.example.piamoviles2

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.piamoviles2.databinding.ActivityRegisterBinding

class EditProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupHeader()
        setupFieldsForEdit()
        loadCurrentUserData()
        setupClickListeners()
    }

    private fun setupHeader() {
        val headerView = findViewById<View>(R.id.headerApp)
        HeaderUtils.setupHeaderWithBackButton(this, headerView)
    }

    private fun setupFieldsForEdit() {
        // Hacer el email de solo lectura (NO se puede cambiar)
        binding.etEmail.isEnabled = false
        binding.etEmail.alpha = 0.6f

        // La contraseña SÍ se puede editar
        binding.etPassword.isEnabled = true
        binding.etPassword.hint = "Opcional"

        // Cambiar el texto del botón
        binding.btnRegister.text = "Guardar cambios"

        // Hacer campos opcionales más visibles
        binding.etPhone.hint = "Teléfono (opcional)"
        binding.etAddress.hint = "Dirección (opcional)"
    }

    private fun loadCurrentUserData() {
        // Cargar datos del usuario actual (datos de ejemplo basados en imagen 6)
        binding.etName.setText("Enrique")
        binding.etLastNamePaternal.setText("Pozos")
        binding.etLastNameMaternal.setText("González")
        binding.etEmail.setText("enriquepozos@gmail.com") // Email bloqueado
        binding.etPhone.setText("8125785504")
        binding.etAddress.setText("Villa Rica #122")
        binding.etAlias.setText("@Pozos___")
        binding.etPassword.setText("••••••••••")

        // NO cargar la contraseña actual por seguridad
        // binding.etPassword.setText("") // Vacío para que el usuario decida si cambiarla

        // Cargar imagen actual (simulada - Spider-Man de la imagen)
        binding.ivProfileImage.setImageResource(R.mipmap.ic_launcher)
    }

    private fun setupClickListeners() {
        // Selector de imagen de perfil
        binding.ivProfileImage.setOnClickListener {
            Toast.makeText(this, "Cambiar imagen (próximamente)", Toast.LENGTH_SHORT).show()
            // TODO: Implementar selector de imagen
        }

        // Botón guardar cambios
        binding.btnRegister.setOnClickListener {
            saveProfileChanges()
        }
    }

    private fun saveProfileChanges() {
        val nombre = binding.etName.text.toString().trim()
        val apellidoPaterno = binding.etLastNamePaternal.text.toString().trim()
        val apellidoMaterno = binding.etLastNameMaternal.text.toString().trim()
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

        if (alias.isEmpty()) {
            binding.etAlias.error = "El alias es requerido"
            binding.etAlias.requestFocus()
            return
        }

        // Validar formato del alias
        if (!alias.startsWith("@")) {
            binding.etAlias.error = "El alias debe comenzar con @"
            binding.etAlias.requestFocus()
            return
        }

        // Validar contraseña SOLO si se está cambiando
        if (password.isNotEmpty()) {
            if (!isValidPassword(password)) {
                binding.etPassword.error = "La contraseña debe tener mínimo 10 caracteres, una mayúscula, una minúscula y un número"
                binding.etPassword.requestFocus()
                return
            }
        }

        // Simular guardado exitoso
        performSaveChanges(password.isNotEmpty())
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

    private fun performSaveChanges(passwordChanged: Boolean) {
        // Mostrar loading
        binding.btnRegister.isEnabled = false
        binding.btnRegister.text = "Guardando..."

        // Mensaje específico según lo que se cambió
        val message = if (passwordChanged) {
            "¡Perfil y contraseña actualizados correctamente!"
        } else {
            "¡Perfil actualizado correctamente!"
        }

        Toast.makeText(this, message, Toast.LENGTH_LONG).show()

        // TODO: Aquí actualizarías los datos en tu sistema (API, UserManager, etc.)
        // if (passwordChanged) { // Actualizar contraseña }

        // Volver a la pantalla anterior
        finish()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}