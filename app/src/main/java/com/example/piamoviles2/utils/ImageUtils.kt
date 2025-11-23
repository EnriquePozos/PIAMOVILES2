package com.example.piamoviles2.utils

import android.content.Context
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.piamoviles2.R
import de.hdodenhof.circleimageview.CircleImageView

object ImageUtils {

    /**
     * Carga imagen circular de perfil con Glide + CircleImageView
     */
    fun loadProfileImage(
        context: Context,
        imageUrl: String?,
        circleImageView: CircleImageView,
        showPlaceholder: Boolean = true
    ) {
        val glideRequest = Glide.with(context)
            .load(if (imageUrl.isNullOrEmpty()) null else imageUrl)
            .diskCacheStrategy(DiskCacheStrategy.ALL)

        if (showPlaceholder) {
            glideRequest
                .placeholder(R.mipmap.ic_foto_perfil_round) // Mientras carga
                .error(R.mipmap.ic_foto_perfil_round) // Si hay error
        }

        glideRequest.into(circleImageView)

        android.util.Log.d("IMAGE_UTILS", "Cargando imagen: $imageUrl")
    }

    /**
     * Carga imagen normal (no circular) con Glide
     */
    fun loadImage(
        context: Context,
        imageUrl: String?,
        imageView: ImageView,
        placeholderResId: Int = R.drawable.ic_add_photo
    ) {
        Glide.with(context)
            .load(if (imageUrl.isNullOrEmpty()) null else imageUrl)
            .placeholder(placeholderResId)
            .error(placeholderResId)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(imageView)

        android.util.Log.d("IMAGE_UTILS", "Cargando imagen normal: $imageUrl")
    }

    /**
     * Limpiar caché de Glide (útil para logout)
     */
    fun clearCache(context: Context) {
        Glide.get(context).clearMemory()
        Thread {
            Glide.get(context).clearDiskCache()
        }.start()
    }
}