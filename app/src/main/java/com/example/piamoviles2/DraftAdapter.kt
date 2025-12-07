package com.example.piamoviles2

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.piamoviles2.databinding.ItemDraftBinding
import com.example.piamoviles2.utils.ImageUtils

/**
 * Adaptador especializado para borradores con funcionalidades de editar y eliminar
 */
class DraftAdapter(
    private val onEditDraft: (Post) -> Unit,
    private val onDeleteDraft: (Post) -> Unit,
    private val onDraftClick: (Post) -> Unit
) : ListAdapter<Post, DraftAdapter.DraftViewHolder>(DraftDiffCallback()) {

    companion object {
        private const val TAG = "DRAFT_ADAPTER"
    }

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
            android.util.Log.d(TAG, "Binding draft: ${draft.title}")
            android.util.Log.d(TAG, "Draft Image URL: ${draft.imageUrl}")
            android.util.Log.d(TAG, "Draft API ID: ${draft.apiId}")
            android.util.Log.d(TAG, "Draft isSynced: ${draft.isSynced}")

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

            // ============================================
            // CARGAR IMAGEN - SOPORTE COMPLETO OFFLINE/ONLINE
            // ============================================
            when {
                // 1. ARCHIVOS LOCALES (modo offline)
                draft.imageUrl.startsWith("file://") || ImageUtils.isLocalImagePath(draft.imageUrl) -> {
                    android.util.Log.d(TAG, "Cargando imagen LOCAL: ${draft.imageUrl}")
                    ImageUtils.loadLocalImage(
                        context = binding.root.context,
                        localPath = draft.imageUrl,
                        imageView = binding.ivDraftImage,
                        showPlaceholder = true
                    )
                }

                // 2. URLs REMOTAS (modo online - API/Cloudinary)
                ImageUtils.isValidImageUrl(draft.imageUrl) -> {
                    android.util.Log.d(TAG, "Cargando imagen REMOTA: ${draft.imageUrl}")
                    ImageUtils.loadPostImage(
                        context = binding.root.context,
                        imageUrl = draft.imageUrl,
                        imageView = binding.ivDraftImage,
                        showPlaceholder = true
                    )
                }

                // 3. PLACEHOLDERS para mock data o fallback
                else -> {
                    android.util.Log.d(TAG, "Usando PLACEHOLDER para: ${draft.imageUrl}")
                    loadPlaceholderImage(draft.imageUrl)
                }
            }

            // ============================================
            // ESTILO VISUAL PARA BORRADORES
            // ============================================
            // Aplicar menor opacidad para indicar que es borrador
            binding.ivDraftImage.alpha = 0.8f

            // ============================================
            // CLICK LISTENERS PARA BOTONES
            // ============================================
            binding.btnEditDraft.setOnClickListener {
                android.util.Log.d(TAG, "Editando draft: ${draft.title}")
                onEditDraft(draft)
            }

            binding.btnDeleteDraft.setOnClickListener {
                android.util.Log.d(TAG, "Eliminando draft: ${draft.title}")
                onDeleteDraft(draft)
            }

            // Click listener para toda la card (también edita)
            binding.root.setOnClickListener {
                android.util.Log.d(TAG, "Click en draft: ${draft.title}")
                onDraftClick(draft)
            }

            // ============================================
            // INDICADOR VISUAL PARA BORRADORES SINCRONIZADOS
            // ============================================
            if (draft.isSynced) {
                // Mostrar indicador de sincronización
                binding.btnEditDraft.alpha = 1.0f
                binding.btnDeleteDraft.alpha = 1.0f

                android.util.Log.d(TAG, "Borrador sincronizado: ${draft.title}")
            } else {
                // Borrador local no sincronizado
                binding.btnEditDraft.alpha = 1.0f
                binding.btnDeleteDraft.alpha = 1.0f

                android.util.Log.d(TAG, "Borrador local: ${draft.title}")
            }
        }

        /**
         * Cargar imagen placeholder para mock data o cuando no hay imagen
         */
        private fun loadPlaceholderImage(imageUrl: String) {
            val placeholderResId = when (imageUrl) {
                "draft_pozole" -> R.mipmap.ic_launcher
                "draft_chiles" -> R.mipmap.ic_launcher
                "draft_mole" -> R.mipmap.ic_launcher
                "sample_tacos" -> R.mipmap.ic_launcher
                "sample_sandwich" -> R.mipmap.ic_launcher
                "sample_salad" -> R.mipmap.ic_launcher
                "sample_pasta" -> R.mipmap.ic_launcher
                "default_recipe" -> R.mipmap.ic_launcher
                else -> R.mipmap.ic_launcher
            }

            binding.ivDraftImage.setImageResource(placeholderResId)
            android.util.Log.d(TAG, "Placeholder aplicado: $imageUrl -> $placeholderResId")
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