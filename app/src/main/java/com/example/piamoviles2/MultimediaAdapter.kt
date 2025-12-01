package com.example.piamoviles2

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.io.File

/**
 * Adapter para mostrar items multimedia (imágenes y videos) en un RecyclerView
 *
 * @property items Lista mutable de items multimedia
 * @property onRemove Callback cuando se elimina un item
 */
class MultimediaAdapter(
    private val items: MutableList<MultimediaItem>,
    private val onRemove: (Int) -> Unit
) : RecyclerView.Adapter<MultimediaAdapter.MultimediaViewHolder>() {

    companion object {
        private const val TAG = "MULTIMEDIA_ADAPTER"
    }

    /**
     * ViewHolder para cada item de multimedia
     */
    inner class MultimediaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivPreview: ImageView = view.findViewById(R.id.ivPreview)
        val ivVideoIcon: ImageView = view.findViewById(R.id.ivVideoIcon)
        val viewVideoOverlay: View = view.findViewById(R.id.viewVideoOverlay)
        val tvTypeBadge: TextView = view.findViewById(R.id.tvTypeBadge)
        val btnRemove: ImageButton = view.findViewById(R.id.btnRemove)

        fun bind(item: MultimediaItem, position: Int) {
            // Configurar según el tipo de multimedia
            when {
                item.esImagen() -> bindImagen(item)
                item.esVideo() -> bindVideo(item)
            }

            // Configurar badge de tipo
            tvTypeBadge.text = item.getEmoji()

            // Configurar botón de eliminar
            btnRemove.setOnClickListener {
                android.util.Log.d(TAG, "Eliminando item en posición: $position")
                onRemove(position)
            }
        }

        private fun bindImagen(item: MultimediaItem) {
            item.bitmap?.let { bitmap ->
                ivPreview.setImageBitmap(bitmap)
                ivVideoIcon.visibility = View.GONE
                viewVideoOverlay.visibility = View.GONE

                android.util.Log.d(TAG, "Imagen cargada - ID: ${item.id}")
            } ?: run {
                // Si no hay bitmap, mostrar placeholder
                ivPreview.setImageResource(R.drawable.ic_launcher_background)
                android.util.Log.w(TAG, "Item imagen sin bitmap - ID: ${item.id}")
            }
        }

        private fun bindVideo(item: MultimediaItem) {
            // Si hay thumbnail, mostrarlo
            if (item.videoThumbnail != null) {
                ivPreview.setImageBitmap(item.videoThumbnail)
            } else {
                // Usar placeholder para video
                ivPreview.setImageResource(R.drawable.ic_launcher_background)
            }

            // Mostrar overlay e icono de play
            viewVideoOverlay.visibility = View.VISIBLE
            ivVideoIcon.visibility = View.VISIBLE

            android.util.Log.d(TAG, "Video configurado - URI: ${item.videoUri}")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MultimediaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_multimedia_preview, parent, false)
        return MultimediaViewHolder(view)
    }

    override fun onBindViewHolder(holder: MultimediaViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item, position)
    }

    override fun getItemCount(): Int = items.size

    /**
     * Agrega un nuevo item al final de la lista
     */
    fun addItem(item: MultimediaItem) {
        items.add(item)
        notifyItemInserted(items.size - 1)
        android.util.Log.d(TAG, "Item agregado - Total: ${items.size}")
    }

    /**
     * Elimina un item en la posición especificada
     */
    fun removeItem(position: Int) {
        if (position in items.indices) {
            val item = items[position]
            items.removeAt(position)
            notifyItemRemoved(position)

            android.util.Log.d(TAG, "Item eliminado en posición $position - Total: ${items.size}")
        } else {
            android.util.Log.w(TAG, "Intento de eliminar posición inválida: $position")
        }
    }

    /**
     * Limpia todos los items
     */
    fun clearAll() {
        val count = items.size
        items.clear()
        notifyDataSetChanged()
        android.util.Log.d(TAG, "Todos los items eliminados - Conteo anterior: $count")
    }

    /**
     * Obtiene la lista de archivos listos para subir a la API
     */
    fun getFiles(): List<File> {
        return items.mapNotNull { it.file }
    }

    /**
     * Verifica si hay items en la lista
     */
    fun isEmpty(): Boolean = items.isEmpty()

    /**
     * Obtiene el número de items
     */
    fun getSize(): Int = items.size
}