package com.example.piamoviles2.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.piamoviles2.R
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.*

object FileUtils {

    private const val TAG = "FileUtils"
    private const val MAX_IMAGE_SIZE = 1024 * 1024 // 1MB

    // ============================================
    // CONVERTIR URI A FILE
    // ============================================
    fun uriToFile(context: Context, uri: Uri, fileName: String = "temp_image.jpg"): File? {
        return try {
            Log.d(TAG, "Convirtiendo URI a File: $uri")

            val inputStream = context.contentResolver.openInputStream(uri)
            val tempFile = File(context.cacheDir, fileName)

            inputStream?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            Log.d(TAG, "✅ Archivo creado: ${tempFile.absolutePath}, tamaño: ${tempFile.length()} bytes")
            tempFile

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al convertir URI a File", e)
            null
        }
    }

    // ============================================
    // CREAR MULTIPART BODY PART DESDE FILE
    // ============================================
    fun createMultipartBodyPart(file: File, paramName: String = "archivos"): MultipartBody.Part? {
        return try {
            Log.d(TAG, "Creando MultipartBody.Part para: ${file.name}")

            val requestFile = RequestBody.create("image/*".toMediaTypeOrNull(), file)
            val part = MultipartBody.Part.createFormData(paramName, file.name, requestFile)

            Log.d(TAG, "✅ MultipartBody.Part creado exitosamente")
            part

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al crear MultipartBody.Part", e)
            null
        }
    }

    // ============================================
    // CREAR LISTA DE MULTIPART BODY PARTS
    // ============================================
    fun createMultipartBodyParts(files: List<File>, paramName: String = "archivos"): List<MultipartBody.Part> {
        val parts = mutableListOf<MultipartBody.Part>()

        files.forEachIndexed { index, file ->
            try {
                Log.d(TAG, "Procesando archivo $index: ${file.name}")

                val requestFile = RequestBody.create("image/*".toMediaTypeOrNull(), file)
                val part = MultipartBody.Part.createFormData(paramName, "imagen_$index.jpg", requestFile)
                parts.add(part)

                Log.d(TAG, "✅ Archivo $index procesado correctamente")

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error al procesar archivo $index", e)
            }
        }

        Log.d(TAG, "Total de archivos procesados: ${parts.size}/${files.size}")
        return parts
    }

    // ============================================
    // COMPRIMIR IMAGEN SI ES NECESARIA
    // ============================================
    fun compressImageIfNeeded(context: Context, uri: Uri): File? {
        return try {
            Log.d(TAG, "Verificando si necesita compresión...")

            // Crear archivo temporal
            val tempFile = File(context.cacheDir, "compressed_${System.currentTimeMillis()}.jpg")

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                // Leer la imagen
                val bitmap = BitmapFactory.decodeStream(inputStream)

                if (bitmap != null) {
                    // Calcular tamaño actual
                    val baos = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
                    val originalSize = baos.size()

                    Log.d(TAG, "Tamaño original: ${originalSize} bytes")

                    // Si es mayor a 1MB, comprimir
                    val quality = if (originalSize > MAX_IMAGE_SIZE) {
                        val ratio = MAX_IMAGE_SIZE.toFloat() / originalSize.toFloat()
                        (ratio * 100).toInt().coerceIn(10, 90)
                    } else {
                        90 // Calidad alta si no necesita compresión
                    }

                    Log.d(TAG, "Comprimiendo con calidad: $quality")

                    // Comprimir y guardar
                    tempFile.outputStream().use { output ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)
                    }

                    Log.d(TAG, "✅ Imagen comprimida: ${tempFile.length()} bytes")
                    tempFile
                } else {
                    Log.e(TAG, "❌ No se pudo decodificar la imagen")
                    null
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al comprimir imagen", e)
            null
        }
    }

    // ============================================
    // VALIDAR ARCHIVO DE IMAGEN
    // ============================================
    fun isValidImageFile(file: File): Boolean {
        return try {
            // Verificar que existe
            if (!file.exists()) {
                Log.w(TAG, "Archivo no existe: ${file.absolutePath}")
                return false
            }

            // Verificar tamaño
            if (file.length() == 0L) {
                Log.w(TAG, "Archivo está vacío: ${file.name}")
                return false
            }

            // Verificar que sea una imagen válida
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            val isValid = bitmap != null

            Log.d(TAG, "Archivo ${file.name} es válido: $isValid")
            isValid

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al validar archivo", e)
            false
        }
    }

    // ============================================
    // LIMPIAR ARCHIVOS TEMPORALES
    // ============================================
    fun cleanTempFiles(context: Context) {
        try {
            Log.d(TAG, "Limpiando archivos temporales...")

            val cacheDir = context.cacheDir
            val tempFiles = cacheDir.listFiles { file ->
                file.name.startsWith("temp_") || file.name.startsWith("compressed_")
            }

            tempFiles?.forEach { file ->
                if (file.delete()) {
                    Log.d(TAG, "Archivo temporal eliminado: ${file.name}")
                }
            }

            Log.d(TAG, "✅ Limpieza de archivos temporales completada")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al limpiar archivos temporales", e)
        }
    }

    // ============================================
    // OBTENER EXTENSIÓN DE ARCHIVO
    // ============================================
    fun getFileExtension(fileName: String): String {
        return fileName.substringAfterLast(".", "")
    }

    // ============================================
    // VALIDAR EXTENSIONES PERMITIDAS
    // ============================================
    fun isImageExtensionValid(fileName: String): Boolean {
        val validExtensions = listOf("jpg", "jpeg", "png", "webp")
        val extension = getFileExtension(fileName).lowercase()
        val isValid = extension in validExtensions

        Log.d(TAG, "Extensión '$extension' es válida: $isValid")
        return isValid
    }

    // ============================================
    // CREAR ARCHIVO TEMPORAL CON NOMBRE ÚNICO
    // ============================================
    fun createTempImageFile(context: Context): File {
        val timestamp = System.currentTimeMillis()
        val fileName = "temp_image_$timestamp.jpg"
        return File(context.cacheDir, fileName)
    }

    // ============================================
    // OBTENER TAMAÑO LEGIBLE DE ARCHIVO
    // ============================================
    fun getReadableFileSize(sizeInBytes: Long): String {
        return when {
            sizeInBytes >= 1024 * 1024 -> "%.2f MB".format(sizeInBytes / (1024.0 * 1024.0))
            sizeInBytes >= 1024 -> "%.2f KB".format(sizeInBytes / 1024.0)
            else -> "$sizeInBytes bytes"
        }
    }

}
