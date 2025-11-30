package com.example.piamoviles2

import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import java.io.File

data class MultimediaItem(
    val id: Long = System.currentTimeMillis(),
    val tipo: TipoMultimedia,
    val bitmap: Bitmap? = null,
    val videoUri: Uri? = null,
    var videoThumbnail: Bitmap? = null,
    var file: File? = null
) {
    /**
     * Enum para los tipos de multimedia soportados
     */
    enum class TipoMultimedia {
        IMAGEN,
        VIDEO
    }

    /**
     * Helpers para verificar el tipo de multimedia
     */
    fun esImagen(): Boolean = tipo == TipoMultimedia.IMAGEN
    fun esVideo(): Boolean = tipo == TipoMultimedia.VIDEO

    /**
     * Obtiene el emoji correspondiente al tipo
     */
    fun getEmoji(): String = when (tipo) {
        TipoMultimedia.IMAGEN -> "📷"
        TipoMultimedia.VIDEO -> "🎥"
    }

    /**
     * Verifica si el item tiene contenido listo para mostrar
     */
    fun tieneContenido(): Boolean = when (tipo) {
        TipoMultimedia.IMAGEN -> bitmap != null
        TipoMultimedia.VIDEO -> videoUri != null
    }

    /**
     * Limpia recursos y elimina archivos temporales
     */
    fun cleanup() {
        file?.let { tempFile ->
            try {
                if (tempFile.exists()) {
                    tempFile.delete()
                    Log.d("MultimediaItem", "Archivo eliminado: ${tempFile.name}")
                }
            } catch (e: Exception) {
                Log.w("MultimediaItem", "No se pudo eliminar archivo: ${tempFile.name}", e)
            }
        }

        // Liberar bitmaps
        bitmap?.recycle()
        videoThumbnail?.recycle()
    }

    companion object {
        /**
         * Crea un MultimediaItem de tipo IMAGEN
         */
        fun crearImagen(bitmap: Bitmap): MultimediaItem {
            return MultimediaItem(
                tipo = TipoMultimedia.IMAGEN,
                bitmap = bitmap
            )
        }

        /**
         * Crea un MultimediaItem de tipo VIDEO
         */
        fun crearVideo(uri: Uri, thumbnail: Bitmap? = null): MultimediaItem {
            return MultimediaItem(
                tipo = TipoMultimedia.VIDEO,
                videoUri = uri,
                videoThumbnail = thumbnail
            )
        }
    }
}