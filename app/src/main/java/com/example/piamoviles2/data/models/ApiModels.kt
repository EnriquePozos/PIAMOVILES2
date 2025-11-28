// Ubicación: app/src/main/java/com/example/piamoviles2/data/models/ApiModels.kt

package com.example.piamoviles2.data.models

import com.google.gson.annotations.SerializedName
import com.example.piamoviles2.Comment

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

// ============================================
// REGISTRO DE USUARIO - REQUEST & RESPONSE
// ============================================
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
// PUBLICACIONES RESPONSE MODELS DETALLADOS
// ============================================
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

// ============================================
// PUBLICACION DETALLE COMPLETA (PARA PANTALLA DE DETALLES)
// ============================================
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

// ============================================
// ACTUALIZAR USUARIO - REQUEST
// ============================================
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

// ============================================
// CAMBIAR CONTRASEÑA - REQUEST & RESPONSE
// ============================================
data class CambiarContrasenaRequest(
    @SerializedName("contraseña_actual") val contrasenaActual: String,
    @SerializedName("contraseña_nueva") val contrasenaNueva: String
)

data class CambiarContrasenaResponse(
    val message: String
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

// ============================================
// MULTIMEDIA API MODELS
// ============================================
data class MultimediaResponse(
    val id: String,
    val url: String,
    val tipo: String, // "imagen" o "video"
    @SerializedName("fecha_subida") val fechaSubida: String,
    @SerializedName("id_publicacion") val idPublicacion: String
)

// ============================================
// PUBLICACIONES REQUEST MODELS
// ============================================
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

// ============================================
// SUCCESS RESPONSE MODELS
// ============================================
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

// ============================================
// REACCIONES API MODELS
// ============================================

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

// ============================================
// REQUEST MODELS PARA REACCIONES
// ============================================

// No necesitamos un request body específico porque la API usa Query params
// Los endpoints del backend usan:
// POST /api/reaccion/publicacion/{id_publicacion}?id_usuario=xxx&tipo_reaccion=like

// ============================================
// RESPONSE MODELS PARA REACCIONES
// ============================================

// Respuesta al crear/actualizar reacción
data class ReaccionResponse(
    @SerializedName("id_usuario") val idUsuario: String,
    @SerializedName("id_publicacion") val idPublicacion: String?,
    @SerializedName("id_comentario") val idComentario: String?,
    @SerializedName("tipo_reaccion") val tipoReaccion: String,  // "like" o "dislike"
    @SerializedName("publicacion_titulo") val publicacionTitulo: String?
) {
    // Helpers para facilitar el uso
    fun esLike(): Boolean = tipoReaccion == "like"
    fun esDislike(): Boolean = tipoReaccion == "dislike"

    // Compatible con TipoReaccion enum existente
    fun getTipoReaccionEnum(): TipoReaccion? = TipoReaccion.fromString(tipoReaccion)
}

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

// ============================================
// COMENTARIOS API MODELS
// ============================================

// Enum para estatus de comentarios (coincide con el backend)
enum class EstatusComentario(val value: String) {
    ACTIVO("activo"),
    ELIMINADO("eliminado"),
    OCULTO("oculto");

    companion object {
        fun fromString(value: String): EstatusComentario? {
            return values().find { it.value == value }
        }
    }
}

// ============================================
// REQUEST MODELS PARA COMENTARIOS
// ============================================

// Crear comentario en publicación (usando Query params como tu backend)
// POST /api/comentario/publicacion/{id_publicacion}?comentario=xxx&id_usuario=xxx

// Crear respuesta a comentario
// POST /api/comentario/respuesta/{id_comentario_padre}?comentario=xxx&id_usuario=xxx

// Request body para el método POST genérico
data class ComentarioCreateRequest(
    val comentario: String,
    @SerializedName("id_publicacion") val idPublicacion: String?, // Para comentario raíz
    @SerializedName("id_comentario") val idComentario: String?    // Para respuesta
) {
    // Validación básica
    init {
        require(comentario.isNotBlank()) { "El comentario no puede estar vacío" }
        require(idPublicacion != null || idComentario != null) {
            "Debe especificar id_publicacion o id_comentario"
        }
        require(!(idPublicacion != null && idComentario != null)) {
            "No puede especificar ambos id_publicacion e id_comentario"
        }
    }
}

// Request para actualizar comentario
data class ComentarioUpdateRequest(
    val comentario: String
) {
    init {
        require(comentario.isNotBlank()) { "El comentario no puede estar vacío" }
    }
}

// ============================================
// RESPONSE MODELS PARA COMENTARIOS
// ============================================

// Respuesta completa de comentario (coincide con tu backend)
data class ComentarioResponse(
    val id: String,
    val comentario: String,
    val estatus: String, // "activo", "eliminado", "oculto"
    @SerializedName("fecha_creacion") val fechaCreacion: String,
    @SerializedName("id_usuario") val idUsuario: String,
    @SerializedName("id_publicacion") val idPublicacion: String?, // null si es respuesta
    @SerializedName("id_comentario") val idComentario: String?,   // null si es comentario raíz

    // Información del usuario
    @SerializedName("usuario_alias") val usuarioAlias: String?,
    @SerializedName("usuario_foto") val usuarioFoto: String?,

    // Indicadores y estadísticas
    @SerializedName("es_respuesta") val esRespuesta: Boolean = false,
    @SerializedName("total_respuestas") val totalRespuestas: Int = 0,
    @SerializedName("total_reacciones") val totalReacciones: Int = 0
) {
    // Helpers para facilitar el uso
    fun esComentarioRaiz(): Boolean = idComentario == null
    fun esComentarioRespuesta(): Boolean = idComentario != null
    fun tieneRespuestas(): Boolean = totalRespuestas > 0
    fun getEstatusEnum(): EstatusComentario? = EstatusComentario.fromString(estatus)
}

// Respuesta simplificada para listas
data class ComentarioSimpleResponse(
    val id: String,
    val comentario: String,
    @SerializedName("fecha_creacion") val fechaCreacion: String,
    @SerializedName("usuario_alias") val usuarioAlias: String?,
    @SerializedName("total_respuestas") val totalRespuestas: Int = 0
)

// Respuesta para comentarios con respuestas anidadas
data class ComentarioConRespuestasResponse(
    val id: String,
    val comentario: String,
    val estatus: String,
    @SerializedName("fecha_creacion") val fechaCreacion: String,
    @SerializedName("id_usuario") val idUsuario: String,
    @SerializedName("id_publicacion") val idPublicacion: String?,

    // Información del usuario
    @SerializedName("usuario_alias") val usuarioAlias: String?,
    @SerializedName("usuario_foto") val usuarioFoto: String?,

    // Estadísticas
    @SerializedName("total_respuestas") val totalRespuestas: Int = 0,
    @SerializedName("total_reacciones") val totalReacciones: Int = 0,

    // Respuestas anidadas
    val respuestas: List<ComentarioResponse> = emptyList()
)

// ============================================
// SUCCESS RESPONSE MODELS PARA COMENTARIOS
// ============================================

data class ComentarioCreatedResponse(
    val message: String,
    val comentario: ComentarioResponse
)

data class ComentarioUpdatedResponse(
    val message: String,
    val comentario: ComentarioResponse
)

data class ComentarioDeletedResponse(
    val message: String
)

// ============================================
// FAVORITOS API MODELS
// ============================================

// Request para crear favorito (simple, solo necesita el id_publicacion)
data class FavoritoCreateRequest(
    @SerializedName("id_publicacion") val idPublicacion: String
)

// ============================================
// RESPONSE MODELS PARA FAVORITOS
// ============================================

// Respuesta al agregar/verificar favorito
data class FavoritoResponse(
    @SerializedName("id_usuario") val idUsuario: String,
    @SerializedName("id_publicacion") val idPublicacion: String,
    @SerializedName("fecha_guardado") val fechaGuardado: String,
    @SerializedName("publicacion_titulo") val publicacionTitulo: String?
) {
    // Helper para mostrar fecha formateada
    fun getFechaFormateada(): String {
        return try {
            if (fechaGuardado.contains("T")) {
                val partes = fechaGuardado.split("T")
                val fecha = partes[0] // "2025-11-23"
                val fechaParts = fecha.split("-")
                if (fechaParts.size == 3) {
                    "${fechaParts[2]}/${fechaParts[1]}/${fechaParts[0]}"
                } else {
                    "Fecha inválida"
                }
            } else {
                fechaGuardado
            }
        } catch (e: Exception) {
            "Fecha inválida"
        }
    }
}

// ============================================
// SUCCESS RESPONSE MODELS PARA FAVORITOS
// ============================================

data class FavoritoCreatedResponse(
    val message: String,
    val favorito: FavoritoResponse
)

data class FavoritoDeletedResponse(
    val message: String
)

// ============================================
// RESPONSE MODELS PARA REACCIONES EN COMENTARIOS
// ============================================

// Respuesta de conteo de reacciones de un comentario (similar a publicaciones)
data class ConteoReaccionesComentarioResponse(
    @SerializedName("id_comentario") val idComentario: String,
    val likes: Int,
    val dislikes: Int,
    val total: Int
)

// Respuesta para verificar reacción de usuario en comentario
data class VerificarReaccionComentarioResponse(
    @SerializedName("tiene_reaccion") val tieneReaccion: Boolean,
    @SerializedName("tipo_reaccion") val tipoReaccion: String? // "like", "dislike" o null
) {
    // Helpers para facilitar el uso (reutilizan la misma lógica)
    fun esLike(): Boolean = tipoReaccion == "like"
    fun esDislike(): Boolean = tipoReaccion == "dislike"
    fun getTipoReaccionEnum(): TipoReaccion? = tipoReaccion?.let { TipoReaccion.fromString(it) }
}

// ============================================
// MODELS PARA COMPATIBILIDAD CON Comment.kt EXISTENTE
// ============================================

/**
 * Extensiones para convertir entre modelos API y modelos locales
 */

// Convertir ComentarioResponse a Comment (para CommentAdapter existente)
fun ComentarioResponse.toComment(): Comment {
    return Comment(
        id = this.id.hashCode(), // Convertir UUID a Int
        apiId = this.id,  // Mantener el ID real de la API
        user = this.usuarioAlias ?: "Usuario",
        text = this.comentario,
        timestamp = formatearFechaComentario(this.fechaCreacion),
        userLikeState = Comment.LikeState.NONE, // Se cargará por separado
        replies = mutableListOf() // Las respuestas se cargarán por separado
    )
}

// Convertir lista de ComentarioResponse a Comment
fun List<ComentarioResponse>.toCommentList(): List<Comment> {
    return this.map { it.toComment() }
}

// Helper para formatear fechas de comentarios
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