// Ubicación: app/src/main/java/com/example/piamoviles2/data/models/ApiModels.kt

package com.example.piamoviles2.data.models

import com.google.gson.annotations.SerializedName


// USUARIO API MODELS
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


// REGISTRO DE USUARIO - REQUEST & RESPONSE
data class RegistroRequest(
    val email: String,
    val alias: String,
    @SerializedName("contraseña") val contrasena: String,
    val nombre: String,
    @SerializedName("apellido_paterno") val apellidoPaterno: String,
    @SerializedName("apellido_materno") val apellidoMaterno: String? = null,
    val telefono: String? = null,
    val direccion: String? = null,
    @SerializedName("foto_perfil") val fotoPerfil: String? = null
)

data class RegistroResponse(
    val id: String,
    val email: String,
    val alias: String,
    val nombre: String,
    @SerializedName("apellido_paterno") val apellidoPaterno: String,
    @SerializedName("apellido_materno") val apellidoMaterno: String?,
    val telefono: String?,
    val direccion: String?,
    @SerializedName("foto_perfil") val fotoPerfil: String?,
    @SerializedName("fecha_registro") val fechaRegistro: String
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


// PUBLICACION API MODELS
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


// PUBLICACIONES RESPONSE MODELS DETALLADOS
data class PublicacionDetalle(
    val id: String,
    val titulo: String,
    val descripcion: String?,
    @SerializedName("fecha_creacion") val fechaCreacion: String,
    @SerializedName("fecha_publicacion") val fechaPublicacion: String?,
    @SerializedName("fecha_modificacion") val fechaModificacion: String?,
    val estatus: String,
    @SerializedName("id_autor") val idAutor: String,
    @SerializedName("autor_alias") val autorAlias: String?,
    @SerializedName("autor_foto") val autorFoto: String?,
    @SerializedName("total_comentarios") val totalComentarios: Int = 0,
    @SerializedName("total_reacciones") val totalReacciones: Int = 0,
    @SerializedName("total_favoritos") val totalFavoritos: Int = 0,
    val multimedia: List<MultimediaResponse> = emptyList()
)

data class PublicacionListFeed(
    val id: String,
    val titulo: String,
    val descripcion: String?,
    @SerializedName("fecha_publicacion") val fechaPublicacion: String?,
    val estatus: String,
    @SerializedName("id_autor") val idAutor: String,
    @SerializedName("autor_alias") val autorAlias: String?,
    @SerializedName("autor_foto") val autorFoto: String?,
    @SerializedName("total_comentarios") val totalComentarios: Int = 0,
    @SerializedName("total_reacciones") val totalReacciones: Int = 0,
    @SerializedName("imagen_preview") val imagenPreview: String?
)


// PUBLICACION DETALLE COMPLETA (PARA PANTALLA DE DETALLES)
data class PublicacionDetalleCompleta(
    val id: String,
    val titulo: String,
    val descripcion: String?,
    @SerializedName("fecha_publicacion") val fechaPublicacion: String?,
    @SerializedName("fecha_creacion") val fechaCreacion: String?,
    val estatus: String,
    @SerializedName("id_autor") val idAutor: String,

    // Datos del autor
    @SerializedName("autor_alias") val autorAlias: String?,
    @SerializedName("autor_foto") val autorFoto: String?,

    // Estadísticas
    @SerializedName("total_comentarios") val totalComentarios: Int = 0,
    @SerializedName("total_reacciones") val totalReacciones: Int = 0,

    // Multimedia completa
    @SerializedName("multimedia_list") val multimediaList: List<MultimediaDetalle> = emptyList(),

    // Comentarios completos
    @SerializedName("comentarios") val comentarios: List<ComentarioDetalle> = emptyList()
)

data class MultimediaDetalle(
    val id: String,
    val url: String,
    val tipo: String, // "imagen", "video"
    val descripcion: String?
)

data class ComentarioDetalle(
    val id: String,
    val contenido: String,
    @SerializedName("fecha_creacion") val fechaCreacion: String?,
    @SerializedName("id_usuario") val idUsuario: String,
    @SerializedName("usuario_alias") val usuarioAlias: String?,
    @SerializedName("usuario_foto") val usuarioFoto: String?
)

// ACTUALIZAR USUARIO - REQUEST
data class UsuarioUpdateRequest(
    val email: String? = null,
    val alias: String? = null,
    val nombre: String? = null,
    @SerializedName("apellido_paterno") val apellidoPaterno: String? = null,
    @SerializedName("apellido_materno") val apellidoMaterno: String? = null,
    val telefono: String? = null,
    val direccion: String? = null,
    @SerializedName("foto_perfil") val fotoPerfil: String? = null
)


// CAMBIAR CONTRASEÑA - REQUEST & RESPONSE
data class CambiarContrasenaRequest(
    @SerializedName("contraseña_actual") val contrasenaActual: String,
    @SerializedName("contraseña_nueva") val contrasenaNueva: String
)

data class CambiarContrasenaResponse(
    val message: String
)


// RESPONSE WRAPPERS
data class ApiResponse<T>(
    val success: Boolean,
    val data: T?,
    val message: String?
)

data class ErrorResponse(
    val detail: String
)


// MULTIMEDIA API MODELS
data class MultimediaResponse(
    val id: String,
    val url: String,
    val tipo: String, // "imagen" o "video"
    @SerializedName("fecha_subida") val fechaSubida: String,
    @SerializedName("id_publicacion") val idPublicacion: String
)


// PUBLICACIONES REQUEST MODELS
data class CrearPublicacionRequest(
    val titulo: String,
    val descripcion: String?,
    val estatus: String, // "borrador" o "publicada"
    @SerializedName("id_autor") val idAutor: String
)

data class ActualizarPublicacionRequest(
    val titulo: String?,
    val descripcion: String?,
    val estatus: String? // "borrador" o "publicada"
)


// SUCCESS RESPONSE MODELS
data class PublicacionCreatedResponse(
    val message: String,
    val publicacion: PublicacionDetalle
)

data class PublicacionUpdatedResponse(
    val message: String,
    val publicacion: PublicacionDetalle
)

data class PublicacionDeletedResponse(
    val message: String
)


// REACCIONES API MODELS

// Enum para tipos de reacción (coincide con el backend)
enum class TipoReaccion(val value: String) {
    LIKE("like"),
    DISLIKE("dislike");

    companion object {
        fun fromString(value: String): TipoReaccion? {
            return values().find { it.value == value }
        }
    }
}


// RESPONSE MODELS PARA REACCIONES

// Respuesta al crear/actualizar reacción
data class ReaccionResponse(
    val id: String,
    val reaccion: String, // "like" o "dislike"
    @SerializedName("id_usuario") val idUsuario: String,
    @SerializedName("id_publicacion") val idPublicacion: String?,
    @SerializedName("id_comentario") val idComentario: String?
)

// Respuesta de conteo de reacciones de una publicación
data class ConteoReaccionesResponse(
    @SerializedName("id_publicacion") val idPublicacion: String,
    val likes: Int,
    val dislikes: Int,
    val total: Int
)

// Respuesta para verificar reacción de usuario
data class VerificarReaccionResponse(
    @SerializedName("tiene_reaccion") val tieneReaccion: Boolean,
    @SerializedName("tipo_reaccion") val tipoReaccion: String? // "like", "dislike" o null
) {
    // Helpers para facilitar el uso
    fun esLike(): Boolean = tipoReaccion == "like"
    fun esDislike(): Boolean = tipoReaccion == "dislike"
    fun getTipoReaccionEnum(): TipoReaccion? = tipoReaccion?.let { TipoReaccion.fromString(it) }
}

// ============================================
// SUCCESS RESPONSE MODELS PARA REACCIONES
// ============================================

data class ReaccionCreatedResponse(
    val message: String,
    val reaccion: ReaccionResponse
)

data class ReaccionDeletedResponse(
    val message: String
)