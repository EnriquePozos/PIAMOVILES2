package com.example.piamoviles2

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.piamoviles2.databinding.ItemDraftBinding

/**
 * Adaptador especializado para borradores con funcionalidades de editar y eliminar
 */
class DraftAdapter(
    private val onEditDraft: (Post) -> Unit,
    private val onDeleteDraft: (Post) -> Unit,
    private val onDraftClick: (Post) -> Unit
) : ListAdapter<Post, DraftAdapter.DraftViewHolder>(DraftDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DraftViewHolder {
        val binding = ItemDraftBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DraftViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DraftViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class DraftViewHolder(
        private val binding: ItemDraftBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(draft: Post) {
            // Configurar datos del borrador
            binding.tvDraftTitle.text = draft.title

            // Mostrar descripción completa o truncada
            val description = if (draft.description.length > 100) {
                draft.description.take(97) + "..."
            } else {
                draft.description
            }
            binding.tvDraftDescription.text = description

            // Mostrar fecha de guardado
            binding.tvDraftDate.text = draft.createdAt

            // Configurar imagen del borrador (con menor opacidad)
            // Por ahora usamos placeholders, después puedes usar Glide para imágenes reales
            when (draft.imageUrl) {
                "draft_pozole" -> binding.ivDraftImage.setImageResource(R.mipmap.ic_launcher)
                "draft_chiles" -> binding.ivDraftImage.setImageResource(R.mipmap.ic_launcher)
                "draft_mole" -> binding.ivDraftImage.setImageResource(R.mipmap.ic_launcher)
                else -> binding.ivDraftImage.setImageResource(R.mipmap.ic_launcher)
            }

            // Click listeners para botones
            binding.btnEditDraft.setOnClickListener {
                onEditDraft(draft)
            }

            binding.btnDeleteDraft.setOnClickListener {
                onDeleteDraft(draft)
            }

            // Click listener para toda la card (también edita)
            binding.root.setOnClickListener {
                onDraftClick(draft)
            }
        }
    }

    /**
     * DiffCallback para optimizar las actualizaciones del RecyclerView
     */
    private class DraftDiffCallback : DiffUtil.ItemCallback<Post>() {
        override fun areItemsTheSame(oldItem: Post, newItem: Post): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Post, newItem: Post): Boolean {
            return oldItem == newItem
        }
    }
}