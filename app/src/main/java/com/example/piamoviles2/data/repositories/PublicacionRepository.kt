// Ubicación: app/src/main/java/com/example/piamoviles2/data/repositories/PublicacionRepository.kt

package com.example.piamoviles2.data.repositories

import com.example.piamoviles2.data.api.ApiService
import com.example.piamoviles2.data.models.*
import com.example.piamoviles2.data.network.NetworkConfig
import com.example.piamoviles2.Post
import retrofit2.Response
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File

class PublicacionRepository(
    private val apiService: ApiService = NetworkConfig.apiService
) {

    // CREAR PUBLICACIÓN - FORMDATA CON ARCHIVOS
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
                android.util.Log.d("PUBLICACION_REPO_DEBUG", " Publicación creada exitosamente")
                Result.success(response.body()!!)
            } else {
                val errorMsg = parseErrorMessage(response)
                android.util.Log.e("PUBLICACION_REPO_DEBUG", "  Error en respuesta: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            android.util.Log.e("PUBLICACION_REPO_DEBUG", "  Exception: ${e.message}")
            Result.failure(e)
        }
    }

    // OBTENER FEED DE PUBLICACIONES (ORIGINAL)
    suspend fun obtenerFeedPublicaciones(token: String): Result<List<PublicacionListFeed>> {
        return try {
            android.util.Log.d("PUBLICACION_REPO_DEBUG", "=== obtenerFeedPublicaciones ===")

            val authHeader = "Bearer $token"
            val response = apiService.obtenerFeedPublicaciones(authHeader)

            android.util.Log.d("PUBLICACION_REPO_DEBUG", "Response code: ${response.code()}")

            if (response.isSuccessful) {
                response.body()?.let { feed ->
                    android.util.Log.d("PUBLICACION_REPO_DEBUG", " Feed obtenido: ${feed.size} publicaciones")
                    Result.success(feed)
                } ?: Result.failure(Exception("Respuesta vacía del servidor"))
            } else {
                val errorMsg = parseErrorMessage(response)
                android.util.Log.e("PUBLICACION_REPO_DEBUG", "  Error al obtener feed: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            android.util.Log.e("PUBLICACION_REPO_DEBUG", "  Exception: ${e.message}")
            Result.failure(e)
        }
    }

    // OBTENER FEED CON CONVERSIÓN A POST
    suspend fun obtenerFeedConvertido(token: String, currentUserId: String): Result<List<Post>> {
        return try {
            android.util.Log.d("PUBLICACION_REPO_DEBUG", "=== obtenerFeedConvertido ===")
            android.util.Log.d("PUBLICACION_REPO_DEBUG", "Current User ID: $currentUserId")

            val authHeader = "Bearer $token"
            val response = apiService.obtenerFeedPublicaciones(authHeader)

            android.util.Log.d("PUBLICACION_REPO_DEBUG", "Response code: ${response.code()}")

            if (response.isSuccessful) {
                response.body()?.let { feedList ->
                    android.util.Log.d("PUBLICACION_REPO_DEBUG", "  Feed obtenido: ${feedList.size} publicaciones")

                    // Convertir PublicacionListFeed a Post
                    val posts = feedList.mapIndexed { index, publicacion ->
                        Post(
                            id = (publicacion.id.hashCode().takeIf { it > 0 } ?: (1000 + index)), // ID local
                            apiId = publicacion.id,
                            title = publicacion.titulo,
                            description = publicacion.descripcion ?: "",
                            imageUrl = publicacion.imagenPreview ?: "default_recipe",
                            author = "@${publicacion.autorAlias ?: "Usuario"}",
                            createdAt = formatearFecha(publicacion.fechaPublicacion),
                            isOwner = publicacion.idAutor == currentUserId,
                            isFavorite = false,
                            isDraft = false,
                            likesCount = publicacion.totalReacciones,
                            commentsCount = publicacion.totalComentarios
                        )
                    }

                    android.util.Log.d("PUBLICACION_REPO_DEBUG", "  Convertido a ${posts.size} Posts con API IDs")
                    Result.success(posts)
                } ?: Result.failure(Exception("Respuesta vacía del servidor"))
            } else {
                val errorMsg = parseErrorMessage(response)
                android.util.Log.e("PUBLICACION_REPO_DEBUG", "  Error al obtener feed: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            android.util.Log.e("PUBLICACION_REPO_DEBUG", "  Exception: ${e.message}")
            Result.failure(e)
        }
    }
    // OBTENER PUBLICACIÓN POR ID
    suspend fun obtenerPublicacionPorId(idPublicacion: String, token: String): Result<PublicacionDetalle> {
        return try {
            android.util.Log.d("PUBLICACION_REPO_DEBUG", "=== obtenerPublicacionPorId ===")
            android.util.Log.d("PUBLICACION_REPO_DEBUG", "ID Publicación: $idPublicacion")

            val authHeader = "Bearer $token"
            val response = apiService.obtenerPublicacionPorId(idPublicacion, authHeader)

            if (response.isSuccessful) {
                response.body()?.let { publicacion ->
                    android.util.Log.d("PUBLICACION_REPO_DEBUG", "  Publicación obtenida: ${publicacion.titulo}")
                    Result.success(publicacion)
                } ?: Result.failure(Exception("Respuesta vacía del servidor"))
            } else {
                val errorMsg = parseErrorMessage(response)
                android.util.Log.e("PUBLICACION_REPO_DEBUG", "  Error: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            android.util.Log.e("PUBLICACION_REPO_DEBUG", "  Exception: ${e.message}")
            Result.failure(e)
        }
    }

    //   OBTENER PUBLICACION DETALLE COMPLETA (PARA PANTALLA DE DETALLES) - CORREGIDO
    suspend fun obtenerPublicacionDetalleCompleta(
        idPublicacion: String,
        token: String
    ): Result<PublicacionDetalle> {
        return try {
            android.util.Log.d("PUBLICACION_REPO_DEBUG", "=== obtenerPublicacionDetalleCompleta ===")
            android.util.Log.d("PUBLICACION_REPO_DEBUG", "ID Publicación: $idPublicacion")

            val authHeader = "Bearer $token"
            val response = apiService.obtenerPublicacionPorId(idPublicacion, authHeader)

            if (response.isSuccessful) {
                response.body()?.let { publicacion ->
                    android.util.Log.d("PUBLICACION_REPO_DEBUG", "  Publicación obtenida: ${publicacion.titulo}")
                    android.util.Log.d("PUBLICACION_REPO_DEBUG", "Multimedia: ${publicacion.multimedia.size} items")
                    android.util.Log.d("PUBLICACION_REPO_DEBUG", "Estatus: ${publicacion.estatus}")
                    Result.success(publicacion)
                } ?: Result.failure(Exception("Respuesta vacía del servidor"))
            } else {
                val errorMsg = parseErrorMessage(response)
                android.util.Log.e("PUBLICACION_REPO_DEBUG", "  Error: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            android.util.Log.e("PUBLICACION_REPO_DEBUG", "  Exception: ${e.message}")
            Result.failure(e)
        }
    }

    // ACTUALIZAR PUBLICACIÓN
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
                android.util.Log.d("PUBLICACION_REPO_DEBUG", "  Publicación actualizada exitosamente")
                Result.success(response.body()!!)
            } else {
                val errorMsg = parseErrorMessage(response)
                android.util.Log.e("PUBLICACION_REPO_DEBUG", "  Error: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            android.util.Log.e("PUBLICACION_REPO_DEBUG", "  Exception: ${e.message}")
            Result.failure(e)
        }
    }

    // ELIMINAR PUBLICACIÓN
    suspend fun eliminarPublicacion(idPublicacion: String, token: String): Result<String> {
        return try {
            android.util.Log.d("PUBLICACION_REPO_DEBUG", "=== eliminarPublicacion ===")
            android.util.Log.d("PUBLICACION_REPO_DEBUG", "ID Publicación: $idPublicacion")

            val authHeader = "Bearer $token"
            val response = apiService.eliminarPublicacion(idPublicacion, authHeader)

            if (response.isSuccessful) {
                response.body()?.let { resultado ->
                    android.util.Log.d("PUBLICACION_REPO_DEBUG", "  Publicación eliminada")
                    Result.success(resultado.message)
                } ?: Result.success("Publicación eliminada exitosamente")
            } else {
                val errorMsg = parseErrorMessage(response)
                android.util.Log.e("PUBLICACION_REPO_DEBUG", "  Error: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            android.util.Log.e("PUBLICACION_REPO_DEBUG", "  Exception: ${e.message}")
            Result.failure(e)
        }
    }

    // OBTENER PUBLICACIONES DE USUARIO
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

            val response = if (incluirBorradores) {
                android.util.Log.d("PUBLICACION_REPO_DEBUG", "Llamando a obtenerBorradoresUsuario")
                apiService.obtenerBorradoresUsuario(
                    idUsuario = idAutor,
                    authorization = authHeader
                )
            } else {
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
                    android.util.Log.d("PUBLICACION_REPO_DEBUG", "  Publicaciones obtenidas: ${publicaciones.size}")
                    Result.success(publicaciones)
                } ?: Result.failure(Exception("Respuesta vacía del servidor"))
            } else {
                val errorMsg = parseErrorMessage(response)
                android.util.Log.e("PUBLICACION_REPO_DEBUG", "  Error: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            android.util.Log.e("PUBLICACION_REPO_DEBUG", "  Exception: ${e.message}")
            Result.failure(e)
        }
    }

    //OBTENER PUBLICACIONES USUARIO CONVERTIDAS A POST
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
                            apiId = publicacion.id,
                            title = publicacion.titulo,
                            description = publicacion.descripcion ?: "",
                            imageUrl = publicacion.imagenPreview ?: "default_recipe",
                            author = "@${publicacion.autorAlias ?: "Usuario"}",
                            createdAt = formatearFecha(publicacion.fechaPublicacion),
                            isOwner = true,
                            isFavorite = false,
                            isDraft = publicacion.estatus == "borrador",
                            likesCount = publicacion.totalReacciones,
                            commentsCount = publicacion.totalComentarios
                        )
                    }

                    android.util.Log.d("PUBLICACION_REPO_DEBUG", "  Convertidas ${posts.size} publicaciones de usuario con API IDs")
                    Result.success(posts)
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            android.util.Log.e("PUBLICACION_REPO_DEBUG", " Exception: ${e.message}")
            Result.failure(e)
        }
    }

    // MÉTODOS DE REACCIONES (LIKES/DISLIKES)

    /**
     * Añadir o actualizar reacción a una publicación
     * @param idPublicacion ID de la publicación
     * @param idUsuario ID del usuario que reacciona
     * @param tipoReaccion LIKE o DISLIKE
     * @param token Token de autorización
     * @return Result<ReaccionResponse>
     */
    suspend fun agregarReaccion(
        idPublicacion: String,
        idUsuario: String,
        tipoReaccion: TipoReaccion,
        token: String
    ): Result<ReaccionResponse> {
        return try {
            android.util.Log.d("PUBLICACION_REPO_DEBUG", "=== agregarReaccion ===")
            android.util.Log.d("PUBLICACION_REPO_DEBUG", "ID Publicación: $idPublicacion")
            android.util.Log.d("PUBLICACION_REPO_DEBUG", "ID Usuario: $idUsuario")
            android.util.Log.d("PUBLICACION_REPO_DEBUG", "Tipo Reacción: ${tipoReaccion.value}")

            val response = apiService.agregarReaccion(
                idPublicacion = idPublicacion,
                idUsuario = idUsuario,
                tipoReaccion = tipoReaccion.value,
                authorization = "Bearer $token"
            )

            android.util.Log.d("PUBLICACION_REPO_DEBUG", "Response code: ${response.code()}")

            if (response.isSuccessful && response.body() != null) {
                android.util.Log.d("PUBLICACION_REPO_DEBUG", "✅ Reacción agregada exitosamente")
                Result.success(response.body()!!)
            } else {
                val errorMsg = parseErrorMessage(response)
                android.util.Log.e("PUBLICACION_REPO_DEBUG", "❌ Error al agregar reacción: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            android.util.Log.e("PUBLICACION_REPO_DEBUG", "❌ Exception: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Eliminar reacción de una publicación
     * @param idPublicacion ID de la publicación
     * @param idUsuario ID del usuario
     * @param token Token de autorización
     * @return Result<Boolean> - true si se eliminó exitosamente
     */
    suspend fun eliminarReaccion(
        idPublicacion: String,
        idUsuario: String,
        token: String
    ): Result<Boolean> {
        return try {
            android.util.Log.d("PUBLICACION_REPO_DEBUG", "=== eliminarReaccion ===")
            android.util.Log.d("PUBLICACION_REPO_DEBUG", "ID Publicación: $idPublicacion")
            android.util.Log.d("PUBLICACION_REPO_DEBUG", "ID Usuario: $idUsuario")

            val response = apiService.eliminarReaccion(
                idPublicacion = idPublicacion,
                idUsuario = idUsuario,
                authorization = "Bearer $token"
            )

            android.util.Log.d("PUBLICACION_REPO_DEBUG", "Response code: ${response.code()}")

            if (response.isSuccessful) {
                android.util.Log.d("PUBLICACION_REPO_DEBUG", "✅ Reacción eliminada exitosamente")
                Result.success(true)
            } else {
                val errorMsg = parseErrorMessage(response)
                android.util.Log.e("PUBLICACION_REPO_DEBUG", "❌ Error al eliminar reacción: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            android.util.Log.e("PUBLICACION_REPO_DEBUG", "❌ Exception: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Obtener conteo de reacciones de una publicación
     * @param idPublicacion ID de la publicación
     * @param token Token de autorización
     * @return Result<ConteoReaccionesResponse>
     */
    suspend fun obtenerConteoReacciones(
        idPublicacion: String,
        token: String
    ): Result<ConteoReaccionesResponse> {
        return try {
            android.util.Log.d("PUBLICACION_REPO_DEBUG", "=== obtenerConteoReacciones ===")
            android.util.Log.d("PUBLICACION_REPO_DEBUG", "ID Publicación: $idPublicacion")

            val response = apiService.obtenerConteoReacciones(
                idPublicacion = idPublicacion,
                authorization = "Bearer $token"
            )

            android.util.Log.d("PUBLICACION_REPO_DEBUG", "Response code: ${response.code()}")

            if (response.isSuccessful && response.body() != null) {
                val conteo = response.body()!!
                android.util.Log.d("PUBLICACION_REPO_DEBUG", "✅ Conteo obtenido - Likes: ${conteo.likes}, Dislikes: ${conteo.dislikes}")
                Result.success(conteo)
            } else {
                val errorMsg = parseErrorMessage(response)
                android.util.Log.e("PUBLICACION_REPO_DEBUG", "❌ Error al obtener conteo: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            android.util.Log.e("PUBLICACION_REPO_DEBUG", "❌ Exception: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Verificar si el usuario ha reaccionado a una publicación y qué tipo de reacción
     * @param idPublicacion ID de la publicación
     * @param idUsuario ID del usuario
     * @param token Token de autorización
     * @return Result<VerificarReaccionResponse>
     */
    suspend fun verificarReaccionUsuario(
        idPublicacion: String,
        idUsuario: String,
        token: String
    ): Result<VerificarReaccionResponse> {
        return try {
            android.util.Log.d("PUBLICACION_REPO_DEBUG", "=== verificarReaccionUsuario ===")
            android.util.Log.d("PUBLICACION_REPO_DEBUG", "ID Publicación: $idPublicacion")
            android.util.Log.d("PUBLICACION_REPO_DEBUG", "ID Usuario: $idUsuario")

            val response = apiService.verificarReaccionUsuario(
                idPublicacion = idPublicacion,
                idUsuario = idUsuario,
                authorization = "Bearer $token"
            )

            android.util.Log.d("PUBLICACION_REPO_DEBUG", "Response code: ${response.code()}")

            if (response.isSuccessful && response.body() != null) {
                val verificacion = response.body()!!
                android.util.Log.d("PUBLICACION_REPO_DEBUG", "✅ Verificación obtenida - Tiene reacción: ${verificacion.tieneReaccion}, Tipo: ${verificacion.tipoReaccion}")
                Result.success(verificacion)
            } else {
                val errorMsg = parseErrorMessage(response)
                android.util.Log.e("PUBLICACION_REPO_DEBUG", "❌ Error al verificar reacción: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            android.util.Log.e("PUBLICACION_REPO_DEBUG", "❌ Exception: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Método de conveniencia para hacer toggle de like (agregar si no existe, eliminar si existe)
     * @param idPublicacion ID de la publicación
     * @param idUsuario ID del usuario
     * @param token Token de autorización
     * @return Result<VerificarReaccionResponse> - estado final de la reacción
     */
    suspend fun toggleLike(
        idPublicacion: String,
        idUsuario: String,
        token: String
    ): Result<VerificarReaccionResponse> {
        return try {
            android.util.Log.d("PUBLICACION_REPO_DEBUG", "=== toggleLike ===")

            // Primero verificar si ya tiene reacción
            val verificacionResult = verificarReaccionUsuario(idPublicacion, idUsuario, token)

            verificacionResult.fold(
                onSuccess = { verificacion ->
                    when {
                        verificacion.esLike() -> {
                            // Ya tiene like, eliminarlo
                            android.util.Log.d("PUBLICACION_REPO_DEBUG", "Usuario ya tiene LIKE, eliminando...")
                            eliminarReaccion(idPublicacion, idUsuario, token).fold(
                                onSuccess = {
                                    Result.success(VerificarReaccionResponse(false, null))
                                },
                                onFailure = { Result.failure(it) }
                            )
                        }
                        verificacion.esDislike() -> {
                            // Tiene dislike, cambiarlo a like
                            android.util.Log.d("PUBLICACION_REPO_DEBUG", "Usuario tiene DISLIKE, cambiando a LIKE...")
                            agregarReaccion(idPublicacion, idUsuario, TipoReaccion.LIKE, token).fold(
                                onSuccess = {
                                    Result.success(VerificarReaccionResponse(true, "like"))
                                },
                                onFailure = { Result.failure(it) }
                            )
                        }
                        else -> {
                            // No tiene reacción, agregar like
                            android.util.Log.d("PUBLICACION_REPO_DEBUG", "Usuario no tiene reacción, agregando LIKE...")
                            agregarReaccion(idPublicacion, idUsuario, TipoReaccion.LIKE, token).fold(
                                onSuccess = {
                                    Result.success(VerificarReaccionResponse(true, "like"))
                                },
                                onFailure = { Result.failure(it) }
                            )
                        }
                    }
                },
                onFailure = { Result.failure(it) }
            )
        } catch (e: Exception) {
            android.util.Log.e("PUBLICACION_REPO_DEBUG", "❌ Exception en toggleLike: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Método de conveniencia para hacer toggle de dislike
     * @param idPublicacion ID de la publicación
     * @param idUsuario ID del usuario
     * @param token Token de autorización
     * @return Result<VerificarReaccionResponse> - estado final de la reacción
     */
    suspend fun toggleDislike(
        idPublicacion: String,
        idUsuario: String,
        token: String
    ): Result<VerificarReaccionResponse> {
        return try {
            android.util.Log.d("PUBLICACION_REPO_DEBUG", "=== toggleDislike ===")

            // Primero verificar si ya tiene reacción
            val verificacionResult = verificarReaccionUsuario(idPublicacion, idUsuario, token)

            verificacionResult.fold(
                onSuccess = { verificacion ->
                    when {
                        verificacion.esDislike() -> {
                            // Ya tiene dislike, eliminarlo
                            android.util.Log.d("PUBLICACION_REPO_DEBUG", "Usuario ya tiene DISLIKE, eliminando...")
                            eliminarReaccion(idPublicacion, idUsuario, token).fold(
                                onSuccess = {
                                    Result.success(VerificarReaccionResponse(false, null))
                                },
                                onFailure = { Result.failure(it) }
                            )
                        }
                        verificacion.esLike() -> {
                            // Tiene like, cambiarlo a dislike
                            android.util.Log.d("PUBLICACION_REPO_DEBUG", "Usuario tiene LIKE, cambiando a DISLIKE...")
                            agregarReaccion(idPublicacion, idUsuario, TipoReaccion.DISLIKE, token).fold(
                                onSuccess = {
                                    Result.success(VerificarReaccionResponse(true, "dislike"))
                                },
                                onFailure = { Result.failure(it) }
                            )
                        }
                        else -> {
                            // No tiene reacción, agregar dislike
                            android.util.Log.d("PUBLICACION_REPO_DEBUG", "Usuario no tiene reacción, agregando DISLIKE...")
                            agregarReaccion(idPublicacion, idUsuario, TipoReaccion.DISLIKE, token).fold(
                                onSuccess = {
                                    Result.success(VerificarReaccionResponse(true, "dislike"))
                                },
                                onFailure = { Result.failure(it) }
                            )
                        }
                    }
                },
                onFailure = { Result.failure(it) }
            )
        } catch (e: Exception) {
            android.util.Log.e("PUBLICACION_REPO_DEBUG", "❌ Exception en toggleDislike: ${e.message}")
            Result.failure(e)
        }
    }

    //  HELPER PARA FORMATEAR FECHAS
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


    //  HELPER PARA MANEJAR IMÁGENES POR DEFECTO
    private fun getDefaultImageForRecipe(titulo: String): String {
        return when {
            titulo.contains("taco", ignoreCase = true) -> "sample_tacos"
            titulo.contains("ensalada", ignoreCase = true) -> "sample_salad"
            titulo.contains("pasta", ignoreCase = true) -> "sample_pasta"
            titulo.contains("sandwich", ignoreCase = true) -> "sample_sandwich"
            else -> "default_recipe"
        }
    }

    // HELPER METHODS
    private fun parseErrorMessage(response: Response<*>): String {
        return try {
            response.errorBody()?.string() ?: "Error desconocido"
        } catch (e: Exception) {
            "Error al procesar respuesta: ${response.code()}"
        }
    }
}