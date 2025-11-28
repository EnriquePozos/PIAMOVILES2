package com.example.piamoviles2.data.repositories

import com.example.piamoviles2.data.api.ApiService
import com.example.piamoviles2.data.models.*
import com.example.piamoviles2.data.network.NetworkConfig
import com.example.piamoviles2.Comment
import retrofit2.Response

class ComentarioRepository(
    private val apiService: ApiService = NetworkConfig.apiService
) {

    companion object {
        private const val TAG = "COMENTARIO_REPO_DEBUG"
    }

    // ============================================
    // CREAR COMENTARIOS Y RESPUESTAS
    // ============================================

    /**
     * Crear comentario en una publicación
     * @param idPublicacion ID de la publicación
     * @param comentario Texto del comentario
     * @param idUsuario ID del usuario que comenta
     * @param token Token de autorización
     * @return Result<ComentarioResponse>
     */
    suspend fun crearComentario(
        idPublicacion: String,
        comentario: String,
        idUsuario: String,
        token: String
    ): Result<ComentarioResponse> {
        return try {
            android.util.Log.d(TAG, "=== crearComentario ===")
            android.util.Log.d(TAG, "ID Publicación: $idPublicacion")
            android.util.Log.d(TAG, "ID Usuario: $idUsuario")
            android.util.Log.d(TAG, "Comentario: ${comentario.take(50)}...")

            val response = apiService.crearComentarioEnPublicacion(
                idPublicacion = idPublicacion,
                comentario = comentario,
                idUsuario = idUsuario,
                authorization = "Bearer $token"
            )

            android.util.Log.d(TAG, "Response code: ${response.code()}")

            if (response.isSuccessful && response.body() != null) {
                android.util.Log.d(TAG, "✅ Comentario creado exitosamente")
                Result.success(response.body()!!)
            } else {
                val errorMsg = parseErrorMessage(response)
                android.util.Log.e(TAG, "❌ Error al crear comentario: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Exception: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Crear respuesta a un comentario
     * @param idComentarioPadre ID del comentario padre
     * @param comentario Texto de la respuesta
     * @param idUsuario ID del usuario que responde
     * @param token Token de autorización
     * @return Result<ComentarioResponse>
     */
    suspend fun crearRespuesta(
        idComentarioPadre: String,
        comentario: String,
        idUsuario: String,
        token: String
    ): Result<ComentarioResponse> {
        return try {
            android.util.Log.d(TAG, "=== crearRespuesta ===")
            android.util.Log.d(TAG, "ID Comentario Padre: $idComentarioPadre")
            android.util.Log.d(TAG, "ID Usuario: $idUsuario")
            android.util.Log.d(TAG, "Respuesta: ${comentario.take(50)}...")

            val response = apiService.crearRespuestaAComentario(
                idComentarioPadre = idComentarioPadre,
                comentario = comentario,
                idUsuario = idUsuario,
                authorization = "Bearer $token"
            )

            android.util.Log.d(TAG, "Response code: ${response.code()}")

            if (response.isSuccessful && response.body() != null) {
                android.util.Log.d(TAG, "✅ Respuesta creada exitosamente")
                Result.success(response.body()!!)
            } else {
                val errorMsg = parseErrorMessage(response)
                android.util.Log.e(TAG, "❌ Error al crear respuesta: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Exception: ${e.message}")
            Result.failure(e)
        }
    }

    // ============================================
    // OBTENER COMENTARIOS Y RESPUESTAS
    // ============================================

    /**
     * Obtener comentarios de una publicación
     * @param idPublicacion ID de la publicación
     * @param token Token de autorización
     * @param skip Número de registros a saltar (para paginación)
     * @param limit Límite de registros
     * @return Result<List<ComentarioResponse>>
     */
    suspend fun obtenerComentariosPublicacion(
        idPublicacion: String,
        token: String,
        skip: Int = 0,
        limit: Int = 20
    ): Result<List<ComentarioResponse>> {
        return try {
            android.util.Log.d(TAG, "=== obtenerComentariosPublicacion ===")
            android.util.Log.d(TAG, "ID Publicación: $idPublicacion")
            android.util.Log.d(TAG, "Skip: $skip, Limit: $limit")

            val response = apiService.obtenerComentariosDePublicacion(
                idPublicacion = idPublicacion,
                skip = skip,
                limit = limit,
                authorization = "Bearer $token"
            )

            android.util.Log.d(TAG, "Response code: ${response.code()}")

            if (response.isSuccessful && response.body() != null) {
                val comentarios = response.body()!!
                android.util.Log.d(TAG, "✅ Comentarios obtenidos: ${comentarios.size}")
                Result.success(comentarios)
            } else {
                val errorMsg = parseErrorMessage(response)
                android.util.Log.e(TAG, "❌ Error al obtener comentarios: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Exception: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Obtener respuestas de un comentario
     * @param idComentario ID del comentario padre
     * @param token Token de autorización
     * @param skip Número de registros a saltar
     * @param limit Límite de registros
     * @return Result<List<ComentarioResponse>>
     */
    suspend fun obtenerRespuestasComentario(
        idComentario: String,
        token: String,
        skip: Int = 0,
        limit: Int = 10
    ): Result<List<ComentarioResponse>> {
        return try {
            android.util.Log.d(TAG, "=== obtenerRespuestasComentario ===")
            android.util.Log.d(TAG, "ID Comentario: $idComentario")

            val response = apiService.obtenerRespuestasDeComentario(
                idComentario = idComentario,
                skip = skip,
                limit = limit,
                authorization = "Bearer $token"
            )

            android.util.Log.d(TAG, "Response code: ${response.code()}")

            if (response.isSuccessful && response.body() != null) {
                val respuestas = response.body()!!
                android.util.Log.d(TAG, "✅ Respuestas obtenidas: ${respuestas.size}")
                Result.success(respuestas)
            } else {
                val errorMsg = parseErrorMessage(response)
                android.util.Log.e(TAG, "❌ Error al obtener respuestas: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Exception: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Obtener comentario específico por ID
     * @param idComentario ID del comentario
     * @param token Token de autorización
     * @return Result<ComentarioResponse>
     */
    suspend fun obtenerComentarioPorId(
        idComentario: String,
        token: String
    ): Result<ComentarioResponse> {
        return try {
            android.util.Log.d(TAG, "=== obtenerComentarioPorId ===")
            android.util.Log.d(TAG, "ID Comentario: $idComentario")

            val response = apiService.obtenerComentario(
                idComentario = idComentario,
                authorization = "Bearer $token"
            )

            android.util.Log.d(TAG, "Response code: ${response.code()}")

            if (response.isSuccessful && response.body() != null) {
                android.util.Log.d(TAG, "✅ Comentario obtenido")
                Result.success(response.body()!!)
            } else {
                val errorMsg = parseErrorMessage(response)
                android.util.Log.e(TAG, "❌ Error al obtener comentario: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Exception: ${e.message}")
            Result.failure(e)
        }
    }

    // ============================================
    // EDITAR Y ELIMINAR COMENTARIOS
    // ============================================

    /**
     * Editar comentario existente
     * @param idComentario ID del comentario a editar
     * @param nuevoTexto Nuevo texto del comentario
     * @param idUsuario ID del usuario (debe ser el autor)
     * @param token Token de autorización
     * @return Result<ComentarioResponse>
     */
    suspend fun editarComentario(
        idComentario: String,
        nuevoTexto: String,
        idUsuario: String,
        token: String
    ): Result<ComentarioResponse> {
        return try {
            android.util.Log.d(TAG, "=== editarComentario ===")
            android.util.Log.d(TAG, "ID Comentario: $idComentario")
            android.util.Log.d(TAG, "Nuevo texto: ${nuevoTexto.take(50)}...")

            val request = ComentarioUpdateRequest(comentario = nuevoTexto)

            val response = apiService.editarComentario(
                idComentario = idComentario,
                request = request,
                idUsuario = idUsuario,
                authorization = "Bearer $token"
            )

            android.util.Log.d(TAG, "Response code: ${response.code()}")

            if (response.isSuccessful && response.body() != null) {
                android.util.Log.d(TAG, "✅ Comentario editado exitosamente")
                Result.success(response.body()!!)
            } else {
                val errorMsg = parseErrorMessage(response)
                android.util.Log.e(TAG, "❌ Error al editar comentario: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Exception: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Eliminar comentario
     * @param idComentario ID del comentario a eliminar
     * @param idUsuario ID del usuario (debe ser el autor)
     * @param token Token de autorización
     * @return Result<Boolean>
     */
    suspend fun eliminarComentario(
        idComentario: String,
        idUsuario: String,
        token: String
    ): Result<Boolean> {
        return try {
            android.util.Log.d(TAG, "=== eliminarComentario ===")
            android.util.Log.d(TAG, "ID Comentario: $idComentario")
            android.util.Log.d(TAG, "ID Usuario: $idUsuario")

            val response = apiService.eliminarComentario(
                idComentario = idComentario,
                idUsuario = idUsuario,
                authorization = "Bearer $token"
            )

            android.util.Log.d(TAG, "Response code: ${response.code()}")

            if (response.isSuccessful) {
                android.util.Log.d(TAG, "✅ Comentario eliminado exitosamente")
                Result.success(true)
            } else {
                val errorMsg = parseErrorMessage(response)
                android.util.Log.e(TAG, "❌ Error al eliminar comentario: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Exception: ${e.message}")
            Result.failure(e)
        }
    }

    // ============================================
    // MÉTODOS DE CONVENIENCIA Y CONVERSIÓN
    // ============================================

    /**
     * Obtener comentarios convertidos para CommentAdapter existente
     * @param idPublicacion ID de la publicación
     * @param token Token de autorización
     * @return Result<List<Comment>>
     */
    suspend fun obtenerComentariosConvertidos(
        idPublicacion: String,
        token: String
    ): Result<List<Comment>> {
        return try {
            android.util.Log.d(TAG, "=== obtenerComentariosConvertidos ===")

            // Obtener comentarios principales
            val comentariosResult = obtenerComentariosPublicacion(idPublicacion, token)

            comentariosResult.fold(
                onSuccess = { comentarios ->
                    // Convertir a Comment y cargar respuestas
                    val commentsConvertidos = comentarios.map { comentario ->
                        val comment = comentario.toComment()

                        // Si tiene respuestas, cargarlas
                        if (comentario.totalRespuestas > 0) {
                            val respuestasResult = obtenerRespuestasComentario(comentario.id, token)
                            respuestasResult.fold(
                                onSuccess = { respuestas ->
                                    comment.replies.clear()
                                    comment.replies.addAll(
                                        respuestas.map { respuesta ->
                                            Comment.Reply(
                                                id = respuesta.id.hashCode(),
                                                user = respuesta.usuarioAlias ?: "Usuario",
                                                text = respuesta.comentario,
                                                timestamp = formatearFechaComentario(respuesta.fechaCreacion),
                                                userLikeState = Comment.LikeState.NONE
                                            )
                                        }
                                    )
                                },
                                onFailure = { error ->
                                    android.util.Log.w(TAG, "No se pudieron cargar respuestas para ${comentario.id}: ${error.message}")
                                }
                            )
                        }

                        comment
                    }

                    android.util.Log.d(TAG, "✅ Convertidos ${commentsConvertidos.size} comentarios con respuestas")
                    Result.success(commentsConvertidos)
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Exception en obtenerComentariosConvertidos: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Crear comentario y devolver Comment convertido
     * @param idPublicacion ID de la publicación
     * @param comentario Texto del comentario
     * @param idUsuario ID del usuario
     * @param token Token de autorización
     * @return Result<Comment>
     */
    suspend fun crearComentarioConvertido(
        idPublicacion: String,
        comentario: String,
        idUsuario: String,
        token: String
    ): Result<Comment> {
        return try {
            val result = crearComentario(idPublicacion, comentario, idUsuario, token)

            result.fold(
                onSuccess = { comentarioResponse ->
                    Result.success(comentarioResponse.toComment())
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Crear respuesta y devolver Reply convertido
     * @param idComentarioPadre ID del comentario padre
     * @param comentario Texto de la respuesta
     * @param idUsuario ID del usuario
     * @param token Token de autorización
     * @return Result<Comment.Reply>
     */
    suspend fun crearRespuestaConvertida(
        idComentarioPadre: String,
        comentario: String,
        idUsuario: String,
        token: String
    ): Result<Comment.Reply> {
        return try {
            val result = crearRespuesta(idComentarioPadre, comentario, idUsuario, token)

            result.fold(
                onSuccess = { respuestaResponse ->
                    val reply = Comment.Reply(
                        id = respuestaResponse.id.hashCode(),
                        user = respuestaResponse.usuarioAlias ?: "Usuario",
                        text = respuestaResponse.comentario,
                        timestamp = formatearFechaComentario(respuestaResponse.fechaCreacion),
                        userLikeState = Comment.LikeState.NONE
                    )
                    Result.success(reply)
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ============================================
    // REACCIONES EN COMENTARIOS
    // ============================================

    /**
     * Agregar o actualizar reacción a un comentario
     * @param idComentario ID del comentario
     * @param idUsuario ID del usuario
     * @param tipoReaccion "like" o "dislike"
     * @param token Token de autorización
     * @return Result<ReaccionResponse>
     */
    suspend fun agregarReaccionComentario(
        idComentario: String,
        idUsuario: String,
        tipoReaccion: String,
        token: String
    ): Result<ReaccionResponse> {
        return try {
            android.util.Log.d(TAG, "=== agregarReaccionComentario ===")
            android.util.Log.d(TAG, "ID Comentario: $idComentario")
            android.util.Log.d(TAG, "ID Usuario: $idUsuario")
            android.util.Log.d(TAG, "Tipo Reacción: $tipoReaccion")

            val response = apiService.agregarReaccionComentario(
                idComentario = idComentario,
                idUsuario = idUsuario,
                tipoReaccion = tipoReaccion,
                authorization = "Bearer $token"
            )

            android.util.Log.d(TAG, "Response code: ${response.code()}")

            if (response.isSuccessful && response.body() != null) {
                android.util.Log.d(TAG, "✅ Reacción en comentario agregada exitosamente")
                Result.success(response.body()!!)
            } else {
                val errorMsg = parseErrorMessage(response)
                android.util.Log.e(TAG, "❌ Error al agregar reacción: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Exception: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Quitar reacción de un comentario
     * @param idComentario ID del comentario
     * @param idUsuario ID del usuario
     * @param token Token de autorización
     * @return Result<Boolean>
     */
    suspend fun quitarReaccionComentario(
        idComentario: String,
        idUsuario: String,
        token: String
    ): Result<Boolean> {
        return try {
            android.util.Log.d(TAG, "=== quitarReaccionComentario ===")
            android.util.Log.d(TAG, "ID Comentario: $idComentario")
            android.util.Log.d(TAG, "ID Usuario: $idUsuario")

            val response = apiService.eliminarReaccionComentario(
                idComentario = idComentario,
                idUsuario = idUsuario,
                authorization = "Bearer $token"
            )

            android.util.Log.d(TAG, "Response code: ${response.code()}")

            if (response.isSuccessful) {
                android.util.Log.d(TAG, "✅ Reacción removida exitosamente")
                Result.success(true)
            } else {
                val errorMsg = parseErrorMessage(response)
                android.util.Log.e(TAG, "❌ Error al quitar reacción: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Exception: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Obtener conteo de reacciones de un comentario
     * @param idComentario ID del comentario
     * @param token Token de autorización
     * @return Result<ConteoReaccionesComentarioResponse>
     */
    suspend fun obtenerConteoReaccionesComentario(
        idComentario: String,
        token: String
    ): Result<ConteoReaccionesComentarioResponse> {
        return try {
            android.util.Log.d(TAG, "=== obtenerConteoReaccionesComentario ===")
            android.util.Log.d(TAG, "ID Comentario: $idComentario")

            val response = apiService.obtenerConteoReaccionesComentario(
                idComentario = idComentario,
                authorization = "Bearer $token"
            )

            android.util.Log.d(TAG, "Response code: ${response.code()}")

            if (response.isSuccessful && response.body() != null) {
                val conteo = response.body()!!
                android.util.Log.d(TAG, "✅ Conteo obtenido - Likes: ${conteo.likes}, Dislikes: ${conteo.dislikes}")
                Result.success(conteo)
            } else {
                val errorMsg = parseErrorMessage(response)
                android.util.Log.e(TAG, "❌ Error al obtener conteo: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Exception: ${e.message}")
            Result.failure(e)
        }
    }

    // ============================================
    // MÉTODOS DE TOGGLE PARA REACCIONES EN COMENTARIOS
    // ============================================

    /**
     * Toggle like en comentario (similar al patrón de PublicacionRepository)
     * @param idComentario ID del comentario
     * @param idUsuario ID del usuario
     * @param token Token de autorización
     * @return Result<EstadoReaccionComentario> - Estado final después del toggle
     */
    suspend fun toggleLikeComentario(
        idComentario: String,
        idUsuario: String,
        token: String
    ): Result<EstadoReaccionComentario> {
        return try {
            android.util.Log.d(TAG, "=== toggleLikeComentario ===")

            // Primero obtener conteo actual para determinar estado
            val conteoResult = obtenerConteoReaccionesComentario(idComentario, token)

            conteoResult.fold(
                onSuccess = { conteo ->
                    // Intentar agregar like
                    val reaccionResult = agregarReaccionComentario(idComentario, idUsuario, "like", token)

                    reaccionResult.fold(
                        onSuccess = { reaccion ->
                            android.util.Log.d(TAG, "Toggle like completado: ${reaccion.tipoReaccion}")

                            val tieneReaccion = true
                            val tipoReaccion = reaccion.tipoReaccion
                            val esLike = tipoReaccion == "like"
                            val esDislike = tipoReaccion == "dislike"

                            Result.success(EstadoReaccionComentario(
                                tieneReaccion = tieneReaccion,
                                tipoReaccion = tipoReaccion,
                                esLike = esLike,
                                esDislike = esDislike
                            ))
                        },
                        onFailure = { error ->
                            android.util.Log.e(TAG, "Error en toggle like: ${error.message}")
                            Result.failure(error)
                        }
                    )
                },
                onFailure = { error ->
                    android.util.Log.e(TAG, "Error al obtener conteo para toggle: ${error.message}")
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Exception en toggleLikeComentario: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Toggle dislike en comentario
     * @param idComentario ID del comentario
     * @param idUsuario ID del usuario
     * @param token Token de autorización
     * @return Result<EstadoReaccionComentario>
     */
    suspend fun toggleDislikeComentario(
        idComentario: String,
        idUsuario: String,
        token: String
    ): Result<EstadoReaccionComentario> {
        return try {
            android.util.Log.d(TAG, "=== toggleDislikeComentario ===")

            val conteoResult = obtenerConteoReaccionesComentario(idComentario, token)

            conteoResult.fold(
                onSuccess = { conteo ->
                    val reaccionResult = agregarReaccionComentario(idComentario, idUsuario, "dislike", token)

                    reaccionResult.fold(
                        onSuccess = { reaccion ->
                            android.util.Log.d(TAG, "Toggle dislike completado: ${reaccion.tipoReaccion}")

                            val tieneReaccion = true
                            val tipoReaccion = reaccion.tipoReaccion
                            val esLike = tipoReaccion == "like"
                            val esDislike = tipoReaccion == "dislike"

                            Result.success(EstadoReaccionComentario(
                                tieneReaccion = tieneReaccion,
                                tipoReaccion = tipoReaccion,
                                esLike = esLike,
                                esDislike = esDislike
                            ))
                        },
                        onFailure = { error ->
                            Result.failure(error)
                        }
                    )
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Exception en toggleDislikeComentario: ${e.message}")
            Result.failure(e)
        }
    }

    // ============================================
    // HELPER METHODS
    // ============================================

    /**
     * Parsear mensaje de error de la respuesta
     */
    private fun parseErrorMessage(response: Response<*>): String {
        return try {
            response.errorBody()?.string() ?: "Error desconocido (${response.code()})"
        } catch (e: Exception) {
            "Error al procesar respuesta: ${response.code()}"
        }
    }

    /**
     * Formatear fecha de comentario para mostrar
     */
    private fun formatearFechaComentario(fechaISO: String): String {
        return try {
            if (fechaISO.contains("T")) {
                val partes = fechaISO.split("T")
                val fecha = partes[0] // "2025-11-23"
                val hora = partes[1].substring(0, 5) // "10:30"

                val fechaParts = fecha.split("-")
                if (fechaParts.size == 3) {
                    "${fechaParts[2]}/${fechaParts[1]} $hora"
                } else {
                    "Hace poco"
                }
            } else {
                "Hace poco"
            }
        } catch (e: Exception) {
            "Hace poco"
        }
    }

    // ============================================
    // CLASES DE DATOS PARA REACCIONES
    // ============================================

    /**
     * Clase de datos para representar el estado de reacción de un comentario
     */
    data class EstadoReaccionComentario(
        val tieneReaccion: Boolean,
        val tipoReaccion: String?, // "like", "dislike" o null
        val esLike: Boolean = tipoReaccion == "like",
        val esDislike: Boolean = tipoReaccion == "dislike"
    )
}