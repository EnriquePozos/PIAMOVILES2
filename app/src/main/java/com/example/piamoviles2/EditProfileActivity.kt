package com.example.piamoviles2

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.piamoviles2.databinding.ActivityEditProfileBinding

class EditProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityEditProfileBinding.inflate(layoutInflater)
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

        // Los campos de contraseña están vacíos inicialmente para seguridad
        binding.etCurrentPassword.setText("")
        binding.etNewPassword.setText("")

        // Hacer campos opcionales más visibles
        binding.etPhone.hint = "Teléfono (opcional)"
        binding.etAddress.hint = "Dirección (opcional)"
    }

    private fun loadCurrentUserData() {
        // Cargar datos personales del usuario actual (datos de ejemplo basados en imagen)
        binding.etName.setText("Enrique")
        binding.etLastNamePaternal.setText("Pozos")
        binding.etLastNameMaternal.setText("González")
        binding.etEmail.setText("enriquepozos@gmail.com") // Email bloqueado
        binding.etPhone.setText("8125785504")
        binding.etAddress.setText("Villa Rica #122")
        binding.etAlias.setText("@Pozos___")

        // Los campos de contraseña se mantienen vacíos por seguridad
        binding.etCurrentPassword.setText("")
        binding.etNewPassword.setText("")

        // Cargar imagen actual
        binding.ivProfileImage.setImageResource(R.mipmap.ic_launcher)
    }

    private fun setupClickListeners() {
        // Selector de imagen de perfil
        binding.ivProfileImage.setOnClickListener {
            Toast.makeText(this, "Cambiar imagen (próximamente)", Toast.LENGTH_SHORT).show()
            // TODO: Implementar selector de imagen
        }

        // Botón actualizar datos personales
        binding.btnUpdatePersonalData.setOnClickListener {
            savePersonalDataChanges()
        }

        // Botón cambiar contraseña
        binding.btnChangePassword.setOnClickListener {
            changePassword()
        }
    }

    private fun savePersonalDataChanges() {
        val nombre = binding.etName.text.toString().trim()
        val apellidoPaterno = binding.etLastNamePaternal.text.toString().trim()
        val apellidoMaterno = binding.etLastNameMaternal.text.toString().trim()
        val telefono = binding.etPhone.text.toString().trim()
        val direccion = binding.etAddress.text.toString().trim()
        val alias = binding.etAlias.text.toString().trim()

        // Validaciones para datos personales
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

        // Simular guardado exitoso de datos personales
        performPersonalDataUpdate()
    }

    private fun changePassword() {
        val currentPassword = binding.etCurrentPassword.text.toString().trim()
        val newPassword = binding.etNewPassword.text.toString().trim()

        // Validaciones para contraseña
        if (currentPassword.isEmpty()) {
            binding.etCurrentPassword.error = "Ingresa tu contraseña actual"
            binding.etCurrentPassword.requestFocus()
            return
        }

        if (newPassword.isEmpty()) {
            binding.etNewPassword.error = "Ingresa la nueva contraseña"
            binding.etNewPassword.requestFocus()
            return
        }

        if (currentPassword == newPassword) {
            binding.etNewPassword.error = "La nueva contraseña debe ser diferente a la actual"
            binding.etNewPassword.requestFocus()
            return
        }

        // Validar formato de nueva contraseña
        if (!isValidPassword(newPassword)) {
            binding.etNewPassword.error = "La contraseña debe tener mínimo 10 caracteres, una mayúscula, una minúscula y un número"
            binding.etNewPassword.requestFocus()
            return
        }

        // TODO: Aquí validarías que la contraseña actual sea correcta con tu sistema de autenticación
        if (!validateCurrentPassword(currentPassword)) {
            binding.etCurrentPassword.error = "La contraseña actual es incorrecta"
            binding.etCurrentPassword.requestFocus()
            return
        }

        // Simular cambio exitoso de contraseña
        performPasswordChange()
    }

    private fun validateCurrentPassword(currentPassword: String): Boolean {
        // TODO: Implementar validación real con tu sistema de autenticación
        // Por ahora simulamos que es válida
        return true
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

    private fun performPersonalDataUpdate() {
        // Mostrar loading en botón de datos personales
        binding.btnUpdatePersonalData.isEnabled = false
        binding.btnUpdatePersonalData.text = "Actualizando..."

        // Simular delay de actualización
        binding.btnUpdatePersonalData.postDelayed({
            // Restaurar botón
            binding.btnUpdatePersonalData.isEnabled = true
            binding.btnUpdatePersonalData.text = "Actualizar Datos Personales"

            Toast.makeText(this, "¡Datos personales actualizados correctamente!", Toast.LENGTH_LONG).show()

            // TODO: Aquí actualizarías los datos personales en tu sistema (API, UserManager, etc.)

        }, 1500)
    }

    private fun performPasswordChange() {
        // Mostrar loading en botón de contraseña
        binding.btnChangePassword.isEnabled = false
        binding.btnChangePassword.text = "Cambiando..."

        // Simular delay de actualización
        binding.btnChangePassword.postDelayed({
            // Restaurar botón
            binding.btnChangePassword.isEnabled = true
            binding.btnChangePassword.text = "Cambiar Contraseña"

            // Limpiar campos de contraseña por seguridad
            binding.etCurrentPassword.setText("")
            binding.etNewPassword.setText("")

            Toast.makeText(this, "¡Contraseña cambiada correctamente!", Toast.LENGTH_LONG).show()

            // TODO: Aquí actualizarías la contraseña en tu sistema (API, UserManager, etc.)

        }, 1500)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}