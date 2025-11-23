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
     */
    fun loadProfileImage(
        context: Context,
        imageUrl: String?,
        circleImageView: CircleImageView,
        showPlaceholder: Boolean = true
    ) {
        android.util.Log.d(TAG, "Cargando imagen de perfil: $imageUrl")

        val glideRequest = Glide.with(context)
            .load(if (imageUrl.isNullOrEmpty()) null else imageUrl)
            .diskCacheStrategy(DiskCacheStrategy.ALL)

        if (showPlaceholder) {
            glideRequest
                .placeholder(R.mipmap.ic_foto_perfil_round) // Mientras carga
                .error(R.mipmap.ic_foto_perfil_round) // Si hay error
        }

        glideRequest.into(circleImageView)
    }

    /**
     * Carga imagen de publicación (recetas) con Glide
     */
    fun loadPostImage(
        context: Context,
        imageUrl: String?,
        imageView: ImageView,
        showPlaceholder: Boolean = true
    ) {
        android.util.Log.d(TAG, "Cargando imagen de post: $imageUrl")

        val glideRequest = Glide.with(context)
            .load(if (imageUrl.isNullOrEmpty()) null else imageUrl)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .centerCrop() // ✅ Para que se vea bien en las cards

        if (showPlaceholder) {
            glideRequest
                .placeholder(R.mipmap.ic_launcher) // Mientras carga
                .error(R.mipmap.ic_launcher) // Si hay error o no hay imagen
        }

        glideRequest.into(imageView)
    }

    /**
     * Carga imagen normal (genérica) con Glide
     */
    fun loadImage(
        context: Context,
        imageUrl: String?,
        imageView: ImageView,
        placeholderResId: Int = R.mipmap.ic_launcher
    ) {
        android.util.Log.d(TAG, "Cargando imagen genérica: $imageUrl")

        Glide.with(context)
            .load(if (imageUrl.isNullOrEmpty()) null else imageUrl)
            .placeholder(placeholderResId)
            .error(placeholderResId)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(imageView)
    }

    /**
     * Precargar imagen en caché para mejorar performance
     */
    fun preloadImage(context: Context, imageUrl: String?) {
        if (!imageUrl.isNullOrEmpty()) {
            Glide.with(context)
                .load(imageUrl)
                .preload()
        }
    }

    /**
     * Limpiar caché de Glide (útil para logout o cuando hay problemas)
     */
    fun clearCache(context: Context) {
        android.util.Log.d(TAG, "Limpiando caché de imágenes")
        Glide.get(context).clearMemory()
        Thread {
            Glide.get(context).clearDiskCache()
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
}