// Ubicación: app/src/main/java/com/example/piamoviles2/data/repositories/FavoritoRepository.kt

package com.example.piamoviles2.data.repositories

import android.util.Log
import com.example.piamoviles2.data.api.ApiService
import com.example.piamoviles2.data.models.*
import com.example.piamoviles2.data.network.NetworkConfig
import retrofit2.HttpException
import java.io.IOException

/**
 * Repository para gestionar favoritos de publicaciones
 * Maneja la comunicación con la API para agregar/quitar favoritos
 */
class FavoritoRepository(
    private val apiService: ApiService = NetworkConfig.apiService
) {

    companion object {
        private const val TAG = "FAVORITO_REPO_DEBUG"
    }

    // ============================================
    // MÉTODOS PRINCIPALES - FAVORITOS
    // ============================================

    /**
     * Agregar una publicación a favoritos del usuario
     * @param idUsuario ID del usuario
     * @param idPublicacion ID de la publicación a marcar como favorita
     * @param token Token de autenticación
     * @return Result<FavoritoResponse> - Éxito con datos del favorito o error
     */
    suspend fun agregarFavorito(
        idUsuario: String,
        idPublicacion: String,
        token: String
    ): Result<FavoritoResponse> {
        return try {
            Log.d(TAG, "=== agregarFavorito ===")
            Log.d(TAG, "Usuario: $idUsuario")
            Log.d(TAG, "Publicación: $idPublicacion")
            Log.d(TAG, "URL completa: /api/favoritos/add_favorito/$idUsuario/$idPublicacion")

            val authHeader = "Bearer $token"
            val response = apiService.agregarFavorito(
                idUsuario = idUsuario,
                idPublicacion = idPublicacion,
                authorization = authHeader

            )

            if (response.isSuccessful && response.body() != null) {
                val favoritoResponse = response.body()!!
                Log.d(TAG, "Favorito agregado exitosamente")
                Log.d(TAG, "Título publicación: ${favoritoResponse.publicacionTitulo}")
                Log.d(TAG, "Fecha guardado: ${favoritoResponse.fechaGuardado}")

                Result.success(favoritoResponse)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string())
                Log.e(TAG, "Error HTTP ${response.code()}: $errorMsg")
                Result.failure(Exception("Error al agregar favorito: $errorMsg"))
            }

        } catch (e: HttpException) {
            val errorMsg = "Error HTTP ${e.code()}: ${e.message()}"
            Log.e(TAG, errorMsg, e)
            Result.failure(Exception(errorMsg))
        } catch (e: IOException) {
            val errorMsg = "Error de conexión al agregar favorito"
            Log.e(TAG, errorMsg, e)
            Result.failure(Exception(errorMsg))
        } catch (e: Exception) {
            Log.e(TAG, "Error inesperado al agregar favorito: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Quitar una publicación de favoritos del usuario
     * @param idUsuario ID del usuario
     * @param idPublicacion ID de la publicación a quitar de favoritos
     * @param token Token de autenticación
     * @return Result<Boolean> - true si se quitó exitosamente, false/error en caso contrario
     */
    suspend fun quitarFavorito(
        idUsuario: String,
        idPublicacion: String,
        token: String
    ): Result<Boolean> {
        return try {
            Log.d(TAG, "=== quitarFavorito ===")
            Log.d(TAG, "Usuario: $idUsuario")
            Log.d(TAG, "Publicación: $idPublicacion")

            val authHeader = "Bearer $token"
            val response = apiService.quitarFavorito(
                idUsuario = idUsuario,
                idPublicacion = idPublicacion,
                authorization = authHeader
            )

            if (response.isSuccessful) {
                Log.d(TAG, "Favorito removido exitosamente")
                Result.success(true)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string())
                Log.e(TAG, "Error HTTP ${response.code()}: $errorMsg")

                // Si es 404, significa que no estaba en favoritos (considerarlo éxito)
                if (response.code() == 404) {
                    Log.w(TAG, "Favorito no encontrado - considerando como removido")
                    Result.success(true)
                } else {
                    Result.failure(Exception("Error al quitar favorito: $errorMsg"))
                }
            }

        } catch (e: HttpException) {
            val errorMsg = "Error HTTP ${e.code()}: ${e.message()}"
            Log.e(TAG, errorMsg, e)

            // Si es 404, no estaba en favoritos
            if (e.code() == 404) {
                Log.w(TAG, "Favorito no encontrado - considerando como removido")
                Result.success(true)
            } else {
                Result.failure(Exception(errorMsg))
            }
        } catch (e: IOException) {
            val errorMsg = "Error de conexión al quitar favorito"
            Log.e(TAG, errorMsg, e)
            Result.failure(Exception(errorMsg))
        } catch (e: Exception) {
            Log.e(TAG, "Error inesperado al quitar favorito: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Verificar si una publicación está en favoritos del usuario
     * (Simulado localmente - tu backend no tiene endpoint específico para esto)
     * @param idUsuario ID del usuario
     * @param idPublicacion ID de la publicación a verificar
     * @param token Token de autenticación
     * @return Result<Boolean> - true si está en favoritos, false si no
     */
    suspend fun verificarSiFavorito(
        idUsuario: String,
        idPublicacion: String,
        token: String
    ): Result<Boolean> {
        return try {
            Log.d(TAG, "=== verificarSiFavorito ===")
            Log.d(TAG, "Usuario: $idUsuario")
            Log.d(TAG, "Publicación: $idPublicacion")

            // Como tu backend no tiene endpoint específico para verificar,
            // podemos obtener la lista de favoritos del usuario y buscar la publicación
            val authHeader = "Bearer $token"
            val response = apiService.obtenerFavoritasUsuario(
                idUsuario = idUsuario,
                authorization = authHeader
            )

            if (response.isSuccessful && response.body() != null) {
                val favoritas = response.body()!!
                val esFavorita = favoritas.any { it.id == idPublicacion }

                Log.d(TAG, "Verificación completada: esFavorita=$esFavorita")
                Log.d(TAG, "Total favoritas del usuario: ${favoritas.size}")

                Result.success(esFavorita)
            } else {
                // Manejar 404 como "no tiene favoritos"
                if (response.code() == 404) {
                    Log.d(TAG, "Usuario no tiene favoritos (404 en response) - interpretando como false")
                    Result.success(false)
                } else {
                    val errorMsg = parseErrorMessage(response.errorBody()?.string())
                    Log.e(TAG, "Error HTTP ${response.code()}: $errorMsg")
                    Result.failure(Exception("Error al verificar favorito: $errorMsg"))
                }
            }
        } catch (e: HttpException) {
            if (e.code() == 404) {
                // 404 = Usuario no tiene favoritos = no es favorito
                Log.d(TAG, "Usuario no tiene favoritos (404) - interpretando como false")
                Result.success(false)
            } else {
                val errorMsg = "Error HTTP ${e.code()}: ${e.message()}"
                Log.e(TAG, errorMsg, e)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: IOException) {
            val errorMsg = "Error de conexión al verificar favorito"
            Log.e(TAG, errorMsg, e)
            Result.failure(Exception(errorMsg))
        } catch (e: Exception) {
            Log.e(TAG, "Error inesperado al verificar favorito: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ============================================
    // MÉTODOS DE CONVENIENCIA
    // ============================================

    /**
     * Toggle del estado de favorito (agregar si no está, quitar si está)
     * @param idUsuario ID del usuario
     * @param idPublicacion ID de la publicación
     * @param token Token de autenticación
     * @return Result<EstadoFavoritoResult> - Estado final después del toggle
     */
    suspend fun toggleFavorito(
        idUsuario: String,
        idPublicacion: String,
        token: String
    ): Result<EstadoFavoritoResult> {
        return try {
            Log.d(TAG, "=== toggleFavorito ===")

            // Primero verificar estado actual
            val verificacionResult = verificarSiFavorito(idUsuario, idPublicacion, token)

            verificacionResult.fold(
                onSuccess = { estaEnFavoritos ->
                    Log.d(TAG, "Estado actual: estaEnFavoritos=$estaEnFavoritos")

                    if (estaEnFavoritos) {
                        // Está en favoritos, quitarlo
                        quitarFavorito(idUsuario, idPublicacion, token).fold(
                            onSuccess = {
                                Log.d(TAG, "Toggle completado: REMOVIDO de favoritos")
                                Result.success(EstadoFavoritoResult(
                                    esFavorito = false,
                                    accion = "removido",
                                    mensaje = "Removido de favoritos"
                                ))
                            },
                            onFailure = { error ->
                                Log.e(TAG, "Error al quitar favorito en toggle: ${error.message}")
                                Result.failure(error)
                            }
                        )
                    } else {
                        // No está en favoritos, agregarlo
                        agregarFavorito(idUsuario, idPublicacion, token).fold(
                            onSuccess = { favoritoResponse ->
                                Log.d(TAG, "Toggle completado: AGREGADO a favoritos")
                                Result.success(EstadoFavoritoResult(
                                    esFavorito = true,
                                    accion = "agregado",
                                    mensaje = "Agregado a favoritos",
                                    favoritoData = favoritoResponse
                                ))
                            },
                            onFailure = { error ->
                                Log.e(TAG, "Error al agregar favorito en toggle: ${error.message}")
                                Result.failure(error)
                            }
                        )
                    }
                },
                onFailure = { error ->
                    //   CAMBIO PRINCIPAL: Manejar 404 como "no es favorito"
                    if (error.message?.contains("404") == true) {
                        Log.d(TAG, "404 en toggle - interpretando como no es favorito, procederemos a agregar")
                        // No está en favoritos (404), proceder a agregarlo
                        agregarFavorito(idUsuario, idPublicacion, token).fold(
                            onSuccess = { favoritoResponse ->
                                Log.d(TAG, "Toggle completado: AGREGADO a favoritos (desde 404)")
                                Result.success(EstadoFavoritoResult(
                                    esFavorito = true,
                                    accion = "agregado",
                                    mensaje = "Agregado a favoritos",
                                    favoritoData = favoritoResponse
                                ))
                            },
                            onFailure = { addError ->
                                Log.e(TAG, "Error al agregar favorito desde 404: ${addError.message}")
                                Result.failure(addError)
                            }
                        )
                    } else {
                        Log.e(TAG, "Error al verificar estado en toggle: ${error.message}")
                        Result.failure(error)
                    }
                }
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error inesperado en toggleFavorito: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ============================================
    // HELPERS PRIVADOS
    // ============================================

    /**
     * Parser de mensajes de error del API
     */
    private fun parseErrorMessage(errorBody: String?): String {
        return try {
            if (errorBody.isNullOrEmpty()) return "Error desconocido"

            // Si es JSON con formato {"detail": "mensaje"}
            if (errorBody.contains("detail")) {
                val detail = errorBody.substringAfter("\"detail\":\"").substringBefore("\"")
                if (detail.isNotEmpty()) return detail
            }

            // Si contiene mensaje directo
            errorBody.take(100) // Limitar longitud

        } catch (e: Exception) {
            Log.w(TAG, "Error al parsear mensaje de error: ${e.message}")
            "Error en la operación"
        }
    }

    /**
     * Clase de datos para representar el resultado de un toggle de favorito
     */
    data class EstadoFavoritoResult(
        val esFavorito: Boolean,
        val accion: String, // "agregado" o "removido"
        val mensaje: String,
        val favoritoData: FavoritoResponse? = null
    )
}