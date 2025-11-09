package com.example.piamoviles2.data.repositories

import com.example.piamoviles2.data.api.ApiService
import com.example.piamoviles2.data.models.*
import com.example.piamoviles2.data.network.NetworkConfig
import retrofit2.Response
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull

class UserRepository(
    private val apiService: ApiService = NetworkConfig.apiService
) {

    suspend fun loginUsuario(email: String, contrasena: String): Result<LoginResponse> {
        android.util.Log.d("API_DEBUG", "=== UserRepository.loginUsuario ===")
        android.util.Log.d("API_DEBUG", "Email: $email")
        android.util.Log.d("API_DEBUG", "Contraseña length: ${contrasena.length}")

        return try {
            android.util.Log.d("API_DEBUG", "Haciendo llamada a apiService.loginUsuario...")
            // Llamada directa con parámetros (como estaba originalmente)
            val response = apiService.loginUsuario(email, contrasena)
            android.util.Log.d("API_DEBUG", "Response code: ${response.code()}")
            android.util.Log.d("API_DEBUG", "Response successful: ${response.isSuccessful}")

            if (response.isSuccessful && response.body() != null) {
                android.util.Log.d("API_DEBUG", "Login exitoso, retornando Success")
                Result.success(response.body()!!)
            } else {
                val errorMsg = parseErrorMessage(response)
                android.util.Log.e("API_DEBUG", "Error del servidor: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            android.util.Log.e("API_DEBUG", "Exception en loginUsuario: ${e.message}")
            android.util.Log.e("API_DEBUG", "Exception type: ${e.javaClass.simpleName}")
            Result.failure(e)
        }
    }




    suspend fun obtenerPerfil(usuarioId: String, token: String): Result<UsuarioResponse> {
        return try {
            val response = apiService.obtenerPerfil(usuarioId, "Bearer $token")
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMsg = parseErrorMessage(response)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // REGISTRO DE USUARIO - FORMDATA CON IMAGEN
    suspend fun registrarUsuario(
        email: String,
        alias: String,
        contrasena: String,
        nombre: String,
        apellidoPaterno: String,
        apellidoMaterno: String? = null,
        telefono: String? = null,
        direccion: String? = null,
        fotoPerfil: String? = null // Base64 de la imagen
    ): Result<UsuarioResponse> {
        android.util.Log.d("API_DEBUG", "=== UserRepository.registrarUsuario (FormData) ===")
        android.util.Log.d("API_DEBUG", "Email: $email")
        android.util.Log.d("API_DEBUG", "Alias: $alias")
        android.util.Log.d("API_DEBUG", "Tiene imagen: ${fotoPerfil != null}")

        return try {
            android.util.Log.d("API_DEBUG", "=== DEBUGGING CONTRASEÑA ===")
            android.util.Log.d("API_DEBUG", "Contraseña original: '$contrasena'")
            android.util.Log.d("API_DEBUG", "Contraseña length: ${contrasena.length}")
            android.util.Log.d("API_DEBUG", "Contraseña bytes: ${contrasena.toByteArray().contentToString()}")

            // Intentar diferentes enfoques
            val textMediaType = "text/plain".toMediaTypeOrNull()
            val imageMediaType = "image/jpeg".toMediaTypeOrNull()

            // ✅ OPCIÓN 1: Usar URLEncoder para escapar caracteres especiales
            val contrasenaEscapada = java.net.URLEncoder.encode(contrasena, "UTF-8")
            android.util.Log.d("API_DEBUG", "Contraseña escapada: '$contrasenaEscapada'")

            // Crear RequestBody para campos de texto
            val emailBody = RequestBody.create(textMediaType, email)
            val aliasBody = RequestBody.create(textMediaType, alias)
            val contrasenaBody = RequestBody.create(textMediaType, contrasenaEscapada) // ✅ Usar escapada

            val nombreBody = nombre?.let { RequestBody.create(textMediaType, it) }
            val apellidoPaternoBody = apellidoPaterno?.let { RequestBody.create(textMediaType, it) }
            val apellidoMaternoBody = apellidoMaterno?.let { RequestBody.create(textMediaType, it) }
            val telefonoBody = telefono?.let { RequestBody.create(textMediaType, it) }
            val direccionBody = direccion?.let { RequestBody.create(textMediaType, it) }

            // Crear MultipartBody.Part para la imagen (si existe)
            val fotoPart = fotoPerfil?.let { base64Image ->
                // Convertir Base64 a bytes
                val imageBytes = android.util.Base64.decode(base64Image, android.util.Base64.DEFAULT)
                val requestFile = RequestBody.create(imageMediaType, imageBytes)
                MultipartBody.Part.createFormData("foto_perfil", "profile.jpg", requestFile)
            }

            android.util.Log.d("API_DEBUG", "Haciendo llamada a apiService.registrarUsuario...")
            val response = apiService.registrarUsuario(
                email = emailBody,
                alias = aliasBody,
                contrasena = contrasenaBody,
                nombre = nombreBody,
                apellidoPaterno = apellidoPaternoBody,
                apellidoMaterno = apellidoMaternoBody,
                telefono = telefonoBody,
                direccion = direccionBody,
                foto_perfil = fotoPart
            )

            android.util.Log.d("API_DEBUG", "Response code: ${response.code()}")
            android.util.Log.d("API_DEBUG", "Response successful: ${response.isSuccessful}")

            if (response.isSuccessful && response.body() != null) {
                android.util.Log.d("API_DEBUG", "Registro exitoso")
                Result.success(response.body()!!)
            } else {
                val errorMsg = parseErrorMessage(response)
                android.util.Log.e("API_DEBUG", "Error del servidor: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            android.util.Log.e("API_DEBUG", "Exception en registrarUsuario: ${e.message}")
            Result.failure(e)
        }
    }

    private fun parseErrorMessage(response: Response<*>): String {
        return try {
            response.errorBody()?.string() ?: "Error desconocido"
        } catch (e: Exception) {
            "Error al procesar respuesta: ${response.code()}"
        }
    }
}