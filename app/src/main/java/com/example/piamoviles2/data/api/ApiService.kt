package com.example.piamoviles2.data.api

import com.example.piamoviles2.data.models.*
import retrofit2.Response
import retrofit2.http.*

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Multipart
import retrofit2.http.Part

interface ApiService {

    // USUARIOS

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