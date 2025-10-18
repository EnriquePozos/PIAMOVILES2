package com.example.piamoviles2

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.piamoviles2.databinding.ActivityPostDetailBinding

class PostDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPostDetailBinding
    private lateinit var commentAdapter: CommentAdapter
    private var currentPost: Post? = null
    private var comments = mutableListOf<Comment>()

    // Estados de like/dislike
    private var currentLikeState: LikeState = LikeState.NONE

    enum class LikeState {
        NONE, LIKED, DISLIKED
    }

    companion object {
        const val EXTRA_POST_ID = "extra_post_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPostDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupHeader()
        setupRecyclerView()
        setupClickListeners()
        loadPostData()
        loadComments()
    }

    private fun setupHeader() {
        val headerView = findViewById<View>(R.id.headerApp)
        HeaderUtils.setupHeaderWithBackButton(this, headerView)
    }

    private fun setupRecyclerView() {
        commentAdapter = CommentAdapter(
            comments = comments,
            onCommentLike = { comment -> handleCommentLike(comment) },
            onCommentDislike = { comment -> handleCommentDislike(comment) },
            onReplyLike = { reply -> handleReplyLike(reply) },
            onReplyDislike = { reply -> handleReplyDislike(reply) },
            onReplySubmit = { comment, replyText -> handleReplySubmit(comment, replyText) }
        )

        binding.rvComments.apply {
            layoutManager = LinearLayoutManager(this@PostDetailActivity)
            adapter = commentAdapter
        }
    }

    private fun setupClickListeners() {
        // Botón Like del post
        binding.btnLike.setOnClickListener {
            when (currentLikeState) {
                LikeState.NONE -> {
                    setLikeState(LikeState.LIKED)
                    Toast.makeText(this, "Te gusta esta publicación", Toast.LENGTH_SHORT).show()
                }
                LikeState.LIKED -> {
                    setLikeState(LikeState.NONE)
                    Toast.makeText(this, "Like removido", Toast.LENGTH_SHORT).show()
                }
                LikeState.DISLIKED -> {
                    setLikeState(LikeState.LIKED)
                    Toast.makeText(this, "Te gusta esta publicación", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Botón Dislike del post
        binding.btnDislike.setOnClickListener {
            when (currentLikeState) {
                LikeState.NONE -> {
                    setLikeState(LikeState.DISLIKED)
                    Toast.makeText(this, "No te gusta esta publicación", Toast.LENGTH_SHORT).show()
                }
                LikeState.DISLIKED -> {
                    setLikeState(LikeState.NONE)
                    Toast.makeText(this, "Dislike removido", Toast.LENGTH_SHORT).show()
                }
                LikeState.LIKED -> {
                    setLikeState(LikeState.DISLIKED)
                    Toast.makeText(this, "No te gusta esta publicación", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Botón Favoritos
        binding.btnFavorite.setOnClickListener {
            currentPost?.let { post ->
                post.isFavorite = !post.isFavorite
                updateFavoriteButton()
                val message = if (post.isFavorite) {
                    "Agregado a favoritos"
                } else {
                    "Removido de favoritos"
                }
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        }

        // Campo para agregar comentario nuevo
        binding.btnSendComment.setOnClickListener {
            val commentText = binding.etNewComment.text.toString().trim()
            if (commentText.isNotEmpty()) {
                // Crear nuevo comentario principal
                val newComment = Comment(
                    id = System.currentTimeMillis().toInt(), // ID temporal único
                    user = "Usuario Actual", // En una app real sería el usuario logueado
                    text = commentText,
                    timestamp = "Ahora",
                    userLikeState = Comment.LikeState.NONE,
                    replies = mutableListOf()
                )

                // Agregar a la lista de comentarios
                comments.add(newComment)

                // Limpiar el campo de texto
                binding.etNewComment.text.clear()

                // Actualizar el RecyclerView
                commentAdapter.notifyItemInserted(comments.size - 1)

                // Scroll al nuevo comentario
                binding.rvComments.scrollToPosition(comments.size - 1)
                Toast.makeText(this, "Comentario agregado", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Escribe un comentario", Toast.LENGTH_SHORT).show()
            }
        }

    }

    private fun setLikeState(newState: LikeState) {
        currentLikeState = newState
        updateLikeButtons()
    }

    private fun updateLikeButtons() {
        when (currentLikeState) {
            LikeState.NONE -> {
                binding.btnLike.setColorFilter(resources.getColor(android.R.color.darker_gray))
                binding.btnDislike.setColorFilter(resources.getColor(android.R.color.darker_gray))
            }
            LikeState.LIKED -> {
                binding.btnLike.setColorFilter(resources.getColor(android.R.color.holo_green_dark))
                binding.btnDislike.setColorFilter(resources.getColor(android.R.color.darker_gray))
            }
            LikeState.DISLIKED -> {
                binding.btnLike.setColorFilter(resources.getColor(android.R.color.darker_gray))
                binding.btnDislike.setColorFilter(resources.getColor(android.R.color.holo_red_dark))
            }
        }
    }

    private fun updateFavoriteButton() {
        currentPost?.let { post ->
            val color = if (post.isFavorite) {
                resources.getColor(android.R.color.holo_orange_dark)
            } else {
                resources.getColor(android.R.color.darker_gray)
            }
            binding.btnFavorite.setColorFilter(color)
        }
    }

    private fun loadPostData() {
        val postId = intent.getIntExtra(EXTRA_POST_ID, -1)
        if (postId == -1) {
            Toast.makeText(this, "Error: Post no encontrado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Buscar el post por ID (usando datos de ejemplo)
        currentPost = Post.getSamplePosts().find { it.id == postId }

        currentPost?.let { post ->
            displayPostData(post)
        } ?: run {
            Toast.makeText(this, "Error: Post no encontrado", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun displayPostData(post: Post) {
        binding.tvPostTitle.text = post.title
        binding.tvPostAuthor.text = post.author
        binding.tvPostDescription.text = post.description

        // Mostrar imágenes (simulado - en una app real cargarías desde URLs)
        binding.ivPostImage1.setImageResource(R.mipmap.ic_launcher)

        // Si hay múltiples imágenes, mostrar la segunda
        if (post.imageUrl.contains("multiple") || post.title.contains("Desayuno")) {
            binding.ivPostImage2.visibility = View.VISIBLE
            binding.ivPostImage2.setImageResource(R.mipmap.ic_launcher)
        } else {
            binding.ivPostImage2.visibility = View.GONE
        }

        // Actualizar estado de favorito
        updateFavoriteButton()
    }

    private fun loadComments() {
        // Datos de ejemplo para comentarios
        comments.clear()
        comments.addAll(Comment.getSampleComments())
        commentAdapter.notifyDataSetChanged()
    }

    // Manejo de likes en comentarios principales
    private fun handleCommentLike(comment: Comment) {
        when (comment.userLikeState) {
            Comment.LikeState.NONE -> {
                comment.userLikeState = Comment.LikeState.LIKED
                Toast.makeText(this, "Te gusta este comentario", Toast.LENGTH_SHORT).show()
            }
            Comment.LikeState.LIKED -> {
                comment.userLikeState = Comment.LikeState.NONE
                Toast.makeText(this, "Like removido", Toast.LENGTH_SHORT).show()
            }
            Comment.LikeState.DISLIKED -> {
                comment.userLikeState = Comment.LikeState.LIKED
                Toast.makeText(this, "Te gusta este comentario", Toast.LENGTH_SHORT).show()
            }
        }
        commentAdapter.notifyDataSetChanged()
    }

    private fun handleCommentDislike(comment: Comment) {
        when (comment.userLikeState) {
            Comment.LikeState.NONE -> {
                comment.userLikeState = Comment.LikeState.DISLIKED
                Toast.makeText(this, "No te gusta este comentario", Toast.LENGTH_SHORT).show()
            }
            Comment.LikeState.DISLIKED -> {
                comment.userLikeState = Comment.LikeState.NONE
                Toast.makeText(this, "Dislike removido", Toast.LENGTH_SHORT).show()
            }
            Comment.LikeState.LIKED -> {
                comment.userLikeState = Comment.LikeState.DISLIKED
                Toast.makeText(this, "No te gusta este comentario", Toast.LENGTH_SHORT).show()
            }
        }
        commentAdapter.notifyDataSetChanged()
    }

    // Manejo de likes en respuestas
    private fun handleReplyLike(reply: Comment.Reply) {
        when (reply.userLikeState) {
            Comment.LikeState.NONE -> {
                reply.userLikeState = Comment.LikeState.LIKED
                Toast.makeText(this, "Te gusta esta respuesta", Toast.LENGTH_SHORT).show()
            }
            Comment.LikeState.LIKED -> {
                reply.userLikeState = Comment.LikeState.NONE
                Toast.makeText(this, "Like removido", Toast.LENGTH_SHORT).show()
            }
            Comment.LikeState.DISLIKED -> {
                reply.userLikeState = Comment.LikeState.LIKED
                Toast.makeText(this, "Te gusta esta respuesta", Toast.LENGTH_SHORT).show()
            }
        }
        commentAdapter.notifyDataSetChanged()
    }

    private fun handleReplyDislike(reply: Comment.Reply) {
        when (reply.userLikeState) {
            Comment.LikeState.NONE -> {
                reply.userLikeState = Comment.LikeState.DISLIKED
                Toast.makeText(this, "No te gusta esta respuesta", Toast.LENGTH_SHORT).show()
            }
            Comment.LikeState.DISLIKED -> {
                reply.userLikeState = Comment.LikeState.NONE
                Toast.makeText(this, "Dislike removido", Toast.LENGTH_SHORT).show()
            }
            Comment.LikeState.LIKED -> {
                reply.userLikeState = Comment.LikeState.DISLIKED
                Toast.makeText(this, "No te gusta esta respuesta", Toast.LENGTH_SHORT).show()
            }
        }
        commentAdapter.notifyDataSetChanged()
    }

    // Manejo de envío de respuestas
    private fun handleReplySubmit(parentComment: Comment, replyText: String) {
        if (replyText.trim().isEmpty()) {
            Toast.makeText(this, "Escribe una respuesta", Toast.LENGTH_SHORT).show()
            return
        }

        // Crear nueva respuesta
        val newReply = Comment.Reply(
            id = System.currentTimeMillis().toInt(), // ID temporal
            user = "Usuario Actual", // En una app real sería el usuario logueado
            text = replyText.trim(),
            timestamp = "Ahora",
            userLikeState = Comment.LikeState.NONE
        )

        // Agregar la respuesta al comentario padre
        parentComment.replies.add(newReply)

        Toast.makeText(this, "Respuesta agregada", Toast.LENGTH_SHORT).show()
        commentAdapter.notifyDataSetChanged()
    }
}