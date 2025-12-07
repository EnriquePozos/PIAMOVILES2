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

// Session manager para obtener usuario actual
import com.example.piamoviles2.utils.SessionManager

class PublicacionRepository(
    private val context: Context, // NUEVO: Context para acceder a Room
    private val apiService: ApiService = NetworkConfig.apiService
) {
    private val sessionManager = SessionManager(context)

    // COMPONENTES PARA MODO OFFLINE
    internal val database: AppDatabase? by lazy {
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
// MÉTODO OFFLINE CORREGIDO - Copiar archivos a directorio persistente
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
            val ctx = context ?: return Result.failure(Exception("Context no disponible"))

            // NUEVO: Copiar archivos a directorio persistente
            val archivosPersistentes = imagenes?.mapIndexed { index, originalFile ->
                try {
                    // Crear directorio persistente para multimedia offline
                    val offlineDir = File(ctx.filesDir, "multimedia_offline")
                    if (!offlineDir.exists()) {
                        offlineDir.mkdirs()
                        Log.d(TAG, "Directorio offline creado: ${offlineDir.absolutePath}")
                    }

                    // Generar nombre único para el archivo
                    val timestamp = System.currentTimeMillis()
                    val extension = originalFile.extension.ifEmpty { "jpg" }
                    val fileName = "multimedia_${timestamp}_$index.$extension"

                    // Archivo destino en directorio persistente
                    val destFile = File(offlineDir, fileName)

                    Log.d(TAG, "Copiando archivo de: ${originalFile.absolutePath}")
                    Log.d(TAG, "Copiando archivo a: ${destFile.absolutePath}")

                    // Copiar archivo
                    originalFile.copyTo(destFile, overwrite = true)

                    Log.d(TAG, "Archivo copiado exitosamente - Tamaño: ${destFile.length()} bytes")
                    Log.d(TAG, "Archivo existe después de copia: ${destFile.exists()}")

                    mapOf(
                        "tipo" to getMediaTypeForFile(originalFile),
                        "ruta" to destFile.absolutePath,  // ✅ RUTA PERSISTENTE
                        "nombre" to fileName
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error al copiar archivo $index: ${e.message}", e)
                    null
                }
            }?.filterNotNull()

            // Convertir a JSON
            val multimediaJson = archivosPersistentes?.let {
                JSONArray(it).toString()
            }

            Log.d(TAG, "JSON generado para multimedia: $multimediaJson")
            Log.d(TAG, "Archivos copiados a directorio persistente: ${archivosPersistentes?.size ?: 0}")

            // Crear entidad local
            val publicacionLocal = PublicacionLocal(
                titulo = titulo,
                descripcion = descripcion,
                estatus = estatus,
                idAutor = idAutor,
                multimediaJson = multimediaJson,
                token = token,
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

    // MÉTODO PARA SINCRONIZAR PUBLICACIONES PENDIENTES - CON LOGS DE DEBUG
    suspend fun sincronizarPublicacionesPendientes(token: String): Result<Int> {
        return try {
            Log.d(TAG, "=== sincronizarPublicacionesPendientes ===")

            val db = database ?: return Result.failure(Exception("Base de datos no disponible"))
            val publicacionesPendientes = db.publicacionLocalDao().obtenerPendientes()

            Log.d(TAG, "Publicaciones pendientes: ${publicacionesPendientes.size}")

            var sincronizadas = 0

            for (publicacion in publicacionesPendientes) {
                try {
                    Log.d(TAG, "=== DEBUGGING RECUPERACIÓN DE MULTIMEDIA ===")
                    Log.d(TAG, "Sincronizando publicación local ID: ${publicacion.id}")
                    Log.d(TAG, "Título: ${publicacion.titulo}")
                    Log.d(TAG, "MultimediaJson RAW: ${publicacion.multimediaJson}")
                    Log.d(TAG, "MultimediaJson es null? ${publicacion.multimediaJson == null}")
                    Log.d(TAG, "MultimediaJson está vacío? ${publicacion.multimediaJson?.isEmpty()}")
                    Log.d(TAG, "MultimediaJson length: ${publicacion.multimediaJson?.length ?: 0}")

                    // Intentar crear en API
                    val imagenes = publicacion.multimediaJson?.let { json ->
                        Log.d(TAG, "Procesando JSON: $json")

                        try {
                            val jsonArray = JSONArray(json)
                            Log.d(TAG, "JSONArray creado exitosamente, longitud: ${jsonArray.length()}")

                            val archivosList = (0 until jsonArray.length()).mapNotNull { i ->
                                try {
                                    val obj = jsonArray.getJSONObject(i)
                                    Log.d(TAG, "Objeto $i: $obj")

                                    val ruta = obj.getString("ruta")
                                    val tipo = obj.optString("tipo", "desconocido")
                                    val nombre = obj.optString("nombre", "sin_nombre")

                                    Log.d(TAG, "Archivo $i - Ruta: $ruta")
                                    Log.d(TAG, "Archivo $i - Tipo: $tipo")
                                    Log.d(TAG, "Archivo $i - Nombre: $nombre")

                                    val file = File(ruta)
                                    Log.d(TAG, "Archivo $i - Existe?: ${file.exists()}")
                                    Log.d(TAG, "Archivo $i - Tamaño: ${if (file.exists()) file.length() else "N/A"} bytes")
                                    Log.d(TAG, "Archivo $i - Legible?: ${file.canRead()}")

                                    if (file.exists()) {
                                        Log.d(TAG, "✅ Archivo $i válido: ${file.absolutePath}")
                                        file
                                    } else {
                                        Log.w(TAG, "❌ Archivo $i no existe: $ruta")
                                        null
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "❌ Error al procesar objeto $i: ${e.message}", e)
                                    null
                                }
                            }

                            Log.d(TAG, "Archivos válidos encontrados: ${archivosList.size}")
                            archivosList.forEachIndexed { index, archivo ->
                                Log.d(TAG, "Archivo válido $index: ${archivo.absolutePath}")
                            }

                            archivosList
                        } catch (jsonException: Exception) {
                            Log.e(TAG, "❌ Error al parsear JSON: ${jsonException.message}", jsonException)
                            emptyList<File>()
                        }
                    } ?: run {
                        Log.w(TAG, "⚠️ multimediaJson es null, retornando lista vacía")
                        emptyList<File>()
                    }

                    Log.d(TAG, "=== RESULTADO FINAL ===")
                    Log.d(TAG, "Total de imágenes recuperadas: ${imagenes.size}")
                    Log.d(TAG, "Llamando a crearPublicacion con ${imagenes.size} archivos")

                    if (imagenes.isEmpty()) {
                        Log.w(TAG, "⚠️ ADVERTENCIA: No hay imágenes para sincronizar")
                        Log.w(TAG, "⚠️ La publicación se sincronizará SIN multimedia")
                    } else {
                        Log.d(TAG, "✅ Archivos listos para enviar:")
                        imagenes.forEachIndexed { index, file ->
                            Log.d(TAG, "  [$index] ${file.name} (${file.length()} bytes)")
                        }
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

    suspend fun actualizarPublicacionOffline(
        localId: Long,
        titulo: String,
        descripcion: String,
        archivosMultimedia: List<File>,
        estatus: String? = "borrador"
    ): Result<com.example.piamoviles2.data.models.PublicacionDetalle> {
        return try {
            android.util.Log.d(TAG, "=== actualizarBorradorEnSQLite ===")
            android.util.Log.d(TAG, "Local ID: $localId")
            android.util.Log.d(TAG, "Nuevos archivos: ${archivosMultimedia.size}")

            val db = this.database
                ?: return Result.failure(Exception("Base de datos no disponible"))

            val currentUser = sessionManager.getCurrentUser()

            // 1. Obtener la publicación actual
            val publicacionActual = db.publicacionLocalDao().obtenerPublicacionesParaFeed(currentUser?.id)
                .find { it.id == localId }
                ?: return Result.failure(Exception("Borrador no encontrado"))

            // 2. Procesar nuevos archivos multimedia
            val nuevoMultimediaJson = if (archivosMultimedia.isNotEmpty()) {
                // Copiar archivos a directorio persistente
                val ctx = this.context
                    ?: return Result.failure(Exception("Context no disponible"))

                val offlineDir = File(ctx.filesDir, "multimedia_offline")
                if (!offlineDir.exists()) {
                    offlineDir.mkdirs()
                }

                val archivosPersistentes = archivosMultimedia.mapIndexed { index, originalFile ->
                    try {
                        val timestamp = System.currentTimeMillis()
                        val extension = originalFile.extension.ifEmpty { "jpg" }
                        val fileName = "multimedia_updated_${timestamp}_$index.$extension"
                        val destFile = File(offlineDir, fileName)

                        originalFile.copyTo(destFile, overwrite = true)

                        mapOf(
                            "tipo" to this.getMediaTypeForFile(originalFile),
                            "ruta" to destFile.absolutePath,
                            "nombre" to fileName
                        )
                    } catch (e: Exception) {
                        android.util.Log.e(TAG, "Error al copiar archivo $index", e)
                        null
                    }
                }.filterNotNull()

                if (archivosPersistentes.isNotEmpty()) {
                    org.json.JSONArray(archivosPersistentes).toString()
                } else {
                    publicacionActual.multimediaJson // Mantener multimedia actual
                }
            } else {
                publicacionActual.multimediaJson // No hay nuevos archivos, mantener actuales
            }

            // 3. Actualizar en SQLite
            val publicacionActualizada = publicacionActual.copy(
                titulo = titulo,
                descripcion = descripcion,
                estatus = estatus?: "borrador",
                multimediaJson = nuevoMultimediaJson,
                fechaCreacion = System.currentTimeMillis() // Actualizar timestamp
            )

            db.publicacionLocalDao().actualizar(publicacionActualizada)

            android.util.Log.d(TAG, "✅ Borrador actualizado en SQLite")

            // 4. Crear respuesta simulada
            val fechaActual = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())

            val publicacionDetalle = PublicacionDetalle(
                id = "offline_${publicacionActualizada.id}",
                titulo = titulo,
                descripcion = descripcion,
                fechaCreacion = fechaActual,
                fechaPublicacion = fechaActual,
                fechaModificacion = fechaActual,
                estatus = estatus?: "borrador",
                idAutor = publicacionActualizada.idAutor,
                autorAlias = "Usuario",
                autorFoto = null,
                totalComentarios = 0,
                totalReacciones = 0,
                multimedia = emptyList()
            )

            Result.success(publicacionDetalle)

        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Error al actualizar borrador SQLite", e)
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

    /**
     * Elimina una publicación local de SQLite (modo offline)
     * SOLO para publicaciones NO sincronizadas
     */
    suspend fun eliminarPublicacionOffline(localId: Long): Result<String> {
        return try {
            Log.d(TAG, "=== eliminarPublicacionOffline ===")
            Log.d(TAG, "Local ID: $localId")

            val db = database ?: return Result.failure(Exception("Base de datos no disponible"))

            // 1. Verificar que la publicación existe
            val publicacion = db.publicacionLocalDao().obtenerPorId(localId)
                ?: return Result.failure(Exception("Publicación no encontrada"))

            // 2. ✅ VALIDACIÓN: Solo eliminar si NO está sincronizada
            if (publicacion.sincronizado && !publicacion.apiId.isNullOrEmpty()) {
                Log.e(TAG, "❌ No se puede eliminar: publicación ya sincronizada")
                return Result.failure(
                    Exception("No puedes eliminar un borrador sincronizado sin conexión. Conéctate a internet.")
                )
            }

            // 3. Eliminar de la base de datos
            db.publicacionLocalDao().eliminar(publicacion)

            Log.d(TAG, "✅ Publicación local eliminada: ${publicacion.titulo}")
            Result.success("Borrador eliminado correctamente")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al eliminar publicación offline", e)
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

    /**
     * Obtener feed de publicaciones desde SQLite (modo offline)
     * Convierte PublicacionLocal a Post para mantener compatibilidad
     */
    suspend fun obtenerFeedOfflineConvertido(currentUserId: String): Result<List<Post>> {
        return try {
            Log.d(TAG, "=== obtenerFeedOfflineConvertido ===")

            val db = database ?: return Result.failure(Exception("Base de datos no disponible"))
            val currentUserId = sessionManager.getCurrentUser()?.id ?: ""

            val publicacionesLocales = db.publicacionLocalDao().obtenerPublicacionesParaFeed(currentUserId)

            Log.d(TAG, "Publicaciones locales encontradas: ${publicacionesLocales.size}")

            val posts = publicacionesLocales.mapNotNull { publicacionLocal ->
                if (publicacionLocal.estatus == "borrador") return@mapNotNull null
                // Convertir PublicacionLocal a Post
                Post(
                    id = publicacionLocal.id.toInt(), // Usar el ID local
                    apiId = publicacionLocal.apiId, // Puede ser null si no se ha sincronizado
                    title = publicacionLocal.titulo,
                    description = publicacionLocal.descripcion ?: "",
                    imageUrl = extraerPrimeraImagenLocal(publicacionLocal.multimediaJson) ?: "default_recipe",
                    author = "Usuario", // En offline no tenemos info del autor completa
                    createdAt = formatearFechaLocal(publicacionLocal.fechaCreacion),
                    isOwner = publicacionLocal.idAutor == currentUserId,
                    isFavorite = false, // En offline no manejamos favoritos aún
                    isDraft = publicacionLocal.estatus == "borrador",
                    likesCount = 0, // En offline no tenemos conteos
                    commentsCount = 0 // En offline no tenemos conteos
                )
            }

            Log.d(TAG, "Convertidas ${posts.size} publicaciones offline a Post")
            Result.success(posts)

        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener feed offline: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Método principal para obtener feed según conectividad
     * ONLINE = API, OFFLINE = SQLite
     */
    suspend fun obtenerFeedSegunConectividad(token: String, currentUserId: String): Result<List<Post>> {
        val hasInternet = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            networkMonitor?.isOnline() ?: true
        } else {
            true // Para versiones anteriores, asumir conexión
        }

        Log.d(TAG, "=== obtenerFeedSegunConectividad ===")
        Log.d(TAG, "Estado de conectividad: ${if (hasInternet) "ONLINE" else "OFFLINE"}")

        return if (hasInternet) {
            // Modo online - usar API (método existente)
            obtenerFeedConvertido(token, currentUserId)
        } else {
            // Modo offline - usar SQLite
            obtenerFeedOfflineConvertido(currentUserId)
        }
    }

    /**
     * Extraer la primera imagen del JSON de multimedia para preview
     */
    private fun extraerPrimeraImagenLocal(multimediaJson: String?): String? {
        return try {
            if (multimediaJson.isNullOrEmpty()) {
                Log.d(TAG, "No hay multimedia JSON")
                return null
            }

            val jsonArray = JSONArray(multimediaJson)
            if (jsonArray.length() > 0) {
                val primerItem = jsonArray.getJSONObject(0)
                val ruta = primerItem.getString("ruta")
                val tipo = primerItem.optString("tipo", "")

                Log.d(TAG, "Primera imagen encontrada: $ruta")

                // Verificar si es imagen y si el archivo existe
                if (tipo.startsWith("image/") && File(ruta).exists()) {
                    return "file://$ruta" // Prefijo para cargar desde archivo local
                }
            }
            null
        } catch (e: Exception) {
            Log.w(TAG, "Error al extraer primera imagen: ${e.message}")
            null
        }
    }

    /**
     * Formatear timestamp local a fecha legible
     */
    private fun formatearFechaLocal(timestamp: Long): String {
        return try {
            val fecha = java.util.Date(timestamp)
            val formato = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
            formato.format(fecha)
        } catch (e: Exception) {
            Log.w(TAG, "Error al formatear fecha local: $timestamp")
            "Fecha no disponible"
        }
    }
    // ============================================
    // NUEVO: MÉTODOS PARA PERFIL OFFLINE
    // ============================================

    /**
     * Obtener publicaciones del usuario desde SQLite (modo offline)
     * Como en SQLite solo se guardan publicaciones del usuario activo,
     * solo filtramos por estatus (borrador vs publicada)
     */
    suspend fun obtenerPublicacionesUsuarioOfflineConvertidas(
        incluirBorradores: Boolean = false
    ): Result<List<Post>> {
        return try {
            Log.d(TAG, "=== obtenerPublicacionesUsuarioOfflineConvertidas ===")
            Log.d(TAG, "Incluir borradores: $incluirBorradores")

            val db = database ?: return Result.failure(Exception("Base de datos no disponible"))

            val currentUserId = sessionManager.getCurrentUser()?.id ?: ""

            // Obtener todas las publicaciones locales (ya son del usuario activo)
            val publicacionesLocales = db.publicacionLocalDao().obtenerPublicacionesParaFeed(currentUserId)

            Log.d(TAG, "Total publicaciones offline encontradas: ${publicacionesLocales.size}")

            // Filtrar por estatus según lo solicitado
            val publicacionesFiltradas = if (incluirBorradores) {
                // Para borradores: solo las que tengan estatus "borrador"
                publicacionesLocales.filter { it.estatus == "borrador" }
            } else {
                // Para publicadas: solo las que tengan estatus "publicada"
                publicacionesLocales.filter { it.estatus == "publicada" }
            }

            Log.d(TAG, "Publicaciones después de filtrar: ${publicacionesFiltradas.size}")

            val posts = publicacionesFiltradas.mapIndexed { index, publicacionLocal ->
                Post(
                    id = publicacionLocal.id.toInt(), // Usar ID local
                    apiId = publicacionLocal.apiId, // null si no se ha sincronizado
                    title = publicacionLocal.titulo,
                    description = publicacionLocal.descripcion ?: "",
                    imageUrl = extraerPrimeraImagenLocal(publicacionLocal.multimediaJson) ?: "default_recipe",
                    author = "Usuario", // En offline no tenemos alias completo
                    createdAt = formatearFechaLocal(publicacionLocal.fechaCreacion),
                    isOwner = true, // Todas las publicaciones offline son del usuario activo
                    isFavorite = false, // En offline no manejamos favoritos
                    isDraft = publicacionLocal.estatus == "borrador",
                    likesCount = 0, // En offline no tenemos conteos
                    commentsCount = 0, // En offline no tenemos conteos
                    isSynced = publicacionLocal.sincronizado
                )
            }

            Log.d(TAG, "Convertidas ${posts.size} publicaciones offline del usuario")
            Result.success(posts)

        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener publicaciones usuario offline: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Método principal para obtener publicaciones de usuario según conectividad
     * ONLINE = API, OFFLINE = SQLite filtrado por estatus
     */
    suspend fun obtenerPublicacionesUsuarioSegunConectividad(
        idAutor: String,
        incluirBorradores: Boolean = false,
        token: String
    ): Result<List<Post>> {
        val hasInternet = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            networkMonitor?.isOnline() ?: true
        } else {
            true // Para versiones anteriores, asumir conexión
        }

        Log.d(TAG, "=== obtenerPublicacionesUsuarioSegunConectividad ===")
        Log.d(TAG, "Estado de conectividad: ${if (hasInternet) "ONLINE" else "OFFLINE"}")
        Log.d(TAG, "Incluir borradores: $incluirBorradores")

        return if (hasInternet) {
            // Modo online - usar API (método existente)
            obtenerPublicacionesUsuarioConvertidas(idAutor, incluirBorradores, token)
        } else {
            // Modo offline - usar SQLite filtrado
            obtenerPublicacionesUsuarioOfflineConvertidas(incluirBorradores)
        }
    }
}