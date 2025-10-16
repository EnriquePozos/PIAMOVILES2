package com.example.piamoviles2

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.piamoviles2.databinding.ItemPostBinding

/**
 * Adaptador para la lista de publicaciones/recetas - Versión simplificada
 */
class PostAdapter(
    private val onPostClick: (Post) -> Unit
) : ListAdapter<Post, PostAdapter.PostViewHolder>(PostDiffCallback()) {

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
            // Configurar datos básicos de la publicación
            binding.tvPostTitle.text = post.title

            // Limitar descripción a 50 caracteres máximo
            val shortDescription = if (post.description.length > 50) {
                post.description.take(47) + "..."
            } else {
                post.description
            }
            binding.tvPostDescription.text = shortDescription

            // Configurar imagen de la publicación
            // Por ahora usamos placeholders, después puedes usar Glide para imágenes reales
            when (post.imageUrl) {
                "sample_tacos" -> binding.ivPostImage.setImageResource(R.mipmap.ic_launcher)
                "sample_sandwich" -> binding.ivPostImage.setImageResource(R.mipmap.ic_launcher)
                "sample_salad" -> binding.ivPostImage.setImageResource(R.mipmap.ic_launcher)
                "sample_pasta" -> binding.ivPostImage.setImageResource(R.mipmap.ic_launcher)
                else -> binding.ivPostImage.setImageResource(R.mipmap.ic_launcher)
            }

            // Click listener para toda la card
            binding.root.setOnClickListener {
                onPostClick(post)
            }
        }
    }

    /**
     * DiffCallback para optimizar las actualizaciones del RecyclerView
     */
    private class PostDiffCallback : DiffUtil.ItemCallback<Post>() {
        override fun areItemsTheSame(oldItem: Post, newItem: Post): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Post, newItem: Post): Boolean {
            return oldItem == newItem
        }
    }
}