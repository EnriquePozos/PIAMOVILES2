package com.example.piamoviles2.data.repositories

import com.example.piamoviles2.data.api.ApiService
import com.example.piamoviles2.data.models.*
import com.example.piamoviles2.data.network.NetworkConfig
import com.example.piamoviles2.Post // ✅ AGREGAR IMPORT
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
    // OBTENER FEED DE PUBLICACIONES (ORIGINAL)
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
// ✅ NUEVO: OBTENER FEED CON CONVERSIÓN A POST
// ============================================
    suspend fun obtenerFeedConvertido(token: String, currentUserId: String): Result<List<Post>> {
        return try {
            android.util.Log.d("PUBLICACION_REPO_DEBUG", "=== obtenerFeedConvertido ===")
            android.util.Log.d("PUBLICACION_REPO_DEBUG", "Current User ID: $currentUserId")

            val authHeader = "Bearer $token"
            val response = apiService.obtenerFeedPublicaciones(authHeader)

            android.util.Log.d("PUBLICACION_REPO_DEBUG", "Response code: ${response.code()}")

            if (response.isSuccessful) {
                response.body()?.let { feedList ->
                    android.util.Log.d("PUBLICACION_REPO_DEBUG", "✅ Feed obtenido: ${feedList.size} publicaciones")

                    // Convertir PublicacionListFeed a Post
                    val posts = feedList.mapIndexed { index, publicacion ->
                        Post(
                            id = (publicacion.id.hashCode().takeIf { it > 0 } ?: (1000 + index)), // ID local
                            apiId = publicacion.id, // ✅ AGREGAR: ID real de la API
                            title = publicacion.titulo,
                            description = publicacion.descripcion ?: "",
                            imageUrl = publicacion.imagenPreview ?: "default_recipe",
                            author = "@${publicacion.autorAlias ?: "Usuario"}",
                            createdAt = formatearFecha(publicacion.fechaPublicacion),
                            isOwner = publicacion.idAutor == currentUserId,
                            isFavorite = false, // TODO: Implementar lógica de favoritos
                            isDraft = false, // Solo publicadas en el feed
                            likesCount = publicacion.totalReacciones,
                            commentsCount = publicacion.totalComentarios
                        )
                    }

                    android.util.Log.d("PUBLICACION_REPO_DEBUG", "✅ Convertido a ${posts.size} Posts con API IDs")
                    Result.success(posts)
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

            // ✅ USAR ENDPOINTS CORRECTOS QUE SÍ EXISTEN EN EL BACKEND
            val response = if (incluirBorradores) {
                // Llamar endpoint de borradores
                android.util.Log.d("PUBLICACION_REPO_DEBUG", "Llamando a obtenerBorradoresUsuario")
                apiService.obtenerBorradoresUsuario(
                    idUsuario = idAutor,
                    authorization = authHeader
                )
            } else {
                // Llamar endpoint de publicaciones activas
                android.util.Log.d("PUBLICACION_REPO_DEBUG", "Llamando a obtenerPublicacionesActivasUsuario")
                apiService.obtenerPublicacionesActivasUsuario(
                    idUsuario = idAutor,
                    authorization = authHeader
                )
            }

            android.util.Log.d("PUBLICACION_REPO_DEBUG", "Response code: ${response.code()}")
            android.util.Log.d("PUBLICACION_REPO_DEBUG", "Response successful: ${response.isSuccessful}")

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
// ✅ NUEVO: OBTENER PUBLICACIONES USUARIO CONVERTIDAS A POST
// ============================================
    suspend fun obtenerPublicacionesUsuarioConvertidas(
        idAutor: String,
        incluirBorradores: Boolean = false,
        token: String
    ): Result<List<Post>> {
        return try {
            android.util.Log.d("PUBLICACION_REPO_DEBUG", "=== obtenerPublicacionesUsuarioConvertidas ===")

            // Obtener publicaciones del usuario
            val result = obtenerPublicacionesUsuario(idAutor, incluirBorradores, token)

            result.fold(
                onSuccess = { publicacionesList ->
                    // Convertir a Post
                    val posts = publicacionesList.mapIndexed { index, publicacion ->
                        Post(
                            id = (publicacion.id.hashCode().takeIf { it > 0 } ?: (2000 + index)),
                            apiId = publicacion.id, // ✅ AGREGAR: ID real de la API
                            title = publicacion.titulo,
                            description = publicacion.descripcion ?: "",
                            imageUrl = publicacion.imagenPreview ?: "default_recipe",
                            author = "@${publicacion.autorAlias ?: "Usuario"}",
                            createdAt = formatearFecha(publicacion.fechaPublicacion),
                            isOwner = true, // Siempre true para publicaciones del usuario
                            isFavorite = false, // TODO: Implementar lógica de favoritos
                            isDraft = publicacion.estatus == "borrador",
                            likesCount = publicacion.totalReacciones,
                            commentsCount = publicacion.totalComentarios
                        )
                    }

                    android.util.Log.d("PUBLICACION_REPO_DEBUG", "✅ Convertidas ${posts.size} publicaciones de usuario con API IDs")
                    Result.success(posts)
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            android.util.Log.e("PUBLICACION_REPO_DEBUG", "❌ Exception: ${e.message}")
            Result.failure(e)
        }
    }

    // ============================================
    // ✅ HELPER PARA FORMATEAR FECHAS
    // ============================================
    private fun formatearFecha(fechaISO: String?): String {
        return try {
            if (fechaISO.isNullOrEmpty()) {
                "Fecha no disponible"
            } else {
                // Convertir de formato ISO a formato legible
                // "2025-11-23T10:30:00" -> "23/11/2025 10:30"
                val partes = fechaISO.split("T")
                if (partes.size == 2) {
                    val fechaParte = partes[0] // "2025-11-23"
                    val horaParte = partes[1] // "10:30:00"

                    // Convertir fecha
                    val fechaArray = fechaParte.split("-")
                    if (fechaArray.size == 3) {
                        val año = fechaArray[0]
                        val mes = fechaArray[1]
                        val dia = fechaArray[2]

                        // Convertir hora
                        val hora = horaParte.substring(0, 5) // Solo HH:mm

                        "$dia/$mes/$año $hora"
                    } else {
                        fechaISO
                    }
                } else {
                    fechaISO
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("PUBLICACION_REPO_DEBUG", "Error al formatear fecha: $fechaISO")
            "Fecha inválida"
        }
    }

    // ============================================
    // ✅ HELPER PARA MANEJAR IMÁGENES POR DEFECTO
    // ============================================
    private fun getDefaultImageForRecipe(titulo: String): String {
        return when {
            titulo.contains("taco", ignoreCase = true) -> "sample_tacos"
            titulo.contains("ensalada", ignoreCase = true) -> "sample_salad"
            titulo.contains("pasta", ignoreCase = true) -> "sample_pasta"
            titulo.contains("sandwich", ignoreCase = true) -> "sample_sandwich"
            else -> "default_recipe"
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