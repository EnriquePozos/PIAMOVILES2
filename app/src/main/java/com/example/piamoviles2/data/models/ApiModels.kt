package com.example.piamoviles2.data.models

import com.google.gson.annotations.SerializedName

// ============================================
// USUARIO API MODELS
// ============================================
data class UsuarioCreateRequest(
    val email: String,
    val alias: String,
    @SerializedName("contraseña") val contrasena: String,
    val nombre: String,
    @SerializedName("apellido_paterno") val apellidoPaterno: String,
    @SerializedName("apellido_materno") val apellidoMaterno: String?,
    val telefono: String?,
    val direccion: String?,
    @SerializedName("foto_perfil") val fotoPerfil: String?
)

data class LoginRequest(
    val email: String,
    @SerializedName("contraseña") val contrasena: String
)

data class LoginResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("token_type") val tokenType: String,
    val usuario: UsuarioResponse
)

data class UsuarioResponse(
    val id: String,
    val email: String,
    val alias: String,
    val nombre: String,
    @SerializedName("apellido_paterno") val apellidoPaterno: String,
    @SerializedName("apellido_materno") val apellidoMaterno: String?,
    val telefono: String?,
    val direccion: String?,
    @SerializedName("fecha_registro") val fechaRegistro: String,
    @SerializedName("foto_perfil") val fotoPerfil: String?
)

// ============================================
// PUBLICACION API MODELS
// ============================================
data class PublicacionResponse(
    val id: String,
    val titulo: String,
    val descripcion: String?,
    @SerializedName("fecha_creacion") val fechaCreacion: String,
    @SerializedName("fecha_publicacion") val fechaPublicacion: String?,
    val estatus: String,
    @SerializedName("id_autor") val idAutor: String,
    @SerializedName("autor_alias") val autorAlias: String?,
    @SerializedName("autor_foto") val autorFoto: String?,
    @SerializedName("total_comentarios") val totalComentarios: Int = 0,
    @SerializedName("total_reacciones") val totalReacciones: Int = 0,
    @SerializedName("total_favoritos") val totalFavoritos: Int = 0
)

// ============================================
// RESPONSE WRAPPERS
// ============================================
data class ApiResponse<T>(
    val success: Boolean,
    val data: T?,
    val message: String?
)

data class ErrorResponse(
    val detail: String
)
