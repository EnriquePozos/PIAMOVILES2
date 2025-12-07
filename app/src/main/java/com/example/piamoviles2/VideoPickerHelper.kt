package com.example.piamoviles2

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream

/**
 * Helper class para seleccionar videos desde la galería o grabar con la cámara
 * Similar a ImagePickerHelper pero para videos
 */
class VideoPickerHelper(
    private val activity: AppCompatActivity,
    private val onVideoSelected: (VideoResult?) -> Unit
) {

    companion object {
        private const val TAG = "VIDEO_PICKER_HELPER"
        private const val MAX_VIDEO_SIZE_MB = 10 // Límite de 100MB
        private const val MAX_VIDEO_DURATION_MS = 32400000L // 60 segundos
    }

    /**
     * Data class para el resultado del video
     */
    data class VideoResult(
        val uri: Uri,
        val thumbnail: Bitmap?,
        val file: File,
        val durationMs: Long,
        val sizeBytes: Long
    )

    // Launcher para galería
    private val galleryLauncher: ActivityResultLauncher<Intent> =
        activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    processVideo(uri)
                } ?: run {
                    android.util.Log.e(TAG, "URI del video es null")
                    onVideoSelected(null)
                }
            } else {
                android.util.Log.d(TAG, "Selección de video cancelada")
                onVideoSelected(null)
            }
        }

    // Launcher para cámara
    private var videoUri: Uri? = null
    private val cameraLauncher: ActivityResultLauncher<Intent> =
        activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                videoUri?.let { uri ->
                    processVideo(uri)
                } ?: run {
                    android.util.Log.e(TAG, "URI del video de cámara es null")
                    onVideoSelected(null)
                }
            } else {
                android.util.Log.d(TAG, "Grabación de video cancelada")
                onVideoSelected(null)
            }
        }

    /**
     * Abre la galería para seleccionar un video
     */
    fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI).apply {
            type = "video/*"
        }
        galleryLauncher.launch(intent)
        android.util.Log.d(TAG, "Abriendo galería para seleccionar video")
    }

    /**
     * Abre la cámara para grabar un video
     */
    fun openCamera() {
        try {
            // Crear archivo temporal para el video
            val tempFile = File(activity.cacheDir, "temp_video_${System.currentTimeMillis()}.mp4")

            // Usar FileProvider para crear URI seguro
            videoUri = androidx.core.content.FileProvider.getUriForFile(
                activity,
                "${activity.packageName}.fileprovider",
                tempFile
            )

            val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE).apply {
                // Limitar duración (opcional)
                putExtra(MediaStore.EXTRA_DURATION_LIMIT, 32400) // 60 segundos
                putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1) // 1 = alta calidad
                putExtra(MediaStore.EXTRA_OUTPUT, videoUri)

                // Dar permisos temporales a la app de cámara
                addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            // Verificar que hay una app de cámara disponible
            if (intent.resolveActivity(activity.packageManager) != null) {
                cameraLauncher.launch(intent)
                android.util.Log.d(TAG, "Abriendo cámara para grabar video")
            } else {
                android.util.Log.e(TAG, "No hay app de cámara disponible")
                android.widget.Toast.makeText(
                    activity,
                    "No se encontró aplicación de cámara",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }

        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error al abrir cámara", e)
            android.widget.Toast.makeText(
                activity,
                "Error al abrir cámara: ${e.message}",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Procesa el video: valida tamaño, genera thumbnail y copia a archivo temporal
     */
    private fun processVideo(uri: Uri) {
        try {
            android.util.Log.d(TAG, "=== Procesando video ===")
            android.util.Log.d(TAG, "URI: $uri")

            // Validar el video
            val validation = validateVideo(uri)
            if (!validation.isValid) {
                android.util.Log.e(TAG, "Video no válido: ${validation.errorMessage}")
                android.widget.Toast.makeText(
                    activity,
                    validation.errorMessage,
                    android.widget.Toast.LENGTH_LONG
                ).show()
                onVideoSelected(null)
                return
            }

            // Generar thumbnail
            val thumbnail = generateThumbnail(uri)

            // Copiar video a archivo temporal
            val videoFile = copyVideoToTempFile(uri)

            if (videoFile != null) {
                val result = VideoResult(
                    uri = uri,
                    thumbnail = thumbnail,
                    file = videoFile,
                    durationMs = validation.durationMs,
                    sizeBytes = validation.sizeBytes
                )

                android.util.Log.d(TAG, "  Video procesado exitosamente")
                android.util.Log.d(TAG, "Archivo: ${videoFile.name}, Tamaño: ${videoFile.length()} bytes")

                onVideoSelected(result)
            } else {
                android.util.Log.e(TAG, "Error al copiar video a archivo temporal")
                onVideoSelected(null)
            }

        } catch (e: Exception) {
            android.util.Log.e(TAG, "  Error al procesar video", e)
            android.widget.Toast.makeText(
                activity,
                "Error al procesar video: ${e.message}",
                android.widget.Toast.LENGTH_SHORT
            ).show()
            onVideoSelected(null)
        }
    }

    /**
     * Valida tamaño y duración del video
     */
    private fun validateVideo(uri: Uri): ValidationResult {
        val retriever = MediaMetadataRetriever()

        try {
            retriever.setDataSource(activity, uri)

            // Obtener duración
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = durationStr?.toLongOrNull() ?: 0L

            // Obtener tamaño del archivo
            val sizeBytes = activity.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                pfd.statSize
            } ?: 0L

            val sizeMB = sizeBytes / (1024.0 * 1024.0)

            android.util.Log.d(TAG, "Duración: ${durationMs}ms (${durationMs / 1000}s)")
            android.util.Log.d(TAG, "Tamaño: $sizeMB MB")

            // Validar duración
            if (durationMs > MAX_VIDEO_DURATION_MS) {
                return ValidationResult(
                    isValid = false,
                    errorMessage = "El video es muy largo. Máximo ${MAX_VIDEO_DURATION_MS / 1000} segundos.",
                    durationMs = durationMs,
                    sizeBytes = sizeBytes
                )
            }

            // Validar tamaño
            if (sizeMB > MAX_VIDEO_SIZE_MB) {
                return ValidationResult(
                    isValid = false,
                    errorMessage = "El video es muy pesado. Máximo ${MAX_VIDEO_SIZE_MB}MB.",
                    durationMs = durationMs,
                    sizeBytes = sizeBytes
                )
            }

            return ValidationResult(
                isValid = true,
                errorMessage = null,
                durationMs = durationMs,
                sizeBytes = sizeBytes
            )

        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error al validar video", e)
            return ValidationResult(
                isValid = false,
                errorMessage = "Error al validar video: ${e.message}",
                durationMs = 0,
                sizeBytes = 0
            )
        } finally {
            retriever.release()
        }
    }

    /**
     * Genera un thumbnail (miniatura) del video
     */
    private fun generateThumbnail(uri: Uri): Bitmap? {
        val retriever = MediaMetadataRetriever()

        return try {
            retriever.setDataSource(activity, uri)

            // Obtener frame en el segundo 1
            val thumbnail = retriever.getFrameAtTime(
                1000000, // 1 segundo en microsegundos
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC
            )

            if (thumbnail != null) {
                android.util.Log.d(TAG, "  Thumbnail generado: ${thumbnail.width}x${thumbnail.height}")
            } else {
                android.util.Log.w(TAG, "No se pudo generar thumbnail")
            }

            thumbnail

        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error al generar thumbnail", e)
            null
        } finally {
            retriever.release()
        }
    }

    /**
     * Copia el video a un archivo temporal en el cache
     */
    private fun copyVideoToTempFile(uri: Uri): File? {
        return try {
            val tempFile = File(activity.cacheDir, "video_${System.currentTimeMillis()}.mp4")

            activity.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }

            android.util.Log.d(TAG, "Video copiado a: ${tempFile.absolutePath}")
            tempFile

        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error al copiar video", e)
            null
        }
    }

    /**
     * Data class para resultado de validación
     */
    private data class ValidationResult(
        val isValid: Boolean,
        val errorMessage: String?,
        val durationMs: Long,
        val sizeBytes: Long
    )
}