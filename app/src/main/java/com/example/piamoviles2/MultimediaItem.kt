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
    var file: File? = null,

    // NUEVO: Flag para indicar si el archivo es persistente (no se debe eliminar)
    var isPersistent: Boolean = false
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
     * MODIFICADO: Solo elimina archivos si NO son persistentes
     */
    fun cleanup() {
        file?.let { tempFile ->
            try {
                // VALIDAR: Solo eliminar si NO es persistente
                if (!isPersistent) {
                    if (tempFile.exists()) {
                        val deleted = tempFile.delete()
                        if (deleted) {
                            Log.d("MultimediaItem", "Archivo temporal eliminado: ${tempFile.name}")
                        } else {
                            Log.w("MultimediaItem", "No se pudo eliminar archivo temporal: ${tempFile.name}")
                        }
                    }
                } else {
                    Log.d("MultimediaItem", "Archivo persistente PRESERVADO: ${tempFile.name}")
                }
            } catch (e: Exception) {
                Log.w("MultimediaItem", "Error al procesar archivo: ${tempFile.name}", e)
            }
        }

        // Liberar bitmaps (esto siempre se hace)
        bitmap?.recycle()
        videoThumbnail?.recycle()
    }

    companion object {
        /**
         * Crea un MultimediaItem de tipo IMAGEN
         * @param bitmap Bitmap de la imagen
         * @param isPersistent Si es true, el archivo NO se eliminará al hacer cleanup
         */
        fun crearImagen(bitmap: Bitmap, isPersistent: Boolean = false): MultimediaItem {
            return MultimediaItem(
                tipo = TipoMultimedia.IMAGEN,
                bitmap = bitmap,
                isPersistent = isPersistent
            )
        }

        /**
         * Crea un MultimediaItem de tipo VIDEO
         * @param uri Uri del video
         * @param thumbnail Thumbnail opcional del video
         * @param isPersistent Si es true, el archivo NO se eliminará al hacer cleanup
         */
        fun crearVideo(uri: Uri, thumbnail: Bitmap? = null, isPersistent: Boolean = false): MultimediaItem {
            return MultimediaItem(
                tipo = TipoMultimedia.VIDEO,
                videoUri = uri,
                videoThumbnail = thumbnail,
                isPersistent = isPersistent
            )
        }
    }
}