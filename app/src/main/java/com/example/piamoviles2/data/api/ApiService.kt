package com.example.piamoviles2.data.api

import com.example.piamoviles2.data.models.*
import retrofit2.Response
import retrofit2.http.*

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Multipart
import retrofit2.http.Part
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Header

/**
 * Interfaz de servicios API para "El Sazón de Toto"
 * Contiene todos los endpoints para usuarios y publicaciones
 */
interface ApiService {

    // ============================================
    // ENDPOINTS DE USUARIOS
    // ============================================

    // POST - REGISTRO DE USUARIO
    @Multipart
    @POST("api/usuarios/registro")
    suspend fun registrarUsuario(
        @Part("email") email: RequestBody,
        @Part("alias") alias: RequestBody,
        @Part("password") contrasena: RequestBody,
        @Part("nombre") nombre: RequestBody?,
        @Part("apellido_paterno") apellidoPaterno: RequestBody?,
        @Part("apellido_materno") apellidoMaterno: RequestBody?,
        @Part("telefono") telefono: RequestBody?,
        @Part("direccion") direccion: RequestBody?,
        @Part foto_perfil: MultipartBody.Part?
    ): Response<UsuarioResponse>

    // POST - LOGIN
    @POST("api/usuarios/login")
    suspend fun loginUsuario(
        @Query("email") email: String,
        @Query("contraseña") contrasena: String
    ): Response<LoginResponse>

    @GET("api/usuarios/perfil/{usuario_id}")
    suspend fun obtenerPerfil(
        @Path("usuario_id") usuarioId: String,
        @Header("Authorization") authorization: String
    ): Response<UsuarioResponse>

    @GET("api/usuarios/{usuario_id}")
    suspend fun obtenerUsuario(
        @Path("usuario_id") usuarioId: String,
        @Header("Authorization") authorization: String
    ): Response<UsuarioResponse>

    // ENDPOINTS DE PERFIL Y EDICIÓN
    @GET("api/usuarios/{usuario_id}")
    suspend fun obtenerPerfilUsuario(
        @Path("usuario_id") usuarioId: String,
        @Header("Authorization") authHeader: String
    ): Response<UsuarioResponse>

    // Actualizar Usuario
    @Multipart
    @PUT("api/usuarios/{usuario_id}")
    suspend fun actualizarUsuario(
        @Path("usuario_id") usuarioId: String,
        @Header("Authorization") authorization: String,
        @Part("email") email: RequestBody?,
        @Part("alias") alias: RequestBody?,
        @Part("nombre") nombre: RequestBody?,
        @Part("apellido_paterno") apellidoPaterno: RequestBody?,
        @Part("apellido_materno") apellidoMaterno: RequestBody?,
        @Part("telefono") telefono: RequestBody?,
        @Part("direccion") direccion: RequestBody?,
        @Part fotoPerfil: MultipartBody.Part?
    ): Response<UsuarioResponse>

    @POST("api/usuarios/{usuario_id}/cambiar-contraseña")
    suspend fun cambiarContrasena(
        @Path("usuario_id") usuarioId: String,
        @Body request: CambiarContrasenaRequest,
        @Header("Authorization") authHeader: String
    ): Response<CambiarContrasenaResponse>

    // ============================================
    // ENDPOINTS DE PUBLICACIONES
    // ============================================

    // Crear nueva publicación con archivos
    @Multipart
    @POST("api/publicaciones/crear_publicacion")
    suspend fun crearPublicacion(
        @Part("titulo") titulo: RequestBody,
        @Part("descripcion") descripcion: RequestBody?,
        @Part("estatus") estatus: RequestBody,
        @Part("id_autor") idAutor: RequestBody,
        @Part archivos: List<MultipartBody.Part>?,
        @Header("Authorization") authorization: String
    ): Response<PublicacionDetalle>

    // Obtener feed de publicaciones
    @GET("api/publicaciones/get_feed")
    suspend fun obtenerFeedPublicaciones(
        @Header("Authorization") authorization: String
    ): Response<List<PublicacionListFeed>>

    // Obtener publicación por ID
    @GET("api/publicaciones/{id_publicacion}")
    suspend fun obtenerPublicacionPorId(
        @Path("id_publicacion") idPublicacion: String,
        @Header("Authorization") authorization: String
    ): Response<PublicacionDetalle>

    // Actualizar publicación existente
    @Multipart
    @PUT("api/publicaciones/update_pub/{id_publicacion}")
    suspend fun actualizarPublicacion(
        @Path("id_publicacion") idPublicacion: String,
        @Part("titulo") titulo: RequestBody?,
        @Part("descripcion") descripcion: RequestBody?,
        @Part("estatus") estatus: RequestBody?,
        @Part archivos: List<MultipartBody.Part>?,
        @Header("Authorization") authorization: String
    ): Response<PublicacionDetalle>

    // Eliminar publicación
    @DELETE("api/publicaciones/delete_pub/{id_publicacion}")
    suspend fun eliminarPublicacion(
        @Path("id_publicacion") idPublicacion: String,
        @Header("Authorization") authorization: String
    ): Response<PublicacionDeletedResponse>

    // ============================================
    // ENDPOINTS DE PUBLICACIONES DE USUARIO
    // ============================================

    // Obtener publicaciones del usuario (método genérico con parámetro)
    @GET("api/publicaciones/usuario/{id_autor}")
    suspend fun obtenerPublicacionesUsuario(
        @Path("id_autor") idAutor: String,
        @Header("Authorization") authorization: String,
        @Query("incluir_borradores") incluirBorradores: Boolean = false
    ): Response<List<PublicacionListFeed>>

    // Obtener publicaciones activas del usuario
    @GET("api/publicaciones/get_user_active_pubs/{id_usuario}")
    suspend fun obtenerPublicacionesActivasUsuario(
        @Path("id_usuario") idUsuario: String,
        @Header("Authorization") authorization: String
    ): Response<List<PublicacionListFeed>>

    // Obtener borradores del usuario
    @GET("api/publicaciones/get_user_drafts/{id_usuario}")
    suspend fun obtenerBorradoresUsuario(
        @Path("id_usuario") idUsuario: String,
        @Header("Authorization") authorization: String
    ): Response<List<PublicacionListFeed>>

    // Obtener favoritas del usuario
    @GET("api/publicaciones/get_fav_pubs/{id_usuario}")
    suspend fun obtenerFavoritasUsuario(
        @Path("id_usuario") idUsuario: String,
        @Header("Authorization") authorization: String
    ): Response<List<PublicacionListFeed>>

    // Obtener publicación detalle completa por ID (para pantalla de detalles)
    @GET("api/publicaciones/{id_publicacion}")
    suspend fun obtenerPublicacionDetalleCompleta(
        @Path("id_publicacion") idPublicacion: String,
        @Header("Authorization") authorization: String
    ): Response<PublicacionDetalleCompleta>

    // ============================================
    // 🆕 ENDPOINTS DE REACCIONES (LIKES/DISLIKES)
    // ============================================

    /**
     * Agregar o actualizar reacción a una publicación
     * POST /api/reaccion/publicacion/{id_publicacion}?id_usuario=xxx&tipo_reaccion=like
     */
    @POST("api/reaccion/publicacion/{id_publicacion}")
    suspend fun agregarReaccion(
        @Path("id_publicacion") idPublicacion: String,
        @Query("id_usuario") idUsuario: String,
        @Query("tipo_reaccion") tipoReaccion: String, // "like" o "dislike"
        @Header("Authorization") authorization: String
    ): Response<ReaccionResponse>

    /**
     * Eliminar reacción de una publicación
     * DELETE /api/reaccion/publicacion/{id_publicacion}?id_usuario=xxx
     */
    @DELETE("api/reaccion/publicacion/{id_publicacion}")
    suspend fun eliminarReaccion(
        @Path("id_publicacion") idPublicacion: String,
        @Query("id_usuario") idUsuario: String,
        @Header("Authorization") authorization: String
    ): Response<Void>

    /**
     * Obtener conteo de reacciones de una publicación
     * GET /api/reaccion/publicacion/{id_publicacion}/conteo
     */
    @GET("api/reaccion/publicacion/{id_publicacion}/conteo")
    suspend fun obtenerConteoReacciones(
        @Path("id_publicacion") idPublicacion: String,
        @Header("Authorization") authorization: String
    ): Response<ConteoReaccionesResponse>

    /**
     * Verificar si un usuario ha reaccionado a una publicación
     * GET /api/reaccion/publicacion/{id_publicacion}/usuario/{id_usuario}
     */
    @GET("api/reaccion/publicacion/{id_publicacion}/usuario/{id_usuario}")
    suspend fun verificarReaccionUsuario(
        @Path("id_publicacion") idPublicacion: String,
        @Path("id_usuario") idUsuario: String,
        @Header("Authorization") authorization: String
    ): Response<VerificarReaccionResponse>

    // ============================================
    // 🆕 ENDPOINTS DE COMENTARIOS
    // ============================================

    /**
     * Crear comentario en una publicación
     * POST /api/comentario/publicacion/{id_publicacion}?comentario=xxx&id_usuario=xxx
     */
    @POST("api/comentario/publicacion/{id_publicacion}")
    suspend fun crearComentarioEnPublicacion(
        @Path("id_publicacion") idPublicacion: String,
        @Query("comentario") comentario: String,
        @Query("id_usuario") idUsuario: String,
        @Header("Authorization") authorization: String
    ): Response<ComentarioResponse>

    /**
     * Crear respuesta a un comentario
     * POST /api/comentario/respuesta/{id_comentario_padre}?comentario=xxx&id_usuario=xxx
     */
    @POST("api/comentario/respuesta/{id_comentario_padre}")
    suspend fun crearRespuestaAComentario(
        @Path("id_comentario_padre") idComentarioPadre: String,
        @Query("comentario") comentario: String,
        @Query("id_usuario") idUsuario: String,
        @Header("Authorization") authorization: String
    ): Response<ComentarioResponse>

    /**
     * Obtener comentarios de una publicación
     * GET /api/comentario/publicacion/{id_publicacion}?skip=0&limit=20
     */
    @GET("api/comentario/publicacion/{id_publicacion}")
    suspend fun obtenerComentariosDePublicacion(
        @Path("id_publicacion") idPublicacion: String,
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 20,
        @Header("Authorization") authorization: String
    ): Response<List<ComentarioResponse>>

    /**
     * Obtener respuestas de un comentario
     * GET /api/comentario/{id_comentario}/respuestas?skip=0&limit=10
     */
    @GET("api/comentario/{id_comentario}/respuestas")
    suspend fun obtenerRespuestasDeComentario(
        @Path("id_comentario") idComentario: String,
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 10,
        @Header("Authorization") authorization: String
    ): Response<List<ComentarioResponse>>

    /**
     * Obtener comentario específico por ID
     * GET /api/comentario/{id_comentario}
     */
    @GET("api/comentario/{id_comentario}")
    suspend fun obtenerComentario(
        @Path("id_comentario") idComentario: String,
        @Header("Authorization") authorization: String
    ): Response<ComentarioResponse>

    /**
     * Obtener comentarios de un usuario
     * GET /api/comentario/usuario/{id_usuario}?skip=0&limit=20
     */
    @GET("api/comentario/usuario/{id_usuario}")
    suspend fun obtenerComentariosDeUsuario(
        @Path("id_usuario") idUsuario: String,
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 20,
        @Header("Authorization") authorization: String
    ): Response<List<ComentarioResponse>>

    /**
     * Editar comentario existente
     * PUT /api/comentario/{id_comentario}?id_usuario=xxx
     */
    @PUT("api/comentario/{id_comentario}")
    suspend fun editarComentario(
        @Path("id_comentario") idComentario: String,
        @Body request: ComentarioUpdateRequest,
        @Query("id_usuario") idUsuario: String,
        @Header("Authorization") authorization: String
    ): Response<ComentarioResponse>

    /**
     * Eliminar comentario
     * DELETE /api/comentario/{id_comentario}?id_usuario=xxx
     */
    @DELETE("api/comentario/{id_comentario}")
    suspend fun eliminarComentario(
        @Path("id_comentario") idComentario: String,
        @Query("id_usuario") idUsuario: String,
        @Header("Authorization") authorization: String
    ): Response<Void>

}