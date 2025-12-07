package com.example.piamoviles2

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.VideoView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.piamoviles2.data.models.MultimediaResponse

class MediaCarouselAdapter(
    private val mediaItems: List<MultimediaResponse>,
    private val onVideoClick: (String) -> Unit
) : RecyclerView.Adapter<MediaCarouselAdapter.MediaViewHolder>() {

    companion object {
        private const val TAG = "MEDIA_CAROUSEL_ADAPTER"
    }

    inner class MediaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivMedia: ImageView = view.findViewById(R.id.ivMedia)
        val vvMedia: VideoView = view.findViewById(R.id.vvMedia)
        val layoutVideoOverlay: View = view.findViewById(R.id.layoutVideoOverlay)
        val btnPlayVideo: ImageButton = view.findViewById(R.id.btnPlayVideo)
        val tvMediaType: TextView = view.findViewById(R.id.tvMediaType)

        fun bind(media: MultimediaResponse, position: Int) {
            android.util.Log.d(TAG, "Binding item $position: tipo=${media.tipo}, url=${media.url}")

            when (media.tipo.lowercase()) {
                "imagen" -> bindImage(media)
                "video" -> bindVideo(media)
                else -> {
                    android.util.Log.w(TAG, "Tipo desconocido: ${media.tipo}")
                    bindImage(media) // Fallback a imagen
                }
            }
        }

        private fun bindImage(media: MultimediaResponse) {
            // Mostrar ImageView, ocultar VideoView
            ivMedia.visibility = View.VISIBLE
            vvMedia.visibility = View.GONE
            layoutVideoOverlay.visibility = View.GONE
            tvMediaType.visibility = View.GONE

            // Cargar imagen con Glide
            Glide.with(itemView.context)
                .load(media.url)
                .placeholder(R.drawable.ic_launcher_background)
                .error(R.drawable.ic_launcher_background)
                .centerCrop()
                .into(ivMedia)

            android.util.Log.d(TAG, "  Imagen cargada: ${media.url}")
        }

        private fun bindVideo(media: MultimediaResponse) {
            // Mostrar overlay de video
            ivMedia.visibility = View.VISIBLE
            vvMedia.visibility = View.GONE
            layoutVideoOverlay.visibility = View.VISIBLE
            tvMediaType.visibility = View.VISIBLE
            tvMediaType.text = "🎥 Video"

            // Cargar thumbnail del video con Glide
            Glide.with(itemView.context)
                .load(media.url)
                .placeholder(R.drawable.ic_launcher_background)
                .error(R.drawable.ic_launcher_background)
                .centerCrop()
                .into(ivMedia)

            // Click para reproducir video
            layoutVideoOverlay.setOnClickListener {
                android.util.Log.d(TAG, "Click en video: ${media.url}")
                onVideoClick(media.url)
            }

            btnPlayVideo.setOnClickListener {
                android.util.Log.d(TAG, "Click en botón play: ${media.url}")
                onVideoClick(media.url)
            }

            android.util.Log.d(TAG, "  Video configurado: ${media.url}")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_media_carousel, parent, false)
        return MediaViewHolder(view)
    }

    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        holder.bind(mediaItems[position], position)
    }

    override fun getItemCount(): Int = mediaItems.size
}