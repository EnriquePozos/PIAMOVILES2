package com.example.piamoviles2

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.piamoviles2.databinding.ItemPostBinding
import com.example.piamoviles2.utils.ImageUtils

/**
 * Adaptador para la lista de publicaciones/recetas con imágenes reales de la API
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
            android.util.Log.d(TAG, "Binding post: ${post.title}")
            android.util.Log.d(TAG, "Image URL: ${post.imageUrl}")

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
            // ✅ CARGAR IMAGEN REAL CON GLIDE
            // ============================================
            if (ImageUtils.isValidImageUrl(post.imageUrl)) {
                // Cargar imagen real de la API (URL de Cloudinary)
                ImageUtils.loadPostImage(
                    context = binding.root.context,
                    imageUrl = post.imageUrl,
                    imageView = binding.ivPostImage,
                    showPlaceholder = true
                )
                android.util.Log.d(TAG, "✅ Cargando imagen de URL: ${post.imageUrl}")
            } else {
                // ============================================
                // FALLBACK: IMÁGENES LOCALES PARA DATOS DE EJEMPLO
                // ============================================
                val placeholderResId = when (post.imageUrl) {
                    "sample_tacos" -> R.mipmap.ic_launcher
                    "sample_sandwich" -> R.mipmap.ic_launcher
                    "sample_salad" -> R.mipmap.ic_launcher
                    "sample_pasta" -> R.mipmap.ic_launcher
                    "default_recipe" -> R.mipmap.ic_launcher
                    else -> R.mipmap.ic_launcher
                }

                binding.ivPostImage.setImageResource(placeholderResId)
                android.util.Log.d(TAG, "📱 Usando placeholder para: ${post.imageUrl}")
            }

            // ============================================
            // DATOS ADICIONALES DE LA PUBLICACIÓN
            // ============================================

            // TODO: Agregar autor, fecha, likes, comentarios si están en tu layout
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

            // Si es borrador, mostrar indicador
            if (post.isDraft) {
                // binding.tvDraftIndicator.visibility = View.VISIBLE
            }

            // Si es del usuario actual, mostrar indicador
            if (post.isOwner) {
                // binding.ivOwnerIndicator.visibility = View.VISIBLE
            }
        }
    }

    /**
     * DiffCallback optimizado para actualizaciones del RecyclerView
     */
    private class PostDiffCallback : DiffUtil.ItemCallback<Post>() {
        override fun areItemsTheSame(oldItem: Post, newItem: Post): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Post, newItem: Post): Boolean {
            return oldItem == newItem
        }
    }

    /**
     * Método para precargar imágenes (mejora performance)
     */
    fun preloadImages() {
        currentList.forEach { post ->
            if (ImageUtils.isValidImageUrl(post.imageUrl)) {
                // Obtener contexto desde el RecyclerView
                if (itemCount > 0) {
                    // Precarga las primeras imágenes
                }
            }
        }
    }
}