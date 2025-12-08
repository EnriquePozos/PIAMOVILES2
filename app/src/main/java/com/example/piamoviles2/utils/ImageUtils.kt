package com.example.piamoviles2.utils

import android.content.Context
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.example.piamoviles2.R
import de.hdodenhof.circleimageview.CircleImageView

object ImageUtils {

    private const val TAG = "IMAGE_UTILS"

    /**
     * Carga imagen circular de perfil con Glide + CircleImageView
     * FIX: Previene "Cannot pool recycled bitmap" error
     */
    fun loadProfileImage(
        context: Context,
        imageUrl: String?,
        circleImageView: CircleImageView,
        showPlaceholder: Boolean = true
    ) {
        android.util.Log.d(TAG, "Cargando imagen de perfil: $imageUrl")

        // ============================================
        // FIX: Usar SIEMPRE applicationContext
        // ============================================
        val appContext = context.applicationContext

        // Limpiar ImageView antes de cargar
        try {
            Glide.with(appContext).clear(circleImageView)
        } catch (e: Exception) {
            android.util.Log.w(TAG, "No se pudo limpiar ImageView: ${e.message}")
        }

        // Configuración robusta de Glide
        val glideRequest = Glide.with(appContext)
            .load(if (imageUrl.isNullOrEmpty()) null else imageUrl)
            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
            .dontAnimate()
            .skipMemoryCache(false)

        if (showPlaceholder) {
            glideRequest
                .placeholder(R.mipmap.ic_foto_perfil_round)
                .error(R.mipmap.ic_foto_perfil_round)
        }

        glideRequest.into(circleImageView)
        android.util.Log.d(TAG, "✅ Imagen de perfil cargada")
    }

    /**
     * Carga imagen de publicación (recetas) con Glide
     * FIX: Previene "Cannot pool recycled bitmap" error
     */
    fun loadPostImage(
        context: Context,
        imageUrl: String?,
        imageView: ImageView,
        showPlaceholder: Boolean = true
    ) {
        android.util.Log.d(TAG, "Cargando imagen de post: $imageUrl")

        // ============================================
        // FIX: Usar SIEMPRE applicationContext
        // ============================================
        val appContext = context.applicationContext

        // Limpiar ImageView antes de cargar
        try {
            Glide.with(appContext).clear(imageView)
        } catch (e: Exception) {
            android.util.Log.w(TAG, "No se pudo limpiar ImageView: ${e.message}")
        }

        val glideRequest = Glide.with(appContext)
            .load(if (imageUrl.isNullOrEmpty()) null else imageUrl)
            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
            .dontAnimate()
            .centerCrop()

        if (showPlaceholder) {
            glideRequest
                .placeholder(R.mipmap.ic_launcher)
                .error(R.mipmap.ic_launcher)
        }

        glideRequest.into(imageView)
        android.util.Log.d(TAG, "✅ Imagen de post cargada")
    }

    /**
     * Carga imagen normal (genérica) con Glide
     * FIX: Previene "Cannot pool recycled bitmap" error
     */
    fun loadImage(
        context: Context,
        imageUrl: String?,
        imageView: ImageView,
        placeholderResId: Int = R.mipmap.ic_launcher
    ) {
        android.util.Log.d(TAG, "Cargando imagen genérica: $imageUrl")

        // ============================================
        // FIX: Usar SIEMPRE applicationContext
        // ============================================
        val appContext = context.applicationContext

        // Limpiar ImageView antes de cargar
        try {
            Glide.with(appContext).clear(imageView)
        } catch (e: Exception) {
            android.util.Log.w(TAG, "No se pudo limpiar ImageView: ${e.message}")
        }

        Glide.with(appContext)
            .load(if (imageUrl.isNullOrEmpty()) null else imageUrl)
            .placeholder(placeholderResId)
            .error(placeholderResId)
            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
            .dontAnimate()
            .into(imageView)

        android.util.Log.d(TAG, "✅ Imagen genérica cargada")
    }

    /**
     * Precargar imagen en caché para mejorar performance
     */
    fun preloadImage(context: Context, imageUrl: String?) {
        if (!imageUrl.isNullOrEmpty()) {
            Glide.with(context.applicationContext)
                .load(imageUrl)
                .preload()
        }
    }

    /**
     * Limpiar caché de Glide (útil para logout o cuando hay problemas)
     */
    fun clearCache(context: Context) {
        android.util.Log.d(TAG, "Limpiando caché de imágenes")
        val appContext = context.applicationContext
        Glide.get(appContext).clearMemory()
        Thread {
            Glide.get(appContext).clearDiskCache()
        }.start()
    }

    /**
     * Verificar si una URL de imagen es válida
     */
    fun isValidImageUrl(imageUrl: String?): Boolean {
        return !imageUrl.isNullOrEmpty() &&
                (imageUrl.startsWith("http://") ||
                        imageUrl.startsWith("https://") ||
                        imageUrl.startsWith("res.cloudinary.com"))
    }

    /**
     * Carga imagen desde archivo local (modo offline)
     * Maneja archivos del sistema de archivos con prefijo "file://"
     */
    fun loadLocalImage(
        context: Context,
        localPath: String,
        imageView: ImageView,
        showPlaceholder: Boolean = true
    ) {
        android.util.Log.d(TAG, "Cargando imagen local: $localPath")

        // ============================================
        // FIX: Usar SIEMPRE applicationContext
        // ============================================
        val appContext = context.applicationContext

        try {
            // Limpiar ImageView antes de cargar
            try {
                Glide.with(appContext).clear(imageView)
            } catch (e: Exception) {
                android.util.Log.w(TAG, "No se pudo limpiar ImageView: ${e.message}")
            }

            // Remover prefijo "file://" si existe
            val cleanPath = localPath.removePrefix("file://")
            val file = java.io.File(cleanPath)

            val glideRequest = Glide.with(appContext)
                .load(file)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .dontAnimate()
                .centerCrop()

            if (showPlaceholder) {
                glideRequest
                    .placeholder(R.mipmap.ic_launcher)
                    .error(R.mipmap.ic_launcher)
            }

            glideRequest.into(imageView)
            android.util.Log.d(TAG, "✅ Imagen local cargada")

        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Error cargando imagen local: ${e.message}")
            if (showPlaceholder) {
                imageView.setImageResource(R.mipmap.ic_launcher)
            }
        }
    }

    /**
     * Verificar si es una ruta de archivo local
     */
    fun isLocalImagePath(imagePath: String?): Boolean {
        return !imagePath.isNullOrEmpty() &&
                (imagePath.startsWith("file://") || imagePath.startsWith("/"))
    }
}