package com.example.piamoviles2

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.piamoviles2.databinding.ActivityEditProfileBinding
import com.example.piamoviles2.data.repositories.UserRepository
import com.example.piamoviles2.utils.SessionManager
import com.example.piamoviles2.utils.ImageUtils
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

class EditProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfileBinding
    private lateinit var userRepository: UserRepository
    private lateinit var sessionManager: SessionManager
    private var currentUserId: String? = null
    private var currentUserToken: String? = null

    //   MANEJO DE IMAGEN
    private lateinit var imagePickerHelper: ImagePickerHelper
    private var selectedImageBitmap: Bitmap? = null
    private var selectedImageBase64: String? = null
    private var hasImageChanged = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userRepository = UserRepository()
        sessionManager = SessionManager(this)

        setupHeader()
        setupImagePicker()
        setupFieldsForEdit()
        loadUserCredentials()
        loadCurrentUserData()
        setupClickListeners()
    }

    private fun setupHeader() {
        val headerView = findViewById<View>(R.id.headerApp)
        HeaderUtils.setupHeaderWithBackButton(this, headerView)
    }

    private fun setupImagePicker() {
        imagePickerHelper = ImagePickerHelper(this) { bitmap ->
            bitmap?.let {
                // Redimensionar imagen para optimizar
                selectedImageBitmap = ImagePickerHelper.resizeBitmap(it, 400, 400)

                // Mostrar preview en ImageView
                binding.ivProfileImage.setImageBitmap(selectedImageBitmap)

                // Convertir a Base64 para envío
                selectedImageBase64 = bitmapToBase64(selectedImageBitmap!!)
                hasImageChanged = true

                android.util.Log.d("EDIT_PROFILE_DEBUG", "Nueva imagen seleccionada")
                android.util.Log.d("EDIT_PROFILE_DEBUG", "Base64 length: ${selectedImageBase64?.length}")

                Toast.makeText(this, "Nueva imagen seleccionada", Toast.LENGTH_SHORT).show()
            } ?: run {
                Toast.makeText(this, "Error al seleccionar imagen", Toast.LENGTH_SHORT).show()
            }
        }
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

    private fun loadUserCredentials() {
        //   USAR SESSIONMANAGER EN LUGAR DE SHAREDPREFERENCES
        if (!sessionManager.isLoggedIn()) {
            Toast.makeText(this, "Error: Sesión no válida", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        currentUserToken = sessionManager.getAccessToken()
        val currentUser = sessionManager.getCurrentUser()
        currentUserId = currentUser?.id

        if (currentUserId == null || currentUserToken == null) {
            Toast.makeText(this, "Error: No se pudieron cargar los datos de sesión", Toast.LENGTH_SHORT).show()
            finish()
        }

        //   LOGS PARA DEBUGGING
        android.util.Log.d("EDIT_PROFILE_DEBUG", "User ID: $currentUserId")
        android.util.Log.d("EDIT_PROFILE_DEBUG", "Token exists: ${currentUserToken != null}")
        android.util.Log.d("EDIT_PROFILE_DEBUG", "Token length: ${currentUserToken?.length ?: 0}")
    }

    private fun loadCurrentUserData() {
        currentUserId?.let { userId ->
            currentUserToken?.let { token ->
                lifecycleScope.launch {
                    try {
                        // Mostrar loading
                        showLoading(true)

                        val result = userRepository.obtenerPerfil(userId, token)

                        result.onSuccess { usuario ->
                            runOnUiThread {
                                // Cargar datos en la UI
                                binding.etName.setText(usuario.nombre)
                                binding.etLastNamePaternal.setText(usuario.apellidoPaterno)
                                binding.etLastNameMaternal.setText(usuario.apellidoMaterno ?: "")
                                binding.etEmail.setText(usuario.email)
                                binding.etPhone.setText(usuario.telefono ?: "")
                                binding.etAddress.setText(usuario.direccion ?: "")
                                binding.etAlias.setText(usuario.alias)

                                //   CARGAR IMAGEN CON GLIDE
                                ImageUtils.loadImage(
                                    context = this@EditProfileActivity,
                                    imageUrl = usuario.fotoPerfil,
                                    imageView = binding.ivProfileImage,
                                    placeholderResId = R.drawable.ic_add_photo
                                )

                                android.util.Log.d("EDIT_PROFILE_DEBUG", "Datos cargados correctamente")
                                android.util.Log.d("EDIT_PROFILE_DEBUG", "URL imagen: ${usuario.fotoPerfil}")
                            }
                        }.onFailure { error ->
                            runOnUiThread {
                                Toast.makeText(
                                    this@EditProfileActivity,
                                    "Error al cargar perfil: ${error.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                                android.util.Log.e("EDIT_PROFILE_DEBUG", "Error: ${error.message}")
                            }
                        }
                    } catch (e: Exception) {
                        runOnUiThread {
                            Toast.makeText(
                                this@EditProfileActivity,
                                "Error inesperado: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                            android.util.Log.e("EDIT_PROFILE_DEBUG", "Exception: ${e.message}")
                        }
                    } finally {
                        runOnUiThread {
                            showLoading(false)
                        }
                    }
                }
            }
        }
    }

    private fun setupClickListeners() {
        //   SELECTOR DE IMAGEN CON DIALOG
        binding.ivProfileImage.setOnClickListener {
            showImagePickerDialog()
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

    private fun showImagePickerDialog() {
        val options = arrayOf("Tomar foto", "Seleccionar de galería", "Cancelar")

        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Cambiar imagen de perfil")
        builder.setItems(options) { dialog, which ->
            when (which) {
                0 -> {
                    // Tomar foto
                    imagePickerHelper.openCamera()
                }
                1 -> {
                    // Seleccionar de galería
                    imagePickerHelper.openGallery()
                }
                2 -> {
                    // Cancelar
                    dialog.dismiss()
                }
            }
        }
        builder.show()
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return android.util.Base64.encodeToString(byteArray, android.util.Base64.DEFAULT)
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



        // Llamar método con imagen
        performPersonalDataUpdateWithImage(nombre, apellidoPaterno, apellidoMaterno, telefono, direccion, alias)
    }

    private fun performPersonalDataUpdateWithImage(
        nombre: String,
        apellidoPaterno: String,
        apellidoMaterno: String,
        telefono: String,
        direccion: String,
        alias: String
    ) {
        currentUserId?.let { userId ->
            currentUserToken?.let { token ->
                lifecycleScope.launch {
                    try {
                        runOnUiThread {
                            binding.btnUpdatePersonalData.isEnabled = false
                            if (hasImageChanged) {
                                binding.btnUpdatePersonalData.text = "Actualizando datos e imagen..."
                            } else {
                                binding.btnUpdatePersonalData.text = "Actualizando datos..."
                            }
                        }

                        //   DECIDIR QUE MÉTODO USAR SEGÚN SI HAY IMAGEN NUEVA
                        val result = if (hasImageChanged && selectedImageBase64 != null) {
                            // Actualizar con imagen
                            userRepository.actualizarUsuarioConImagen(
                                userId, token, nombre, apellidoPaterno, apellidoMaterno,
                                telefono, direccion, alias, selectedImageBase64!!
                            )
                        } else {
                            // Actualizar sin imagen
                            userRepository.actualizarDatosPersonales(
                                userId, token, nombre, apellidoPaterno, apellidoMaterno,
                                telefono, direccion, alias
                            )
                        }

                        result.onSuccess { usuarioActualizado ->
                            runOnUiThread {
                                //   ACTUALIZAR SESSIONMANAGER CON LOS NUEVOS DATOS
                                val currentToken = sessionManager.getAccessToken()

                                if (currentToken != null) {
                                    sessionManager.saveLoginData(currentToken, usuarioActualizado)
                                    android.util.Log.d("EDIT_PROFILE_DEBUG", "SessionManager actualizado")
                                    android.util.Log.d("EDIT_PROFILE_DEBUG", "Nueva imagen URL: ${usuarioActualizado.fotoPerfil}")

                                    //   ACTUALIZAR IMAGEN EN LA PANTALLA ACTUAL
                                    ImageUtils.loadImage(
                                        context = this@EditProfileActivity,
                                        imageUrl = usuarioActualizado.fotoPerfil,
                                        imageView = binding.ivProfileImage,
                                        placeholderResId = R.drawable.ic_add_photo
                                    )

                                    hasImageChanged = false // Reset flag
                                }

                                Toast.makeText(
                                    this@EditProfileActivity,
                                    "¡Perfil actualizado correctamente!",
                                    Toast.LENGTH_LONG
                                ).show()

                                // Finalizar activity para que ProfileActivity se refresque
                                finish()
                            }
                        }.onFailure { error ->
                            runOnUiThread {
                                Toast.makeText(
                                    this@EditProfileActivity,
                                    "Error: ${error.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                                android.util.Log.e("EDIT_PROFILE_DEBUG", "Error al actualizar: ${error.message}")
                            }
                        }
                    } catch (e: Exception) {
                        runOnUiThread {
                            Toast.makeText(
                                this@EditProfileActivity,
                                "Error inesperado: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                            android.util.Log.e("EDIT_PROFILE_DEBUG", "Exception: ${e.message}")
                        }
                    } finally {
                        runOnUiThread {
                            binding.btnUpdatePersonalData.isEnabled = true
                            binding.btnUpdatePersonalData.text = "Actualizar Datos Personales"
                        }
                    }
                }
            }
        }
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

        // Llamar a la API
        performPasswordChange(currentPassword, newPassword)
    }

    private fun performPasswordChange(currentPassword: String, newPassword: String) {
        currentUserId?.let { userId ->
            currentUserToken?.let { token ->
                lifecycleScope.launch {
                    try {
                        runOnUiThread {
                            binding.btnChangePassword.isEnabled = false
                            binding.btnChangePassword.text = "Cambiando..."
                        }

                        val result = userRepository.cambiarContrasena(userId, token, currentPassword, newPassword)

                        result.onSuccess { message ->
                            runOnUiThread {
                                binding.etCurrentPassword.setText("")
                                binding.etNewPassword.setText("")
                                Toast.makeText(
                                    this@EditProfileActivity,
                                    message,
                                    Toast.LENGTH_LONG
                                ).show()
                                android.util.Log.d("EDIT_PROFILE_DEBUG", "Contraseña cambiada: $message")
                            }
                        }.onFailure { error ->
                            runOnUiThread {
                                Toast.makeText(
                                    this@EditProfileActivity,
                                    "Error: ${error.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                                android.util.Log.e("EDIT_PROFILE_DEBUG", "Error al cambiar contraseña: ${error.message}")
                            }
                        }
                    } catch (e: Exception) {
                        runOnUiThread {
                            Toast.makeText(
                                this@EditProfileActivity,
                                "Error inesperado: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                            android.util.Log.e("EDIT_PROFILE_DEBUG", "Exception: ${e.message}")
                        }
                    } finally {
                        runOnUiThread {
                            binding.btnChangePassword.isEnabled = true
                            binding.btnChangePassword.text = "Cambiar Contraseña"
                        }
                    }
                }
            }
        }
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

    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            // Deshabilitar todos los campos mientras carga
            binding.etName.isEnabled = false
            binding.etLastNamePaternal.isEnabled = false
            binding.etLastNameMaternal.isEnabled = false
            binding.etPhone.isEnabled = false
            binding.etAddress.isEnabled = false
            binding.etAlias.isEnabled = false
            binding.btnUpdatePersonalData.isEnabled = false
            binding.btnChangePassword.isEnabled = false
            binding.ivProfileImage.isClickable = false
        } else {
            // Rehabilitar campos
            binding.etName.isEnabled = true
            binding.etLastNamePaternal.isEnabled = true
            binding.etLastNameMaternal.isEnabled = true
            binding.etPhone.isEnabled = true
            binding.etAddress.isEnabled = true
            binding.etAlias.isEnabled = true
            binding.btnUpdatePersonalData.isEnabled = true
            binding.btnChangePassword.isEnabled = true
            binding.ivProfileImage.isClickable = true

            // Email siempre deshabilitado
            binding.etEmail.isEnabled = false
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}