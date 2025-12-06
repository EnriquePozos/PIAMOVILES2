package com.example.piamoviles2

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.piamoviles2.databinding.ItemPostBinding
import com.example.piamoviles2.utils.ImageUtils

/**
 * Adaptador para la lista de publicaciones/recetas
 * CON soporte completo para imágenes online (API) y offline (SQLite)
 */
class PostAdapter(
    private val onPostClick: (Post) -> Unit
) : ListAdapter<Post, PostAdapter.PostViewHolder>(PostDiffCallback()) {

    companion object {
        private const val TAG = "POST_ADAPTER"
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = ItemPostBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PostViewHolder(
        private val binding: ItemPostBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(post: Post) {
            android.util.Log.d(TAG, "=== Binding Post ===")
            android.util.Log.d(TAG, "Título: ${post.title}")
            android.util.Log.d(TAG, "Image URL: ${post.imageUrl}")
            android.util.Log.d(TAG, "API ID: ${post.apiId}")
            android.util.Log.d(TAG, "Es Draft: ${post.isDraft}")

            // ============================================
            // CONFIGURAR DATOS BÁSICOS
            // ============================================
            binding.tvPostTitle.text = post.title

            // Limitar descripción a 50 caracteres máximo
            val shortDescription = if (post.description.length > 50) {
                post.description.take(47) + "..."
            } else {
                post.description
            }
            binding.tvPostDescription.text = shortDescription

            // ============================================
            // CARGAR IMAGEN - SOPORTE COMPLETO OFFLINE/ONLINE
            // ============================================
            when {
                // 1. ARCHIVOS LOCALES (modo offline)
                post.imageUrl.startsWith("file://") || ImageUtils.isLocalImagePath(post.imageUrl) -> {
                    android.util.Log.d(TAG, "📱 Cargando imagen LOCAL: ${post.imageUrl}")
                    ImageUtils.loadLocalImage(
                        context = binding.root.context,
                        localPath = post.imageUrl,
                        imageView = binding.ivPostImage,
                        showPlaceholder = true
                    )
                }

                // 2. URLs REMOTAS (modo online - API/Cloudinary)
                ImageUtils.isValidImageUrl(post.imageUrl) -> {
                    android.util.Log.d(TAG, "🌐 Cargando imagen REMOTA: ${post.imageUrl}")
                    ImageUtils.loadPostImage(
                        context = binding.root.context,
                        imageUrl = post.imageUrl,
                        imageView = binding.ivPostImage,
                        showPlaceholder = true
                    )
                }

                // 3. PLACEHOLDERS para mock data o fallback
                else -> {
                    android.util.Log.d(TAG, "🖼️ Usando PLACEHOLDER para: ${post.imageUrl}")
                    loadPlaceholderImage(post.imageUrl)
                }
            }

            // ============================================
            // DATOS ADICIONALES DE LA PUBLICACIÓN
            // ============================================
            // TODO: Descomentar cuando tengas estos elementos en tu layout
            // binding.tvPostAuthor.text = post.author
            // binding.tvPostDate.text = post.createdAt
            // binding.tvLikesCount.text = "${post.likesCount} likes"
            // binding.tvCommentsCount.text = "${post.commentsCount} comentarios"

            // ============================================
            // CLICK LISTENER
            // ============================================
            binding.root.setOnClickListener {
                android.util.Log.d(TAG, "Click en post: ${post.title}")
                android.util.Log.d(TAG, "Post ID: ${post.id}, API ID: ${post.apiId}")
                onPostClick(post)
            }

            // ============================================
            // INDICADORES VISUALES (OPCIONAL)
            // ============================================

            // Si es borrador, mostrar indicador (descomentar cuando tengas el elemento)
            if (post.isDraft) {
                // binding.tvDraftIndicator.visibility = View.VISIBLE
                // binding.tvDraftIndicator.text = "BORRADOR"
                android.util.Log.d(TAG, "📝 Post es borrador")
            }

            // Si es del usuario actual, mostrar indicador (descomentar cuando tengas el elemento)
            if (post.isOwner) {
                // binding.ivOwnerIndicator.visibility = View.VISIBLE
                android.util.Log.d(TAG, "👤 Post es del usuario actual")
            }

            // Si está marcado como favorito
            if (post.isFavorite) {
                // binding.ivFavoriteIndicator.visibility = View.VISIBLE
                android.util.Log.d(TAG, "⭐ Post es favorito")
            }
        }

        /**
         * Cargar imagen placeholder para mock data o cuando no hay imagen
         */
        private fun loadPlaceholderImage(imageUrl: String) {
            val placeholderResId = when (imageUrl) {
                "sample_tacos" -> R.mipmap.ic_launcher
                "sample_sandwich" -> R.mipmap.ic_launcher
                "sample_salad" -> R.mipmap.ic_launcher
                "sample_pasta" -> R.mipmap.ic_launcher
                "default_recipe" -> R.mipmap.ic_launcher
                else -> R.mipmap.ic_launcher
            }

            binding.ivPostImage.setImageResource(placeholderResId)
            android.util.Log.d(TAG, "✅ Placeholder aplicado: $imageUrl -> $placeholderResId")
        }
    }

    /**
     * DiffCallback optimizado para actualizaciones del RecyclerView
     */
    private class PostDiffCallback : DiffUtil.ItemCallback<Post>() {
        override fun areItemsTheSame(oldItem: Post, newItem: Post): Boolean {
            // Comparar por ID local Y API ID para mayor precisión
            return oldItem.id == newItem.id && oldItem.apiId == newItem.apiId
        }

        override fun areContentsTheSame(oldItem: Post, newItem: Post): Boolean {
            return oldItem == newItem
        }
    }

    /**
     * Método para precargar imágenes (mejora performance)
     * ACTUALIZADO para soportar imágenes locales y remotas
     */
    fun preloadImages() {
        currentList.forEach { post ->
            android.util.Log.d(TAG, "Precargando imagen para: ${post.title}")

            when {
                // Precargar imágenes remotas
                ImageUtils.isValidImageUrl(post.imageUrl) -> {
                    android.util.Log.d(TAG, "  Precargando imagen remota: ${post.imageUrl}")
                    // ImageUtils.preloadImage(binding.root.context, post.imageUrl)
                }

                // Precargar imágenes locales (no necesario, ya están en disco)
                post.imageUrl.startsWith("file://") || ImageUtils.isLocalImagePath(post.imageUrl) -> {
                    android.util.Log.d(TAG, "  Imagen local no requiere precarga: ${post.imageUrl}")
                }
            }
        }
    }

    /**
     * Método para debugging - mostrar estado de todas las publicaciones
     */
    fun logCurrentPosts() {
        android.util.Log.d(TAG, "=== ESTADO ACTUAL DEL ADAPTER ===")
        android.util.Log.d(TAG, "Total posts: ${currentList.size}")

        currentList.forEachIndexed { index, post ->
            android.util.Log.d(TAG, "[$index] ${post.title}")
            android.util.Log.d(TAG, "      ID: ${post.id}, API ID: ${post.apiId}")
            android.util.Log.d(TAG, "      Imagen: ${post.imageUrl}")
            android.util.Log.d(TAG, "      Borrador: ${post.isDraft}, Favorito: ${post.isFavorite}")
        }
        android.util.Log.d(TAG, "=====================================")
    }
}