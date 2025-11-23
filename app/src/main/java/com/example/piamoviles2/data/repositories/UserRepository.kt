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




    // ============================================
// OBTENER PERFIL DE USUARIO                           TESTEAR
// ============================================
    suspend fun obtenerPerfil(usuarioId: String, token: String): Result<UsuarioResponse> {
        return try {
            val authHeader = "Bearer $token"
            val response = apiService.obtenerPerfilUsuario(usuarioId, authHeader)

            if (response.isSuccessful) {
                response.body()?.let { usuario ->
                    Result.success(usuario)
                } ?: Result.failure(Exception("Respuesta vacía del servidor"))
            } else {
                val errorMsg = parseErrorMessage(response)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            android.util.Log.e("UserRepository", "Error al obtener perfil: ${e.message}")
            Result.failure(e)
        }
    }

    // ============================================
// ACTUALIZAR DATOS PERSONALES - FORMDATA
// ============================================
    suspend fun actualizarDatosPersonales(
        usuarioId: String,
        token: String,
        nombre: String,
        apellidoPaterno: String,
        apellidoMaterno: String,
        telefono: String?,
        direccion: String?,
        alias: String
    ): Result<UsuarioResponse> {
        return try {
            android.util.Log.d("USER_REPO_DEBUG", "=== actualizarDatosPersonales (FormData) ===")
            android.util.Log.d("USER_REPO_DEBUG", "Usuario ID: $usuarioId")
            android.util.Log.d("USER_REPO_DEBUG", "Alias: $alias")

            val textMediaType = "text/plain".toMediaTypeOrNull()

            // Crear RequestBody para cada campo
            val nombreBody = RequestBody.create(textMediaType, nombre)
            val apellidoPaternoBody = RequestBody.create(textMediaType, apellidoPaterno)
            val apellidoMaternoBody = RequestBody.create(textMediaType, apellidoMaterno)
            val aliasBody = RequestBody.create(textMediaType, alias)
            val telefonoBody = telefono?.takeIf { it.isNotEmpty() }?.let { RequestBody.create(textMediaType, it) }
            val direccionBody = direccion?.takeIf { it.isNotEmpty() }?.let { RequestBody.create(textMediaType, it) }

            android.util.Log.d("USER_REPO_DEBUG", "RequestBodies creados, llamando a API...")

            val response = apiService.actualizarUsuario(
                usuarioId = usuarioId,
                authorization = "Bearer $token",
                email = null, // No cambiar email
                alias = aliasBody,
                nombre = nombreBody,
                apellidoPaterno = apellidoPaternoBody,
                apellidoMaterno = apellidoMaternoBody,
                telefono = telefonoBody,
                direccion = direccionBody,
                fotoPerfil = null // Sin imagen
            )

            if (response.isSuccessful && response.body() != null) {
                android.util.Log.d("USER_REPO_DEBUG", "✅ Actualización exitosa")
                Result.success(response.body()!!)
            } else {
                val errorMsg = parseErrorMessage(response)
                android.util.Log.e("USER_REPO_DEBUG", "❌ Error en respuesta: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            android.util.Log.e("USER_REPO_DEBUG", "❌ Exception: ${e.message}")
            Result.failure(e)
        }
    }

    // ============================================
// CAMBIAR CONTRASEÑA
// ============================================
    suspend fun cambiarContrasena(
        usuarioId: String,
        token: String,
        contrasenaActual: String,
        contrasenaNueva: String
    ): Result<String> {
        return try {
            val authHeader = "Bearer $token"
            val request = CambiarContrasenaRequest(
                contrasenaActual = contrasenaActual,
                contrasenaNueva = contrasenaNueva
            )

            val response = apiService.cambiarContrasena(usuarioId, request, authHeader)

            if (response.isSuccessful) {
                response.body()?.let { resultado ->
                    Result.success(resultado.message)
                } ?: Result.failure(Exception("Respuesta vacía del servidor"))
            } else {
                val errorMsg = parseErrorMessage(response)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            android.util.Log.e("UserRepository", "Error al cambiar contraseña: ${e.message}")
            Result.failure(e)
        }
    }

    //ACTUALIZAR CON IMAGEN - FORMDATA
    suspend fun actualizarUsuarioConImagen(
        usuarioId: String,
        token: String,
        nombre: String,
        apellidoPaterno: String,
        apellidoMaterno: String,
        telefono: String?,
        direccion: String?,
        alias: String,
        fotoPerfil: String // Base64 de la imagen
    ): Result<UsuarioResponse> {
        return try {
            android.util.Log.d("USER_REPO_DEBUG", "=== actualizarUsuarioConImagen (FormData) ===")
            android.util.Log.d("USER_REPO_DEBUG", "Usuario ID: $usuarioId")
            android.util.Log.d("USER_REPO_DEBUG", "Alias: $alias")
            android.util.Log.d("USER_REPO_DEBUG", "Tiene imagen: ${fotoPerfil.isNotEmpty()}")

            val textMediaType = "text/plain".toMediaTypeOrNull()
            val imageMediaType = "image/jpeg".toMediaTypeOrNull()

            // Crear RequestBody para campos de texto
            val nombreBody = RequestBody.create(textMediaType, nombre)
            val apellidoPaternoBody = RequestBody.create(textMediaType, apellidoPaterno)
            val apellidoMaternoBody = RequestBody.create(textMediaType, apellidoMaterno)
            val aliasBody = RequestBody.create(textMediaType, alias)
            val telefonoBody = telefono?.takeIf { it.isNotEmpty() }?.let { RequestBody.create(textMediaType, it) }
            val direccionBody = direccion?.takeIf { it.isNotEmpty() }?.let { RequestBody.create(textMediaType, it) }

            // Crear MultipartBody.Part para la imagen
            val imageBytes = android.util.Base64.decode(fotoPerfil, android.util.Base64.DEFAULT)
            val imageRequestBody = RequestBody.create(imageMediaType, imageBytes)
            val imagePart = MultipartBody.Part.createFormData("foto_perfil", "profile_image.jpg", imageRequestBody)

            android.util.Log.d("USER_REPO_DEBUG", "RequestBodies e imagen creados, llamando a API...")

            val response = apiService.actualizarUsuario(
                usuarioId = usuarioId,
                authorization = "Bearer $token",
                email = null, // No cambiar email
                alias = aliasBody,
                nombre = nombreBody,
                apellidoPaterno = apellidoPaternoBody,
                apellidoMaterno = apellidoMaternoBody,
                telefono = telefonoBody,
                direccion = direccionBody,
                fotoPerfil = imagePart // ✅ Con imagen
            )

            if (response.isSuccessful && response.body() != null) {
                android.util.Log.d("USER_REPO_DEBUG", "✅ Actualización con imagen exitosa")
                android.util.Log.d("USER_REPO_DEBUG", "Nueva imagen URL: ${response.body()!!.fotoPerfil}")
                Result.success(response.body()!!)
            } else {
                val errorMsg = parseErrorMessage(response)
                android.util.Log.e("USER_REPO_DEBUG", "❌ Error en respuesta: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            android.util.Log.e("USER_REPO_DEBUG", "❌ Exception: ${e.message}")
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
            val contrasenaBody = RequestBody.create(textMediaType, contrasena)

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