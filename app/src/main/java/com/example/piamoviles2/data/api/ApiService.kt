package com.example.piamoviles2.data.api

import com.example.piamoviles2.data.models.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // ============================================
    // USUARIOS
    // ============================================
    @POST("api/usuarios/registro")
    suspend fun registrarUsuario(
        @Body request: UsuarioCreateRequest
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

    // ============================================
    // PUBLICACIONES (para futuras implementaciones)
    // ============================================
    @GET("api/publicaciones/feed")
    suspend fun obtenerFeed(
        @Header("Authorization") authorization: String,
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 20
    ): Response<List<PublicacionResponse>>
}