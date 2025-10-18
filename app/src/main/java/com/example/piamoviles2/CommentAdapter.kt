package com.example.piamoviles2

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

/**
 * Adaptador para comentarios con respuestas anidadas
 */
class CommentAdapter(
    private val comments: List<Comment>,
    private val onCommentLike: (Comment) -> Unit,
    private val onCommentDislike: (Comment) -> Unit,
    private val onReplyLike: (Comment.Reply) -> Unit,
    private val onReplyDislike: (Comment.Reply) -> Unit,
    private val onReplySubmit: (Comment, String) -> Unit
) : RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        holder.bind(comments[position])
    }

    override fun getItemCount(): Int = comments.size

    inner class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        // Views del comentario principal
        private val tvCommentUser: TextView = itemView.findViewById(R.id.tvCommentUser)
        private val tvCommentText: TextView = itemView.findViewById(R.id.tvCommentText)
        private val btnCommentLike: ImageButton = itemView.findViewById(R.id.btnCommentLike)
        private val btnCommentDislike: ImageButton = itemView.findViewById(R.id.btnCommentDislike)

        // Views para respuestas
        private val layoutReplies: LinearLayout = itemView.findViewById(R.id.layoutReplies)

        // Views para agregar respuesta
        private val etReply: EditText = itemView.findViewById(R.id.etReply)
        private val btnResponder: Button = itemView.findViewById(R.id.btnResponder)

        fun bind(comment: Comment) {
            // Configurar comentario principal
            tvCommentUser.text = comment.user
            tvCommentText.text = comment.text

            // Configurar botones de like/dislike del comentario
            updateCommentLikeButtons(comment)

            btnCommentLike.setOnClickListener {
                onCommentLike(comment)
            }

            btnCommentDislike.setOnClickListener {
                onCommentDislike(comment)
            }

            // Configurar respuestas
            setupReplies(comment)

            // Configurar botón responder
            btnResponder.setOnClickListener {
                val replyText = etReply.text.toString().trim()
                if (replyText.isNotEmpty()) {
                    onReplySubmit(comment, replyText)
                    etReply.text.clear()
                }
            }
        }

        private fun updateCommentLikeButtons(comment: Comment) {
            val context = itemView.context

            when (comment.userLikeState) {
                Comment.LikeState.NONE -> {
                    btnCommentLike.setColorFilter(ContextCompat.getColor(context, android.R.color.darker_gray))
                    btnCommentDislike.setColorFilter(ContextCompat.getColor(context, android.R.color.darker_gray))
                }
                Comment.LikeState.LIKED -> {
                    btnCommentLike.setColorFilter(ContextCompat.getColor(context, android.R.color.holo_green_dark))
                    btnCommentDislike.setColorFilter(ContextCompat.getColor(context, android.R.color.darker_gray))
                }
                Comment.LikeState.DISLIKED -> {
                    btnCommentLike.setColorFilter(ContextCompat.getColor(context, android.R.color.darker_gray))
                    btnCommentDislike.setColorFilter(ContextCompat.getColor(context, android.R.color.holo_red_dark))
                }
            }
        }

        private fun setupReplies(comment: Comment) {
            // Limpiar respuestas anteriores
            layoutReplies.removeAllViews()

            // Agregar cada respuesta
            comment.replies.forEach { reply ->
                val replyView = createReplyView(reply)
                layoutReplies.addView(replyView)
            }
        }

        private fun createReplyView(reply: Comment.Reply): View {
            val context = itemView.context
            val inflater = LayoutInflater.from(context)
            val replyView = inflater.inflate(R.layout.item_comment_reply, layoutReplies, false)

            // Configurar views de la respuesta
            val tvReplyUser: TextView = replyView.findViewById(R.id.tvReplyUser)
            val tvReplyText: TextView = replyView.findViewById(R.id.tvReplyText)


            // Establecer datos
            tvReplyUser.text = "${reply.user} respondió:"
            tvReplyText.text = reply.text


            return replyView
        }

        private fun updateReplyLikeButtons(
            reply: Comment.Reply,
            btnLike: ImageButton,
            btnDislike: ImageButton
        ) {
            val context = itemView.context

            when (reply.userLikeState) {
                Comment.LikeState.NONE -> {
                    btnLike.setColorFilter(ContextCompat.getColor(context, android.R.color.darker_gray))
                    btnDislike.setColorFilter(ContextCompat.getColor(context, android.R.color.darker_gray))
                }
                Comment.LikeState.LIKED -> {
                    btnLike.setColorFilter(ContextCompat.getColor(context, android.R.color.holo_green_dark))
                    btnDislike.setColorFilter(ContextCompat.getColor(context, android.R.color.darker_gray))
                }
                Comment.LikeState.DISLIKED -> {
                    btnLike.setColorFilter(ContextCompat.getColor(context, android.R.color.darker_gray))
                    btnDislike.setColorFilter(ContextCompat.getColor(context, android.R.color.holo_red_dark))
                }
            }
        }
    }
}