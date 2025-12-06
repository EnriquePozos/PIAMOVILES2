package com.example.piamoviles2.data.repositories

import android.content.Context
import android.util.Log
import android.os.Build
import com.example.piamoviles2.data.api.ApiService
import com.example.piamoviles2.data.models.*
import com.example.piamoviles2.data.network.NetworkConfig
import com.example.piamoviles2.Post
import retrofit2.Response
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File

// NUEVOS IMPORTS PARA MODO OFFLINE
import com.example.piamoviles2.data.local.AppDatabase
import com.example.piamoviles2.data.local.entities.PublicacionLocal
import com.example.piamoviles2.data.local.dao.*
import com.example.piamoviles2.utils.NetworkMonitor
import org.json.JSONArray

class PublicacionRepository(
    private val context: Context? = null, // NUEVO: Context para acceder a Room
    private val apiService: ApiService = NetworkConfig.apiService
) {

    // COMPONENTES PARA MODO OFFLINE
    private val database: AppDatabase? by lazy {
        context?.let { AppDatabase.getDatabase(it) }
    }

    private val networkMonitor: NetworkMonitor? by lazy {
        context?.let { NetworkMonitor(it) }
    }

    companion object {
        private const val TAG = "PUBLICACION_REPO_DEBUG"
    }

    // CREAR PUBLICACIÓN - FORMDATA CON ARCHIVOS - AHORA CON SOPORTE OFFLINE
    suspend fun crearPublicacion(
        titulo: String,
        descripcion: String?,
        estatus: String, // "borrador" o "publicada"
        idAutor: String,
        imagenes: List<File>?,
        token: String
    ): Result<PublicacionDetalle> {
        Log.d(TAG, "=== crearPublicacion ===")
        Log.d(TAG, "Título: $titulo")
        Log.d(TAG, "Estatus: $estatus")
        Log.d(TAG, "ID Autor: $idAutor")
        Log.d(TAG, "Archivos multimedia: ${imagenes?.size ?: 0}")

        // VERIFICAR CONECTIVIDAD
        val hasInternet = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            networkMonitor?.isOnline() ?: true
        } else {
            true // Para versiones anteriores, asumir conexión
        }

        Log.d(TAG, "Conectividad a internet: $hasInternet")

        return if (hasInternet) {
            // LÓGICA ORIGINAL - Mantener nombre del método original
            try {
                val textMediaType = "text/plain".toMediaTypeOrNull()

                // Crear RequestBody para campos de texto
                val tituloBody = RequestBody.create(textMediaType, titulo)
                val estatusBody = RequestBody.create(textMediaType, estatus)
                val idAutorBody = RequestBody.create(textMediaType, idAutor)
                val descripcionBody = descripcion?.let { RequestBody.create(textMediaType, it) }

                // Crear MultipartBody.Part para IMÁGENES Y VIDEOS
                val archivosParts = imagenes?.mapIndexed { index, file ->
                    // Detectar el tipo de archivo correctamente
                    val mediaType = getMediaTypeForFile(file)
                    val fileName = getFileNameForUpload(file, index)

                    Log.d(TAG, "Archivo $index: ${file.name}")
                    Log.d(TAG, "Media Type: $mediaType")
                    Log.d(TAG, "Tamaño: ${file.length()} bytes")

                    val requestFile = RequestBody.create(mediaType.toMediaTypeOrNull(), file)
                    MultipartBody.Part.createFormData("archivos", fileName, requestFile)
                }

                Log.d(TAG, "RequestBodies creados, llamando a API...")

                val response = apiService.crearPublicacion(
                    titulo = tituloBody,
                    descripcion = descripcionBody,
                    estatus = estatusBody,
                    idAutor = idAutorBody,
                    archivos = archivosParts,
                    authorization = "Bearer $token"
                )

                Log.d(TAG, "Response code: ${response.code()}")
                Log.d(TAG, "Response successful: ${response.isSuccessful}")

                if (response.isSuccessful && response.body() != null) {
                    Log.d(TAG, "Publicación creada exitosamente")
                    Result.success(response.body()!!)
                } else {
                    val errorMsg = parseErrorMessage(response)
                    Log.e(TAG, "Error en respuesta: $errorMsg")
                    Result.failure(Exception(errorMsg))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception: ${e.message}", e)
                Result.failure(e)
            }
        } else {
            // MODO OFFLINE - Guardar en SQLite
            crearPublicacionOffline(titulo, descripcion, estatus, idAutor, imagenes, token)
        }
    }

    // MÉTODO OFFLINE - Guardar en SQLite cuando no hay conexión
    private suspend fun crearPublicacionOffline(
        titulo: String,
        descripcion: String?,
        estatus: String,
        idAutor: String,
        imagenes: List<File>?,
        token: String
    ): Result<PublicacionDetalle> {
        return try {
            Log.d(TAG, "MODO OFFLINE - Guardando en SQLite")

            val db = database ?: return Result.failure(Exception("Base de datos no disponible"))

            // Convertir archivos a JSON para almacenar rutas
            val multimediaJson = imagenes?.map { file ->
                mapOf(
                    "tipo" to getMediaTypeForFile(file),
                    "ruta" to file.absolutePath,
                    "nombre" to file.name
                )
            }?.let { JSONArray(it).toString() }

            // Crear entidad local
            val publicacionLocal = PublicacionLocal(
                titulo = titulo,
                descripcion = descripcion,
                estatus = estatus,
                idAutor = idAutor,
                multimediaJson = multimediaJson,
                token = token, // Guardar token para uso posterior en sincronización
                sincronizado = false,
                fechaCreacion = System.currentTimeMillis()
            )

            // Guardar en SQLite
            val id = db.publicacionLocalDao().insertar(publicacionLocal)

            Log.d(TAG, "Publicación guardada offline con ID: $id")

            // Crear respuesta simulada para mantener compatibilidad
            val fechaActual = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())

            val publicacionDetalle = PublicacionDetalle(
                id = "offline_$id",
                titulo = titulo,
                descripcion = descripcion ?: "",
                fechaCreacion = fechaActual,
                fechaPublicacion = fechaActual,
                fechaModificacion = null,
                estatus = estatus,
                idAutor = idAutor,
                autorAlias = "Usuario",
                autorFoto = null,
                totalComentarios = 0,
                totalReacciones = 0,
                multimedia = emptyList()
            )

            Result.success(publicacionDetalle)

        } catch (e: Exception) {
            Log.e(TAG, "Error al guardar offline: ${e.message}", e)
            Result.failure(e)
        }
    }

    // MÉTODO PARA SINCRONIZAR PUBLICACIONES PENDIENTES
    suspend fun sincronizarPublicacionesPendientes(token: String): Result<Int> {
        return try {
            Log.d(TAG, "=== sincronizarPublicacionesPendientes ===")

            val db = database ?: return Result.failure(Exception("Base de datos no disponible"))
            val publicacionesPendientes = db.publicacionLocalDao().obtenerPendientes()

            Log.d(TAG, "Publicaciones pendientes: ${publicacionesPendientes.size}")

            var sincronizadas = 0

            for (publicacion in publicacionesPendientes) {
                try {
                    // Intentar crear en API
                    val imagenes = publicacion.multimediaJson?.let { json ->
                        val jsonArray = JSONArray(json)
                        (0 until jsonArray.length()).map { i ->
                            val obj = jsonArray.getJSONObject(i)
                            File(obj.getString("ruta"))
                        }.filter { it.exists() }
                    }

                    val result = crearPublicacion(
                        titulo = publicacion.titulo,
                        descripcion = publicacion.descripcion,
                        estatus = publicacion.estatus,
                        idAutor = publicacion.idAutor,
                        imagenes = imagenes,
                        token = token
                    )

                    result.fold(
                        onSuccess = { apiResponse ->
                            // Marcar como sincronizada
                            db.publicacionLocalDao().marcarComoSincronizado(publicacion.id, apiResponse.id)
                            sincronizadas++
                            Log.d(TAG, "Publicación ${publicacion.id} sincronizada exitosamente")
                        },
                        onFailure = { error ->
                            Log.e(TAG, "Error al sincronizar publicación ${publicacion.id}: ${error.message}")
                        }
                    )

                } catch (e: Exception) {
                    Log.e(TAG, "Exception al sincronizar publicación ${publicacion.id}: ${e.message}")
                }
            }

            Log.d(TAG, "Sincronización completa: $sincronizadas/${publicacionesPendientes.size}")
            Result.success(sincronizadas)

        } catch (e: Exception) {
            Log.e(TAG, "Error en sincronización: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun getMediaTypeForFile(file: File): String {
        val fileName = file.name.lowercase()

        return when {
            // Videos
            fileName.endsWith(".mp4") -> "video/mp4"
            fileName.endsWith(".mov") -> "video/quicktime"
            fileName.endsWith(".avi") -> "video/x-msvideo"
            fileName.endsWith(".mkv") -> "video/x-matroska"
            fileName.endsWith(".3gp") -> "video/3gpp"
            fileName.endsWith(".webm") -> "video/webm"

            // Imágenes
            fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") -> "image/jpeg"
            fileName.endsWith(".png") -> "image/png"
            fileName.endsWith(".gif") -> "image/gif"
            fileName.endsWith(".webp") -> "image/webp"

            // Por defecto (fallback)
            else -> {
                Log.w(TAG, "Tipo desconocido para: $fileName, usando application/octet-stream")
                "application/octet-stream"
            }
        }
    }

    private fun getFileNameForUpload(file: File, index: Int): String {
        val extension = file.name.substringAfterLast(".", "jpg")
        val prefix = if (extension in listOf("mp4", "mov", "avi", "mkv", "3gp", "webm")) {
            "video"
        } else {
            "imagen"
        }
        return "${prefix}_${index}.${extension}"
    }

    // OBTENER FEED DE PUBLICACIONES (ORIGINAL)
    suspend fun obtenerFeedPublicaciones(token: String): Result<List<PublicacionListFeed>> {
        return try {
            Log.d(TAG, "=== obtenerFeedPublicaciones ===")

            val authHeader = "Bearer $token"
            val response = apiService.obtenerFeedPublicaciones(authHeader)

            Log.d(TAG, "Response code: ${response.code()}")

            if (response.isSuccessful) {
                response.body()?.let { feed ->
                    Log.d(TAG, " Feed obtenido: ${feed.size} publicaciones")
                    Result.success(feed)
                } ?: Result.failure(Exception("Respuesta vacía del servidor"))
            } else {
                val errorMsg = parseErrorMessage(response)
                Log.e(TAG, "  Error al obtener feed: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "  Exception: ${e.message}")
            Result.failure(e)
        }
    }

    // OBTENER FEED CON CONVERSIÓN A POST
    suspend fun obtenerFeedConvertido(token: String, currentUserId: String): Result<List<Post>> {
        return try {
            Log.d(TAG, "=== obtenerFeedConvertido ===")
            Log.d(TAG, "Current User ID: $currentUserId")

            val authHeader = "Bearer $token"
            val response = apiService.obtenerFeedPublicaciones(authHeader)

            Log.d(TAG, "Response code: ${response.code()}")

            if (response.isSuccessful) {
                response.body()?.let { feedList ->
                    Log.d(TAG, "  Feed obtenido: ${feedList.size} publicaciones")

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

                    Log.d(TAG, "  Convertido a ${posts.size} Posts con API IDs")
                    Result.success(posts)
                } ?: Result.failure(Exception("Respuesta vacía del servidor"))
            } else {
                val errorMsg = parseErrorMessage(response)
                Log.e(TAG, "  Error al obtener feed: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "  Exception: ${e.message}")
            Result.failure(e)
        }
    }
    // OBTENER PUBLICACIÓN POR ID
    suspend fun obtenerPublicacionPorId(idPublicacion: String, token: String): Result<PublicacionDetalle> {
        return try {
            Log.d(TAG, "=== obtenerPublicacionPorId ===")
            Log.d(TAG, "ID Publicación: $idPublicacion")

            val authHeader = "Bearer $token"
            val response = apiService.obtenerPublicacionPorId(idPublicacion, authHeader)

            if (response.isSuccessful) {
                response.body()?.let { publicacion ->
                    Log.d(TAG, "  Publicación obtenida: ${publicacion.titulo}")
                    Result.success(publicacion)
                } ?: Result.failure(Exception("Respuesta vacía del servidor"))
            } else {
                val errorMsg = parseErrorMessage(response)
                Log.e(TAG, "  Error: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "  Exception: ${e.message}")
            Result.failure(e)
        }
    }

    //   OBTENER PUBLICACION DETALLE COMPLETA (PARA PANTALLA DE DETALLES) - CORREGIDO
    suspend fun obtenerPublicacionDetalleCompleta(
        idPublicacion: String,
        token: String
    ): Result<PublicacionDetalle> {
        return try {
            Log.d(TAG, "=== obtenerPublicacionDetalleCompleta ===")
            Log.d(TAG, "ID Publicación: $idPublicacion")

            val authHeader = "Bearer $token"
            val response = apiService.obtenerPublicacionPorId(idPublicacion, authHeader)

            if (response.isSuccessful) {
                response.body()?.let { publicacion ->
                    Log.d(TAG, "  Publicación obtenida: ${publicacion.titulo}")
                    Log.d(TAG, "Multimedia: ${publicacion.multimedia.size} items")
                    Log.d(TAG, "Estatus: ${publicacion.estatus}")
                    Result.success(publicacion)
                } ?: Result.failure(Exception("Respuesta vacía del servidor"))
            } else {
                val errorMsg = parseErrorMessage(response)
                Log.e(TAG, "  Error: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "  Exception: ${e.message}")
            Result.failure(e)
        }
    }

    // ACTUALIZAR PUBLICACIÓN
    suspend fun actualizarPublicacion(
        idPublicacion: String,
        titulo: String,
        descripcion: String?,
        estatus: String,
        imagenes: List<File>?,
        token: String
    ): Result<PublicacionDetalle> {
        Log.d(TAG, "=== actualizarPublicacion ===")
        Log.d(TAG, "ID: $idPublicacion")
        Log.d(TAG, "Archivos multimedia: ${imagenes?.size ?: 0}")

        return try {
            val textMediaType = "text/plain".toMediaTypeOrNull()

            val tituloBody = RequestBody.create(textMediaType, titulo)
            val estatusBody = RequestBody.create(textMediaType, estatus)
            val descripcionBody = descripcion?.let { RequestBody.create(textMediaType, it) }

            // Usar las mismas funciones helper para detectar tipos
            val archivosParts = imagenes?.mapIndexed { index, file ->
                val mediaType = getMediaTypeForFile(file)
                val fileName = getFileNameForUpload(file, index)

                Log.d(TAG, "Archivo $index: $fileName, tipo: $mediaType")

                val requestFile = RequestBody.create(mediaType.toMediaTypeOrNull(), file)
                MultipartBody.Part.createFormData("archivos", fileName, requestFile)
            }

            Log.d(TAG, "Llamando a API para actualizar...")

            val response = apiService.actualizarPublicacion(
                idPublicacion = idPublicacion,
                titulo = tituloBody,
                descripcion = descripcionBody,
                estatus = estatusBody,
                archivos = archivosParts,
                authorization = "Bearer $token"
            )

            Log.d(TAG, "Response code: ${response.code()}")

            if (response.isSuccessful && response.body() != null) {
                Log.d(TAG, "Publicación actualizada")
                Result.success(response.body()!!)
            } else {
                val errorMsg = parseErrorMessage(response)
                Log.e(TAG, "Error: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ELIMINAR PUBLICACIÓN
    suspend fun eliminarPublicacion(idPublicacion: String, token: String): Result<String> {
        return try {
            Log.d(TAG, "=== eliminarPublicacion ===")
            Log.d(TAG, "ID Publicación: $idPublicacion")

            val authHeader = "Bearer $token"
            val response = apiService.eliminarPublicacion(idPublicacion, authHeader)

            if (response.isSuccessful) {
                response.body()?.let { resultado ->
                    Log.d(TAG, "  Publicación eliminada")
                    Result.success(resultado.message)
                } ?: Result.success("Publicación eliminada exitosamente")
            } else {
                val errorMsg = parseErrorMessage(response)
                Log.e(TAG, "  Error: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "  Exception: ${e.message}")
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
            Log.d(TAG, "=== obtenerPublicacionesUsuario ===")
            Log.d(TAG, "ID Autor: $idAutor")
            Log.d(TAG, "Incluir borradores: $incluirBorradores")

            val authHeader = "Bearer $token"

            val response = if (incluirBorradores) {
                Log.d(TAG, "Llamando a obtenerBorradoresUsuario")
                apiService.obtenerBorradoresUsuario(
                    idUsuario = idAutor,
                    authorization = authHeader
                )
            } else {
                Log.d(TAG, "Llamando a obtenerPublicacionesActivasUsuario")
                apiService.obtenerPublicacionesActivasUsuario(
                    idUsuario = idAutor,
                    authorization = authHeader
                )
            }

            Log.d(TAG, "Response code: ${response.code()}")
            Log.d(TAG, "Response successful: ${response.isSuccessful}")

            if (response.isSuccessful) {
                response.body()?.let { publicaciones ->
                    Log.d(TAG, "  Publicaciones obtenidas: ${publicaciones.size}")
                    Result.success(publicaciones)
                } ?: Result.failure(Exception("Respuesta vacía del servidor"))
            } else {
                val errorMsg = parseErrorMessage(response)
                Log.e(TAG, "  Error: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "  Exception: ${e.message}")
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
            Log.d(TAG, "=== obtenerPublicacionesUsuarioConvertidas ===")

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

                    Log.d(TAG, "  Convertidas ${posts.size} publicaciones de usuario con API IDs")
                    Result.success(posts)
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, " Exception: ${e.message}")
            Result.failure(e)
        }
    }

    // OBTENER FAVORITOS DEL USUARIO CONVERTIDOS A POST
    suspend fun obtenerFavoritosUsuarioConvertidos(
        idUsuario: String,
        token: String
    ): Result<List<Post>> {
        return try {
            Log.d(TAG, "=== obtenerFavoritosUsuarioConvertidos ===")
            Log.d(TAG, "Usuario ID: $idUsuario")

            val authHeader = "Bearer $token"
            val response = apiService.obtenerFavoritasUsuario(idUsuario, authHeader)

            Log.d(TAG, "Response code: ${response.code()}")

            if (response.isSuccessful) {
                response.body()?.let { favoritasList ->
                    Log.d(TAG, "  Favoritos obtenidos: ${favoritasList.size} publicaciones")

                    // Convertir PublicacionListFeed a Post
                    val posts = favoritasList.mapIndexed { index, publicacion ->
                        Post(
                            id = (publicacion.id.hashCode().takeIf { it > 0 } ?: (3000 + index)), // ID local único para favoritos
                            apiId = publicacion.id,
                            title = publicacion.titulo,
                            description = publicacion.descripcion ?: "",
                            imageUrl = publicacion.imagenPreview ?: "default_recipe",
                            author = "@${publicacion.autorAlias ?: "Anónimo"}",
                            createdAt = formatearFecha(publicacion.fechaPublicacion),
                            isOwner = false, // Los favoritos generalmente no son del mismo usuario
                            isFavorite = true, // Por definición, todos son favoritos
                            isDraft = false, // Los favoritos nunca son borradores
                            likesCount = publicacion.totalReacciones,
                            commentsCount = publicacion.totalComentarios
                        )
                    }

                    Log.d(TAG, "  Posts convertidos: ${posts.size}")
                    Result.success(posts)

                } ?: Result.failure(Exception("Respuesta vacía del servidor"))
            } else {
                val errorMsg = parseErrorMessage(response)
                Log.e(TAG, "  Error: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "  Exception: ${e.message}")
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
            Log.d(TAG, "=== agregarReaccion ===")
            Log.d(TAG, "ID Publicación: $idPublicacion")
            Log.d(TAG, "ID Usuario: $idUsuario")
            Log.d(TAG, "Tipo Reacción: ${tipoReaccion.value}")

            val response = apiService.agregarReaccion(
                idPublicacion = idPublicacion,
                idUsuario = idUsuario,
                tipoReaccion = tipoReaccion.value,
                authorization = "Bearer $token"
            )

            Log.d(TAG, "Response code: ${response.code()}")

            if (response.isSuccessful && response.body() != null) {
                Log.d(TAG, "Reacción agregada exitosamente")
                Result.success(response.body()!!)
            } else {
                val errorMsg = parseErrorMessage(response)
                Log.e(TAG, "Error al agregar reacción: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception: ${e.message}")
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
            Log.d(TAG, "=== eliminarReaccion ===")
            Log.d(TAG, "ID Publicación: $idPublicacion")
            Log.d(TAG, "ID Usuario: $idUsuario")

            val response = apiService.eliminarReaccion(
                idPublicacion = idPublicacion,
                idUsuario = idUsuario,
                authorization = "Bearer $token"
            )

            Log.d(TAG, "Response code: ${response.code()}")

            if (response.isSuccessful) {
                Log.d(TAG, "Reacción eliminada exitosamente")
                Result.success(true)
            } else {
                val errorMsg = parseErrorMessage(response)
                Log.e(TAG, "Error al eliminar reacción: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception: ${e.message}")
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
            Log.d(TAG, "=== obtenerConteoReacciones ===")
            Log.d(TAG, "ID Publicación: $idPublicacion")

            val response = apiService.obtenerConteoReacciones(
                idPublicacion = idPublicacion,
                authorization = "Bearer $token"
            )

            Log.d(TAG, "Response code: ${response.code()}")

            if (response.isSuccessful && response.body() != null) {
                val conteo = response.body()!!
                Log.d(TAG, "Conteo obtenido - Likes: ${conteo.likes}, Dislikes: ${conteo.dislikes}")
                Result.success(conteo)
            } else {
                val errorMsg = parseErrorMessage(response)
                Log.e(TAG, "Error al obtener conteo: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception: ${e.message}")
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
            Log.d(TAG, "=== verificarReaccionUsuario ===")
            Log.d(TAG, "ID Publicación: $idPublicacion")
            Log.d(TAG, "ID Usuario: $idUsuario")

            val response = apiService.verificarReaccionUsuario(
                idPublicacion = idPublicacion,
                idUsuario = idUsuario,
                authorization = "Bearer $token"
            )

            Log.d(TAG, "Response code: ${response.code()}")

            if (response.isSuccessful && response.body() != null) {
                val verificacion = response.body()!!
                Log.d(TAG, "Verificación obtenida - Tiene reacción: ${verificacion.tieneReaccion}, Tipo: ${verificacion.tipoReaccion}")
                Result.success(verificacion)
            } else {
                val errorMsg = parseErrorMessage(response)
                Log.e(TAG, "Error al verificar reacción: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception: ${e.message}")
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
            Log.d(TAG, "=== toggleLike ===")

            // Primero verificar si ya tiene reacción
            val verificacionResult = verificarReaccionUsuario(idPublicacion, idUsuario, token)

            verificacionResult.fold(
                onSuccess = { verificacion ->
                    when {
                        verificacion.esLike() -> {
                            // Ya tiene like, eliminarlo
                            Log.d(TAG, "Usuario ya tiene LIKE, eliminando...")
                            eliminarReaccion(idPublicacion, idUsuario, token).fold(
                                onSuccess = {
                                    Result.success(VerificarReaccionResponse(false, null))
                                },
                                onFailure = { Result.failure(it) }
                            )
                        }
                        verificacion.esDislike() -> {
                            // Tiene dislike, cambiarlo a like
                            Log.d(TAG, "Usuario tiene DISLIKE, cambiando a LIKE...")
                            agregarReaccion(idPublicacion, idUsuario, TipoReaccion.LIKE, token).fold(
                                onSuccess = {
                                    Result.success(VerificarReaccionResponse(true, "like"))
                                },
                                onFailure = { Result.failure(it) }
                            )
                        }
                        else -> {
                            // No tiene reacción, agregar like
                            Log.d(TAG, "Usuario no tiene reacción, agregando LIKE...")
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
            Log.e(TAG, "Exception en toggleLike: ${e.message}")
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
            Log.d(TAG, "=== toggleDislike ===")

            // Primero verificar si ya tiene reacción
            val verificacionResult = verificarReaccionUsuario(idPublicacion, idUsuario, token)

            verificacionResult.fold(
                onSuccess = { verificacion ->
                    when {
                        verificacion.esDislike() -> {
                            // Ya tiene dislike, eliminarlo
                            Log.d(TAG, "Usuario ya tiene DISLIKE, eliminando...")
                            eliminarReaccion(idPublicacion, idUsuario, token).fold(
                                onSuccess = {
                                    Result.success(VerificarReaccionResponse(false, null))
                                },
                                onFailure = { Result.failure(it) }
                            )
                        }
                        verificacion.esLike() -> {
                            // Tiene like, cambiarlo a dislike
                            Log.d(TAG, "Usuario tiene LIKE, cambiando a DISLIKE...")
                            agregarReaccion(idPublicacion, idUsuario, TipoReaccion.DISLIKE, token).fold(
                                onSuccess = {
                                    Result.success(VerificarReaccionResponse(true, "dislike"))
                                },
                                onFailure = { Result.failure(it) }
                            )
                        }
                        else -> {
                            // No tiene reacción, agregar dislike
                            Log.d(TAG, "Usuario no tiene reacción, agregando DISLIKE...")
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
            Log.e(TAG, "Exception en toggleDislike: ${e.message}")
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
            Log.w(TAG, "Error al formatear fecha: $fechaISO")
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