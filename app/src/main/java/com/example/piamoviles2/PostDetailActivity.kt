// Ubicación: app/src/main/java/com/example/piamoviles2/PostDetailActivity.kt

package com.example.piamoviles2

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.piamoviles2.databinding.ActivityPostDetailBinding
import com.example.piamoviles2.data.repositories.PublicacionRepository
import com.example.piamoviles2.data.models.PublicacionDetalle
import com.example.piamoviles2.utils.SessionManager
import kotlinx.coroutines.*
import com.example.piamoviles2.utils.ImageUtils

class PostDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPostDetailBinding
    private lateinit var commentAdapter: CommentAdapter
    private var currentPost: Post? = null
    private var comments = mutableListOf<Comment>()

    // Estados de like/dislike
    private var currentLikeState: LikeState = LikeState.NONE

    private lateinit var publicacionRepository: PublicacionRepository
    private lateinit var sessionManager: SessionManager
    private var isLoading = false

    enum class LikeState {
        NONE, LIKED, DISLIKED
    }

    companion object {
        const val EXTRA_POST_ID = "extra_post_id"
        const val EXTRA_POST_API_ID = "extra_post_id"
        private const val TAG = "POST_DETAIL_DEBUG"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPostDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        publicacionRepository = PublicacionRepository()
        sessionManager = SessionManager(this)

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
        val apiId = intent.getStringExtra(EXTRA_POST_API_ID)

        android.util.Log.d(TAG, "=== loadPostData ===")
        android.util.Log.d(TAG, "Post ID local: $postId")
        android.util.Log.d(TAG, "Post API ID: $apiId")

        if (!apiId.isNullOrEmpty()) {
            android.util.Log.d(TAG, "Cargando desde API con ID: $apiId")
            loadPostFromApi(apiId)
        } else if (postId != -1) {
            android.util.Log.d(TAG, "Usando datos de ejemplo para ID: $postId")
            val samplePost = Post.getSamplePosts().find { it.id == postId }
            if (samplePost != null) {
                currentPost = samplePost
                displayPostData(samplePost)
            } else {
                showError("Publicación no encontrada")
            }
        } else {
            showError("Error al cargar publicación")
        }
    }

    private fun loadPostFromApi(apiId: String) {
        val token = sessionManager.getAccessToken()
        val currentUser = sessionManager.getCurrentUser()

        if (token == null) {
            showError("Sesión no válida")
            return
        }

        android.util.Log.d(TAG, "Cargando publicación desde API: $apiId")
        setLoading(true)

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    publicacionRepository.obtenerPublicacionDetalleCompleta(apiId, token)
                }

                result.fold(
                    onSuccess = { publicacion ->
                        android.util.Log.d(TAG, "✅ Publicación cargada: ${publicacion.titulo}")

                        // Convertir a Post para mantener compatibilidad
                        currentPost = convertirDetalleAPost(publicacion, currentUser?.id ?: "")
                        displayPostData(currentPost!!)

                        // Usar comentarios de ejemplo por ahora
                        loadComments()
                    },
                    onFailure = { error ->
                        android.util.Log.e(TAG, "❌ Error: ${error.message}")
                        showError("Error al cargar publicación: ${error.message}")
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e(TAG, "❌ Exception: ${e.message}")
                showError("Error inesperado: ${e.message}")
            } finally {
                setLoading(false)
            }
        }
    }

    // ✅ CORREGIDO: Usar PublicacionDetalle en lugar de PublicacionDetalleCompleta
    private fun convertirDetalleAPost(detalle: PublicacionDetalle, currentUserId: String): Post {
        // Usar la primera imagen como preview - CORREGIDO: multimedia en lugar de multimediaList
        val imagenPreview = detalle.multimedia.firstOrNull { it.tipo == "imagen" }?.url ?: ""

        // Seleccionar fecha apropiada
        val fechaAUsar = when {
            detalle.estatus == "BORRADOR" && !detalle.fechaCreacion.isNullOrEmpty() -> detalle.fechaCreacion
            !detalle.fechaPublicacion.isNullOrEmpty() -> detalle.fechaPublicacion
            !detalle.fechaCreacion.isNullOrEmpty() -> detalle.fechaCreacion
            else -> null
        }

        android.util.Log.d(TAG, "=== convertirDetalleAPost ===")
        android.util.Log.d(TAG, "Multimedia disponibles: ${detalle.multimedia.size}")
        android.util.Log.d(TAG, "Imagen preview: $imagenPreview")
        android.util.Log.d(TAG, "Fecha seleccionada: $fechaAUsar")

        return Post(
            id = detalle.id.hashCode(),
            apiId = detalle.id,
            title = detalle.titulo,
            description = detalle.descripcion ?: "",
            imageUrl = imagenPreview,
            author = detalle.autorAlias ?: "Usuario Anónimo",
            createdAt = formatearFecha(fechaAUsar),
            isOwner = detalle.idAutor == currentUserId,
            isFavorite = false,
            isDraft = detalle.estatus == "BORRADOR",
            likesCount = detalle.totalReacciones,
            commentsCount = detalle.totalComentarios
        )
    }

    private fun updatePostUI() {
        currentPost?.let { post ->
            displayPostData(post)
        }
    }

    private fun displayPostData(post: Post) {
        android.util.Log.d(TAG, "=== displayPostData ===")
        android.util.Log.d(TAG, "Título: ${post.title}")
        android.util.Log.d(TAG, "Imagen URL: ${post.imageUrl}")

        binding.tvPostTitle.text = post.title
        binding.tvPostAuthor.text = post.author
        binding.tvPostDescription.text = post.description

        // ✅ CARGAR IMAGEN REAL DESDE CLOUDINARY
        if (ImageUtils.isValidImageUrl(post.imageUrl)) {
            android.util.Log.d(TAG, "✅ Cargando imagen desde Cloudinary: ${post.imageUrl}")
            ImageUtils.loadPostImage(
                context = this,
                imageUrl = post.imageUrl,
                imageView = binding.ivPostImage1,
                showPlaceholder = true
            )
            binding.ivPostImage2.visibility = View.GONE
        } else {
            android.util.Log.d(TAG, "❌ URL no válida, usando placeholder")
            binding.ivPostImage1.setImageResource(R.mipmap.ic_launcher)
            binding.ivPostImage2.visibility = View.GONE
        }

        // Actualizar estado de favorito
        updateFavoriteButton()
    }

    private fun setLoading(loading: Boolean) {
        isLoading = loading
        if (loading) {
            android.util.Log.d(TAG, "Mostrando loading...")
        } else {
            android.util.Log.d(TAG, "Ocultando loading...")
        }
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        android.util.Log.e(TAG, "Error mostrado: $message")
        finish()
    }

    private fun formatearFecha(fechaApi: String?): String {
        if (fechaApi.isNullOrEmpty()) return "Sin fecha"

        try {
            if (fechaApi.contains("T")) {
                val soloFecha = fechaApi.split("T")[0]
                val partes = soloFecha.split("-")
                if (partes.size == 3) {
                    return "${partes[2]}/${partes[1]}/${partes[0]}"
                }
            }
            return fechaApi
        } catch (e: Exception) {
            return "Fecha inválida"
        }
    }

    private fun loadComments() {
        // Si no hay comentarios desde API, usar datos de ejemplo
        if (comments.isEmpty()) {
            comments.clear()
            comments.addAll(Comment.getSampleComments())
            commentAdapter.notifyDataSetChanged()
        }
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