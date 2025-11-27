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
import com.example.piamoviles2.data.models.VerificarReaccionResponse
import com.example.piamoviles2.data.models.ConteoReaccionesResponse
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

    // ============================================
    // 🆕 VARIABLES PARA REACCIONES REALES
    // ============================================
    private var currentLikes = 0
    private var currentDislikes = 0
    private var isLoadingReaction = false

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
        // ============================================
        // 🆕 BOTÓN LIKE CON API REAL
        // ============================================
        binding.btnLike.setOnClickListener {
            if (isLoadingReaction) {
                android.util.Log.d(TAG, "Ya hay una operación en curso, ignorando click")
                return@setOnClickListener
            }

            val currentUser = sessionManager.getCurrentUser()
            val token = sessionManager.getAccessToken()
            val apiId = currentPost?.apiId

            if (currentUser == null || token == null || apiId == null) {
                Toast.makeText(this, "Error: Sesión no válida", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            android.util.Log.d(TAG, "Click en LIKE - Estado actual: $currentLikeState")
            performLikeAction(apiId, currentUser.id, token)
        }

        // ============================================
        // 🆕 BOTÓN DISLIKE CON API REAL
        // ============================================
        binding.btnDislike.setOnClickListener {
            if (isLoadingReaction) {
                android.util.Log.d(TAG, "Ya hay una operación en curso, ignorando click")
                return@setOnClickListener
            }

            val currentUser = sessionManager.getCurrentUser()
            val token = sessionManager.getAccessToken()
            val apiId = currentPost?.apiId

            if (currentUser == null || token == null || apiId == null) {
                Toast.makeText(this, "Error: Sesión no válida", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            android.util.Log.d(TAG, "Click en DISLIKE - Estado actual: $currentLikeState")
            performDislikeAction(apiId, currentUser.id, token)
        }

        // Botón Favoritos (mantener lógica existente)
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

    // ============================================
    // 🆕 MÉTODOS PARA LLAMADAS API REALES
    // ============================================

    /**
     * Ejecutar toggle like con API
     */
    private fun performLikeAction(apiId: String, userId: String, token: String) {
        isLoadingReaction = true
        updateReactionButtonsLoading(true)

        CoroutineScope(Dispatchers.Main).launch {
            try {
                android.util.Log.d(TAG, "=== performLikeAction ===")

                val result = withContext(Dispatchers.IO) {
                    publicacionRepository.toggleLike(apiId, userId, token)
                }

                result.fold(
                    onSuccess = { estadoFinal ->
                        android.util.Log.d(TAG, "✅ Toggle like exitoso")
                        android.util.Log.d(TAG, "Estado final: tiene=${estadoFinal.tieneReaccion}, tipo=${estadoFinal.tipoReaccion}")

                        // Actualizar estado local
                        currentLikeState = when {
                            estadoFinal.esLike() -> LikeState.LIKED
                            estadoFinal.esDislike() -> LikeState.DISLIKED
                            else -> LikeState.NONE
                        }

                        // Mostrar mensaje apropiado
                        val message = when (currentLikeState) {
                            LikeState.LIKED -> "Te gusta esta publicación"
                            LikeState.NONE -> "Like removido"
                            LikeState.DISLIKED -> "Te gusta esta publicación" // Cambió de dislike a like
                        }
                        Toast.makeText(this@PostDetailActivity, message, Toast.LENGTH_SHORT).show()

                        // Recargar contadores
                        loadReactionCounts(apiId, token)
                    },
                    onFailure = { error ->
                        android.util.Log.e(TAG, "❌ Error en toggle like: ${error.message}")
                        Toast.makeText(this@PostDetailActivity, "Error al procesar like", Toast.LENGTH_SHORT).show()
                        updateLikeButtons() // Restaurar estado visual
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e(TAG, "❌ Exception en performLikeAction: ${e.message}")
                Toast.makeText(this@PostDetailActivity, "Error inesperado", Toast.LENGTH_SHORT).show()
                updateLikeButtons() // Restaurar estado visual
            } finally {
                isLoadingReaction = false
                updateReactionButtonsLoading(false)
            }
        }
    }

    /**
     * Ejecutar toggle dislike con API
     */
    private fun performDislikeAction(apiId: String, userId: String, token: String) {
        isLoadingReaction = true
        updateReactionButtonsLoading(true)

        CoroutineScope(Dispatchers.Main).launch {
            try {
                android.util.Log.d(TAG, "=== performDislikeAction ===")

                val result = withContext(Dispatchers.IO) {
                    publicacionRepository.toggleDislike(apiId, userId, token)
                }

                result.fold(
                    onSuccess = { estadoFinal ->
                        android.util.Log.d(TAG, "✅ Toggle dislike exitoso")
                        android.util.Log.d(TAG, "Estado final: tiene=${estadoFinal.tieneReaccion}, tipo=${estadoFinal.tipoReaccion}")

                        // Actualizar estado local
                        currentLikeState = when {
                            estadoFinal.esLike() -> LikeState.LIKED
                            estadoFinal.esDislike() -> LikeState.DISLIKED
                            else -> LikeState.NONE
                        }

                        // Mostrar mensaje apropiado
                        val message = when (currentLikeState) {
                            LikeState.DISLIKED -> "No te gusta esta publicación"
                            LikeState.NONE -> "Dislike removido"
                            LikeState.LIKED -> "No te gusta esta publicación" // Cambió de like a dislike
                        }
                        Toast.makeText(this@PostDetailActivity, message, Toast.LENGTH_SHORT).show()

                        // Recargar contadores
                        loadReactionCounts(apiId, token)
                    },
                    onFailure = { error ->
                        android.util.Log.e(TAG, "❌ Error en toggle dislike: ${error.message}")
                        Toast.makeText(this@PostDetailActivity, "Error al procesar dislike", Toast.LENGTH_SHORT).show()
                        updateLikeButtons() // Restaurar estado visual
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e(TAG, "❌ Exception en performDislikeAction: ${e.message}")
                Toast.makeText(this@PostDetailActivity, "Error inesperado", Toast.LENGTH_SHORT).show()
                updateLikeButtons() // Restaurar estado visual
            } finally {
                isLoadingReaction = false
                updateReactionButtonsLoading(false)
            }
        }
    }

    /**
     * Cargar estado inicial de reacciones del usuario
     */
    private fun loadInitialReactionState(apiId: String, userId: String, token: String) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                android.util.Log.d(TAG, "=== loadInitialReactionState ===")

                val result = withContext(Dispatchers.IO) {
                    publicacionRepository.verificarReaccionUsuario(apiId, userId, token)
                }

                result.fold(
                    onSuccess = { verificacion ->
                        android.util.Log.d(TAG, "✅ Estado inicial cargado")
                        android.util.Log.d(TAG, "Tiene reacción: ${verificacion.tieneReaccion}, Tipo: ${verificacion.tipoReaccion}")

                        currentLikeState = when {
                            verificacion.esLike() -> LikeState.LIKED
                            verificacion.esDislike() -> LikeState.DISLIKED
                            else -> LikeState.NONE
                        }

                        updateLikeButtons()
                    },
                    onFailure = { error ->
                        android.util.Log.e(TAG, "❌ Error al cargar estado inicial: ${error.message}")
                        // No mostrar error al usuario, usar estado por defecto
                        currentLikeState = LikeState.NONE
                        updateLikeButtons()
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e(TAG, "❌ Exception en loadInitialReactionState: ${e.message}")
                currentLikeState = LikeState.NONE
                updateLikeButtons()
            }
        }
    }

    /**
     * Cargar contadores de reacciones
     */
    private fun loadReactionCounts(apiId: String, token: String) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                android.util.Log.d(TAG, "=== loadReactionCounts ===")

                val result = withContext(Dispatchers.IO) {
                    publicacionRepository.obtenerConteoReacciones(apiId, token)
                }

                result.fold(
                    onSuccess = { conteo ->
                        android.util.Log.d(TAG, "✅ Conteos cargados - Likes: ${conteo.likes}, Dislikes: ${conteo.dislikes}")

                        currentLikes = conteo.likes
                        currentDislikes = conteo.dislikes

                        // Actualizar UI con nuevos contadores
                        updateReactionCounters()

                        // Actualizar currentPost también
                        // currentPost?.likesCount = conteo.total
                    },
                    onFailure = { error ->
                        android.util.Log.e(TAG, "❌ Error al cargar conteos: ${error.message}")
                        // No mostrar error al usuario, mantener contadores actuales
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e(TAG, "❌ Exception en loadReactionCounts: ${e.message}")
            }
        }
    }

    /**
     * Actualizar contadores en la UI
     */
    private fun updateReactionCounters() {
        // TODO: Si tienes TextViews para mostrar contadores, actualízalos aquí
        // Por ejemplo:
        // binding.tvLikeCount.text = currentLikes.toString()
        // binding.tvDislikeCount.text = currentDislikes.toString()

        android.util.Log.d(TAG, "Contadores actualizados - Likes: $currentLikes, Dislikes: $currentDislikes")
    }

    /**
     * Mostrar estado de loading en botones
     */
    private fun updateReactionButtonsLoading(loading: Boolean) {
        binding.btnLike.isEnabled = !loading
        binding.btnDislike.isEnabled = !loading

        if (loading) {
            // Opcional: cambiar aspecto visual durante loading
            binding.btnLike.alpha = 0.5f
            binding.btnDislike.alpha = 0.5f
        } else {
            binding.btnLike.alpha = 1.0f
            binding.btnDislike.alpha = 1.0f
            updateLikeButtons() // Restaurar colores normales
        }
    }

    // ============================================
    // MÉTODOS EXISTENTES (SIN CAMBIOS)
    // ============================================

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

                        // ============================================
                        // 🆕 CARGAR ESTADO DE REACCIONES
                        // ============================================
                        if (currentUser != null) {
                            loadInitialReactionState(apiId, currentUser.id, token)
                            loadReactionCounts(apiId, token)
                        }

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

    // CORREGIDO: Usar PublicacionDetalle en lugar de PublicacionDetalleCompleta
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

        // CARGAR IMAGEN REAL DESDE CLOUDINARY
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
            android.util.Log.d(TAG, "📱 URL no válida, usando placeholder")
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

    // Manejo de likes en comentarios principales (sin cambios)
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

    // Manejo de likes en respuestas (sin cambios)
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

    // Manejo de envío de respuestas (sin cambios)
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