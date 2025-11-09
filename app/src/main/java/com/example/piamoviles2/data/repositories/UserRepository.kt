package com.example.piamoviles2.data.repositories

import com.example.piamoviles2.data.api.ApiService
import com.example.piamoviles2.data.models.*
import com.example.piamoviles2.data.network.NetworkConfig
import retrofit2.Response

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


// REGISTRO DE USUARIO - VERSIÓN CORREGIDA
    suspend fun registrarUsuario(
        email: String,
        alias: String,
        contrasena: String,
        nombre: String,
        apellidoPaterno: String,
        apellidoMaterno: String? = null,
        telefono: String? = null,
        direccion: String? = null,
        fotoPerfil: String? = null
    ): Result<UsuarioResponse> {
        android.util.Log.d("API_DEBUG", "=== UserRepository.registrarUsuario ===")
        android.util.Log.d("API_DEBUG", "Email: $email")
        android.util.Log.d("API_DEBUG", "Alias: $alias")

        return try {
            // Crear el request object
            val request = UsuarioCreateRequest(
                email = email,
                alias = alias,
                contrasena = contrasena,
                nombre = nombre,
                apellidoPaterno = apellidoPaterno,
                apellidoMaterno = apellidoMaterno,
                telefono = telefono,
                direccion = direccion,
                fotoPerfil = fotoPerfil
            )

            android.util.Log.d("API_DEBUG", "Haciendo llamada a apiService.registrarUsuario...")
            val response = apiService.registrarUsuario(request)
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