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

}