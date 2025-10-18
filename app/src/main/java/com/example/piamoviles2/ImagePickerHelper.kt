package com.example.piamoviles2

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

/**
 * Helper class para manejar la selección de imágenes desde cámara y galería
 */
class ImagePickerHelper(
    private val activity: AppCompatActivity,
    private val onImageSelected: (Bitmap?) -> Unit
) {

    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    private lateinit var galleryLauncher: ActivityResultLauncher<Intent>

    init {
        setupLaunchers()
    }

    private fun setupLaunchers() {
        // Launcher para cámara
        cameraLauncher = activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val imageBitmap = result.data?.extras?.get("data") as? Bitmap
                onImageSelected(imageBitmap)
            }
        }

        // Launcher para galería
        galleryLauncher = activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val imageUri: Uri? = result.data?.data
                imageUri?.let { uri ->
                    try {
                        val bitmap = MediaStore.Images.Media.getBitmap(
                            activity.contentResolver,
                            uri
                        )
                        onImageSelected(bitmap)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        onImageSelected(null)
                    }
                }
            }
        }
    }

    /**
     * Abrir cámara para tomar foto
     */
    fun openCamera() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(activity.packageManager) != null) {
            cameraLauncher.launch(takePictureIntent)
        }
    }

    /**
     * Abrir galería para seleccionar imagen
     */
    fun openGallery() {
        val pickPhotoIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(pickPhotoIntent)
    }

    companion object {
        /**
         * Redimensionar bitmap para optimizar memoria
         */
        fun resizeBitmap(bitmap: Bitmap, maxWidth: Int = 800, maxHeight: Int = 600): Bitmap {
            val width = bitmap.width
            val height = bitmap.height

            val ratioBitmap = width.toFloat() / height.toFloat()
            val ratioMax = maxWidth.toFloat() / maxHeight.toFloat()

            var finalWidth = maxWidth
            var finalHeight = maxHeight

            if (ratioMax > ratioBitmap) {
                finalWidth = (maxHeight.toFloat() * ratioBitmap).toInt()
            } else {
                finalHeight = (maxWidth.toFloat() / ratioBitmap).toInt()
            }

            return Bitmap.createScaledBitmap(bitmap, finalWidth, finalHeight, true)
        }
    }
}