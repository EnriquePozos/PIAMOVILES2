package com.example.piamoviles2

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.example.piamoviles2.databinding.ActivityRegisterBinding
import com.example.piamoviles2.data.repositories.UserRepository
import com.example.piamoviles2.data.models.UsuarioCreateRequest
import com.example.piamoviles2.utils.ValidationUtils
import android.graphics.Bitmap
import androidx.appcompat.app.AlertDialog
import java.io.ByteArrayOutputStream

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var userRepository: UserRepository

    private lateinit var imagePickerHelper: ImagePickerHelper
    private var selectedImageBitmap: Bitmap? = null
    private var selectedImageBase64: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializar repository
        userRepository = UserRepository()
        setupImagePicker()

        // Encontrar el header incluido y configurarlo
        val headerView = findViewById<View>(R.id.headerApp)
        HeaderUtils.setupHeaderWithBackButton(this, headerView)
        HeaderUtils.setupBasicHeader(headerView)

        setupClickListeners()
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

                android.util.Log.d("IMAGE_DEBUG", "Imagen seleccionada y procesada")
                android.util.Log.d("IMAGE_DEBUG", "Base64 length: ${selectedImageBase64?.length}")

                Toast.makeText(this, "Imagen seleccionada correctamente", Toast.LENGTH_SHORT).show()
            } ?: run {
                Toast.makeText(this, "Error al seleccionar imagen", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupClickListeners() {

        binding.ivProfileImage.setOnClickListener {
            showImagePickerDialog()
        }

        // Botón registrarse
        binding.btnRegister.setOnClickListener {
            validateAndRegister()
        }
    }

    private fun showImagePickerDialog() {
        val options = arrayOf("Tomar foto", "Seleccionar de galería", "Cancelar")

        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Seleccionar imagen de perfil")
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
        val byteArrayOutputStream = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return android.util.Base64.encodeToString(byteArray, android.util.Base64.DEFAULT)
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

        // Validaciones según requerimientos del proyecto
        if (!validateRequiredFields(email, alias, password, nombre, apellidoPaterno)) {
            return
        }

        // Validaciones específicas usando ValidationUtils
        if (!ValidationUtils.isValidEmail(email)) {
            showError("Formato de email inválido")
            return
        }

        if (!ValidationUtils.isValidPassword(password)) {
            showError(ValidationUtils.getPasswordErrorMessage(password) ?: "Contraseña inválida")
            return
        }

        if (!ValidationUtils.isValidAlias(alias)) {
            showError("El alias debe tener al menos 3 caracteres")
            return
        }

        if (!ValidationUtils.isValidName(nombre)) {
            showError("El nombre debe tener al menos 2 caracteres")
            return
        }

        if (!ValidationUtils.isValidName(apellidoPaterno)) {
            showError("El apellido paterno debe tener al menos 2 caracteres")
            return
        }

        if (!ValidationUtils.isValidPhone(telefono.ifBlank { null })) {
            showError("El teléfono debe tener 10 dígitos")
            return
        }

        // Si todas las validaciones pasan, proceder con el registro
        performNetworkRegistration(
            email, alias, password, nombre, apellidoPaterno,
            apellidoMaterno.ifBlank { null },
            telefono.ifBlank { null },
            direccion.ifBlank { null }
        )
    }

    private fun validateRequiredFields(
        email: String, alias: String, password: String,
        nombre: String, apellidoPaterno: String
    ): Boolean {
        when {
            email.isEmpty() -> {
                showError("El email es obligatorio")
                return false
            }
            alias.isEmpty() -> {
                showError("El alias es obligatorio")
                return false
            }
            password.isEmpty() -> {
                showError("La contraseña es obligatoria")
                return false
            }
            nombre.isEmpty() -> {
                showError("El nombre es obligatorio")
                return false
            }
            apellidoPaterno.isEmpty() -> {
                showError("El apellido paterno es obligatorio")
                return false
            }
        }
        return true
    }

    private fun performNetworkRegistration(
        email: String, alias: String, password: String,
        nombre: String, apellidoPaterno: String,
        apellidoMaterno: String?, telefono: String?, direccion: String?
    ) {
        android.util.Log.d("REGISTER_DEBUG", "=== INICIANDO REGISTRO ===")
        android.util.Log.d("REGISTER_DEBUG", "Email: $email")
        android.util.Log.d("REGISTER_DEBUG", "Alias: $alias")
        android.util.Log.d("REGISTER_DEBUG", "Tiene imagen: ${selectedImageBase64 != null}")

        lifecycleScope.launch {
            try {
                binding.btnRegister.isEnabled = false
                binding.btnRegister.text = "Registrando..."


                val result = userRepository.registrarUsuario(
                    email = email,
                    alias = alias,
                    contrasena = password,
                    nombre = nombre,
                    apellidoPaterno = apellidoPaterno,
                    apellidoMaterno = apellidoMaterno,
                    telefono = telefono,
                    direccion = direccion,
                    fotoPerfil = selectedImageBase64 //Enviar imagen si existe
                )

                result.fold(
                    onSuccess = { response ->
                        android.util.Log.d("REGISTER_DEBUG", "Registro exitoso: ${response.id}")
                        android.util.Log.d("REGISTER_DEBUG", "Foto perfil URL: ${response.fotoPerfil}")
                        showSuccess("¡Registro exitoso! Bienvenido a El sazón de Toto")
                        finish()
                    },
                    onFailure = { error ->
                        android.util.Log.e("REGISTER_DEBUG", "Error en registro: ${error.message}")
                        showError("Error en registro: ${error.message}")
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e("REGISTER_DEBUG", "Exception: ${e.message}")
                showError("Error inesperado: ${e.message}")
            } finally {
                binding.btnRegister.isEnabled = true
                binding.btnRegister.text = "Registrarse"
            }
        }
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun showSuccess(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}