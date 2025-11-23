package com.example.piamoviles2.data.repositories

import com.example.piamoviles2.data.api.ApiService
import com.example.piamoviles2.data.models.*
import com.example.piamoviles2.data.network.NetworkConfig
import retrofit2.Response
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File

class PublicacionRepository(
    private val apiService: ApiService = NetworkConfig.apiService
) {

    // ============================================
    // CREAR PUBLICACIÓN - FORMDATA CON ARCHIVOS
    // ============================================
    suspend fun crearPublicacion(
        titulo: String,
        descripcion: String?,
        estatus: String, // "borrador" o "publicada"
        idAutor: String,
        imagenes: List<File>?,
        token: String
    ): Result<PublicacionDetalle> {
        android.util.Log.d("PUBLICACION_REPO_DEBUG", "=== crearPublicacion ===")
        android.util.Log.d("PUBLICACION_REPO_DEBUG", "Título: $titulo")
        android.util.Log.d("PUBLICACION_REPO_DEBUG", "Estatus: $estatus")
        android.util.Log.d("PUBLICACION_REPO_DEBUG", "ID Autor: $idAutor")
        android.util.Log.d("PUBLICACION_REPO_DEBUG", "Imágenes: ${imagenes?.size ?: 0}")

        return try {
            val textMediaType = "text/plain".toMediaTypeOrNull()
            val imageMediaType = "image/jpeg".toMediaTypeOrNull()

            // Crear RequestBody para campos de texto
            val tituloBody = RequestBody.create(textMediaType, titulo)
            val estatusBody = RequestBody.create(textMediaType, estatus)
            val idAutorBody = RequestBody.create(textMediaType, idAutor)
            val descripcionBody = descripcion?.let { RequestBody.create(textMediaType, it) }

            // Crear MultipartBody.Part para las imágenes (si existen)
            val archivosParts = imagenes?.mapIndexed { index, file ->
                val requestFile = RequestBody.create(imageMediaType, file)
                MultipartBody.Part.createFormData("archivos", "imagen_$index.jpg", requestFile)
            }

            android.util.Log.d("PUBLICACION_REPO_DEBUG", "RequestBodies creados, llamando a API...")

            val response = apiService.crearPublicacion(
                titulo = tituloBody,
                descripcion = descripcionBody,
                estatus = estatusBody,
                idAutor = idAutorBody,
                archivos = archivosParts,
                authorization = "Bearer $token"
            )

            android.util.Log.d("PUBLICACION_REPO_DEBUG", "Response code: ${response.code()}")
            android.util.Log.d("PUBLICACION_REPO_DEBUG", "Response successful: ${response.isSuccessful}")

            if (response.isSuccessful && response.body() != null) {
                android.util.Log.d("PUBLICACION_REPO_DEBUG", "✅ Publicación creada exitosamente")
                Result.success(response.body()!!)
            } else {
                val errorMsg = parseErrorMessage(response)
                android.util.Log.e("PUBLICACION_REPO_DEBUG", "❌ Error en respuesta: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            android.util.Log.e("PUBLICACION_REPO_DEBUG", "❌ Exception: ${e.message}")
            Result.failure(e)
        }
    }

    // ============================================
    // OBTENER FEED DE PUBLICACIONES
    // ============================================
    suspend fun obtenerFeedPublicaciones(token: String): Result<List<PublicacionListFeed>> {
        return try {
            android.util.Log.d("PUBLICACION_REPO_DEBUG", "=== obtenerFeedPublicaciones ===")

            val authHeader = "Bearer $token"
            val response = apiService.obtenerFeedPublicaciones(authHeader)

            android.util.Log.d("PUBLICACION_REPO_DEBUG", "Response code: ${response.code()}")

            if (response.isSuccessful) {
                response.body()?.let { feed ->
                    android.util.Log.d("PUBLICACION_REPO_DEBUG", "✅ Feed obtenido: ${feed.size} publicaciones")
                    Result.success(feed)
                } ?: Result.failure(Exception("Respuesta vacía del servidor"))
            } else {
                val errorMsg = parseErrorMessage(response)
                android.util.Log.e("PUBLICACION_REPO_DEBUG", "❌ Error al obtener feed: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            android.util.Log.e("PUBLICACION_REPO_DEBUG", "❌ Exception: ${e.message}")
            Result.failure(e)
        }
    }

    // ============================================
    // OBTENER PUBLICACIÓN POR ID
    // ============================================
    suspend fun obtenerPublicacionPorId(idPublicacion: String, token: String): Result<PublicacionDetalle> {
        return try {
            android.util.Log.d("PUBLICACION_REPO_DEBUG", "=== obtenerPublicacionPorId ===")
            android.util.Log.d("PUBLICACION_REPO_DEBUG", "ID Publicación: $idPublicacion")

            val authHeader = "Bearer $token"
            val response = apiService.obtenerPublicacionPorId(idPublicacion, authHeader)

            if (response.isSuccessful) {
                response.body()?.let { publicacion ->
                    android.util.Log.d("PUBLICACION_REPO_DEBUG", "✅ Publicación obtenida: ${publicacion.titulo}")
                    Result.success(publicacion)
                } ?: Result.failure(Exception("Respuesta vacía del servidor"))
            } else {
                val errorMsg = parseErrorMessage(response)
                android.util.Log.e("PUBLICACION_REPO_DEBUG", "❌ Error: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            android.util.Log.e("PUBLICACION_REPO_DEBUG", "❌ Exception: ${e.message}")
            Result.failure(e)
        }
    }

    // ============================================
    // ACTUALIZAR PUBLICACIÓN
    // ============================================
    suspend fun actualizarPublicacion(
        idPublicacion: String,
        titulo: String?,
        descripcion: String?,
        estatus: String?,
        imagenes: List<File>?,
        token: String
    ): Result<PublicacionDetalle> {
        return try {
            android.util.Log.d("PUBLICACION_REPO_DEBUG", "=== actualizarPublicacion ===")
            android.util.Log.d("PUBLICACION_REPO_DEBUG", "ID Publicación: $idPublicacion")

            val textMediaType = "text/plain".toMediaTypeOrNull()
            val imageMediaType = "image/jpeg".toMediaTypeOrNull()

            // Crear RequestBody para campos que no son null
            val tituloBody = titulo?.let { RequestBody.create(textMediaType, it) }
            val descripcionBody = descripcion?.let { RequestBody.create(textMediaType, it) }
            val estatusBody = estatus?.let { RequestBody.create(textMediaType, it) }

            // Crear MultipartBody.Part para las imágenes (si existen)
            val archivosParts = imagenes?.mapIndexed { index, file ->
                val requestFile = RequestBody.create(imageMediaType, file)
                MultipartBody.Part.createFormData("archivos", "imagen_$index.jpg", requestFile)
            }

            val response = apiService.actualizarPublicacion(
                idPublicacion = idPublicacion,
                titulo = tituloBody,
                descripcion = descripcionBody,
                estatus = estatusBody,
                archivos = archivosParts,
                authorization = "Bearer $token"
            )

            if (response.isSuccessful && response.body() != null) {
                android.util.Log.d("PUBLICACION_REPO_DEBUG", "✅ Publicación actualizada exitosamente")
                Result.success(response.body()!!)
            } else {
                val errorMsg = parseErrorMessage(response)
                android.util.Log.e("PUBLICACION_REPO_DEBUG", "❌ Error: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            android.util.Log.e("PUBLICACION_REPO_DEBUG", "❌ Exception: ${e.message}")
            Result.failure(e)
        }
    }

    // ============================================
    // ELIMINAR PUBLICACIÓN
    // ============================================
    suspend fun eliminarPublicacion(idPublicacion: String, token: String): Result<String> {
        return try {
            android.util.Log.d("PUBLICACION_REPO_DEBUG", "=== eliminarPublicacion ===")
            android.util.Log.d("PUBLICACION_REPO_DEBUG", "ID Publicación: $idPublicacion")

            val authHeader = "Bearer $token"
            val response = apiService.eliminarPublicacion(idPublicacion, authHeader)

            if (response.isSuccessful) {
                response.body()?.let { resultado ->
                    android.util.Log.d("PUBLICACION_REPO_DEBUG", "✅ Publicación eliminada")
                    Result.success(resultado.message)
                } ?: Result.success("Publicación eliminada exitosamente")
            } else {
                val errorMsg = parseErrorMessage(response)
                android.util.Log.e("PUBLICACION_REPO_DEBUG", "❌ Error: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            android.util.Log.e("PUBLICACION_REPO_DEBUG", "❌ Exception: ${e.message}")
            Result.failure(e)
        }
    }

    // ============================================
    // OBTENER PUBLICACIONES DE USUARIO
    // ============================================
    suspend fun obtenerPublicacionesUsuario(
        idAutor: String,
        incluirBorradores: Boolean = false,
        token: String
    ): Result<List<PublicacionListFeed>> {
        return try {
            android.util.Log.d("PUBLICACION_REPO_DEBUG", "=== obtenerPublicacionesUsuario ===")
            android.util.Log.d("PUBLICACION_REPO_DEBUG", "ID Autor: $idAutor")
            android.util.Log.d("PUBLICACION_REPO_DEBUG", "Incluir borradores: $incluirBorradores")

            val authHeader = "Bearer $token"
            val response = apiService.obtenerPublicacionesUsuario(
                idAutor = idAutor,
                incluirBorradores = incluirBorradores,
                authorization = authHeader
            )

            if (response.isSuccessful) {
                response.body()?.let { publicaciones ->
                    android.util.Log.d("PUBLICACION_REPO_DEBUG", "✅ Publicaciones obtenidas: ${publicaciones.size}")
                    Result.success(publicaciones)
                } ?: Result.failure(Exception("Respuesta vacía del servidor"))
            } else {
                val errorMsg = parseErrorMessage(response)
                android.util.Log.e("PUBLICACION_REPO_DEBUG", "❌ Error: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            android.util.Log.e("PUBLICACION_REPO_DEBUG", "❌ Exception: ${e.message}")
            Result.failure(e)
        }
    }

    // ============================================
    // HELPER METHODS
    // ============================================
    private fun parseErrorMessage(response: Response<*>): String {
        return try {
            response.errorBody()?.string() ?: "Error desconocido"
        } catch (e: Exception) {
            "Error al procesar respuesta: ${response.code()}"
        }
    }
}
