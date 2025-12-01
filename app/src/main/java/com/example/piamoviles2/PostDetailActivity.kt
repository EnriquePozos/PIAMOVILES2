package com.example.piamoviles2

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.piamoviles2.databinding.ActivityPostDetailBinding
import com.example.piamoviles2.data.repositories.PublicacionRepository
import com.example.piamoviles2.data.repositories.ComentarioRepository
import com.example.piamoviles2.data.repositories.FavoritoRepository
import com.example.piamoviles2.data.models.PublicacionDetalle
import com.example.piamoviles2.data.models.VerificarReaccionResponse
import com.example.piamoviles2.data.models.ConteoReaccionesResponse
import com.example.piamoviles2.data.models.MultimediaResponse
import com.example.piamoviles2.utils.SessionManager
import kotlinx.coroutines.*
import com.example.piamoviles2.utils.ImageUtils

class PostDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPostDetailBinding

    // Variables para carrusel de multimedia
    private lateinit var mediaCarouselAdapter: MediaCarouselAdapter
    private var currentMediaPosition = 0
    private lateinit var commentAdapter: CommentAdapter
    private var currentPost: Post? = null
    private var comments = mutableListOf<Comment>()

    // Estados de like/dislike
    private var currentLikeState: LikeState = LikeState.NONE

    private lateinit var publicacionRepository: PublicacionRepository
    private lateinit var comentarioRepository: ComentarioRepository
    private lateinit var favoritoRepository: FavoritoRepository
    private lateinit var sessionManager: SessionManager
    private var isLoading = false

    // Variables para reacciones reales
    private var currentLikes = 0
    private var currentDislikes = 0
    private var isLoadingReaction = false

    // Variables para comentarios reales
    private var isLoadingComments = false
    private var isCreatingComment = false

    // Variables para favoritos reales
    private var isLoadingFavorite = false

    enum class LikeState {
        NONE, LIKED, DISLIKED
    }

    companion object {
        const val EXTRA_POST_ID = "extra_post_id"        // Para compatibilidad
        const val EXTRA_POST_API_ID = "extra_post_id"    // Para API real
        private const val TAG = "POST_DETAIL_DEBUG"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPostDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        publicacionRepository = PublicacionRepository()
        comentarioRepository = ComentarioRepository()
        favoritoRepository = FavoritoRepository()
        sessionManager = SessionManager(this)

        setupHeader()
        setupRecyclerView()
        setupClickListeners()
        loadPostData()
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
            onReplyLike = { _ -> }, // Sin funcionalidad - solo comentarios padre tienen likes
            onReplyDislike = { _ -> }, // Sin funcionalidad - solo comentarios padre tienen likes
            onReplySubmit = { comment, replyText -> handleReplySubmitReal(comment, replyText) }
        )

        binding.rvComments.apply {
            layoutManager = LinearLayoutManager(this@PostDetailActivity)
            adapter = commentAdapter
        }
    }

    private fun setupMediaCarousel(mediaItems: List<MultimediaResponse>) {
        android.util.Log.d(TAG, "=== setupMediaCarousel ===")
        android.util.Log.d(TAG, "Total items: ${mediaItems.size}")

        if (mediaItems.isEmpty()) {
            binding.layoutMediaCarousel.visibility = View.GONE
            return
        }

        binding.layoutMediaCarousel.visibility = View.VISIBLE

        // Crear adapter
        mediaCarouselAdapter = MediaCarouselAdapter(mediaItems) { videoUrl ->
            playVideo(videoUrl)
        }

        // Configurar ViewPager2
        binding.vpMediaCarousel.adapter = mediaCarouselAdapter
        binding.vpMediaCarousel.offscreenPageLimit = 1

        // Configurar flechas
        if (mediaItems.size > 1) {
            binding.btnPrevMedia.visibility = View.VISIBLE
            binding.btnNextMedia.visibility = View.VISIBLE
            binding.tvMediaCounter.visibility = View.VISIBLE

            binding.btnPrevMedia.setOnClickListener {
                val current = binding.vpMediaCarousel.currentItem
                if (current > 0) {
                    binding.vpMediaCarousel.currentItem = current - 1
                }
            }

            binding.btnNextMedia.setOnClickListener {
                val current = binding.vpMediaCarousel.currentItem
                if (current < mediaItems.size - 1) {
                    binding.vpMediaCarousel.currentItem = current + 1
                }
            }

            // Listener para actualizar contador
            binding.vpMediaCarousel.registerOnPageChangeCallback(object : androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    currentMediaPosition = position
                    updateMediaCounter(position + 1, mediaItems.size)
                    updateArrowsVisibility(position, mediaItems.size)
                }
            })

            updateMediaCounter(1, mediaItems.size)
        } else {
            binding.btnPrevMedia.visibility = View.GONE
            binding.btnNextMedia.visibility = View.GONE
            binding.tvMediaCounter.visibility = View.GONE
        }
    }

    private fun updateMediaCounter(current: Int, total: Int) {
        binding.tvMediaCounter.text = "$current/$total"
    }

    private fun updateArrowsVisibility(position: Int, total: Int) {
        binding.btnPrevMedia.alpha = if (position == 0) 0.5f else 1.0f
        binding.btnNextMedia.alpha = if (position == total - 1) 0.5f else 1.0f
    }

    private fun playVideo(videoUrl: String) {
        android.util.Log.d(TAG, "Reproduciendo video: $videoUrl")

        // Opción 1: Abrir en navegador/app de video
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
            setDataAndType(android.net.Uri.parse(videoUrl), "video/*")
        }

        try {
            startActivity(intent)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error al reproducir video", e)
            Toast.makeText(this, "No se puede reproducir el video", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupClickListeners() {
        // Botón Like con API real
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

        // Botón Dislike con API real
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

        // Botón Favoritos con API real
        binding.btnFavorite.setOnClickListener {
            if (isLoadingFavorite) {
                android.util.Log.d(TAG, "Ya hay una operación de favorito en curso, ignorando click")
                return@setOnClickListener
            }

            val currentUser = sessionManager.getCurrentUser()
            val token = sessionManager.getAccessToken()
            val apiId = currentPost?.apiId

            if (currentUser == null || token == null || apiId == null) {
                Toast.makeText(this, "Error: Sesión no válida", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            android.util.Log.d(TAG, "Click en FAVORITO - Estado actual: ${currentPost?.isFavorite}")
            performFavoriteAction(apiId, currentUser.id, token)
        }

        // Campo para agregar comentario nuevo con API real
        binding.btnSendComment.setOnClickListener {
            handleCreateCommentReal()
        }
    }

    // ============================================
    // MÉTODOS PARA LLAMADAS API REALES - FAVORITOS
    // ============================================

    private fun performFavoriteAction(apiId: String, userId: String, token: String) {
        isLoadingFavorite = true
        updateFavoriteButtonLoading(true)

        CoroutineScope(Dispatchers.Main).launch {
            try {
                android.util.Log.d(TAG, "=== performFavoriteAction ===")

                val result = withContext(Dispatchers.IO) {
                    favoritoRepository.toggleFavorito(userId, apiId, token)
                }

                result.fold(
                    onSuccess = { estadoFavorito ->
                        android.util.Log.d(TAG, "Toggle favorito exitoso")
                        android.util.Log.d(TAG, "Estado final: esFavorito=${estadoFavorito.esFavorito}, acción=${estadoFavorito.accion}")

                        // Actualizar estado local
                        currentPost?.isFavorite = estadoFavorito.esFavorito
                        updateFavoriteButton()

                        val message = if (estadoFavorito.esFavorito) {
                            "Agregado a favoritos"
                        } else {
                            "Removido de favoritos"
                        }
                        Toast.makeText(this@PostDetailActivity, message, Toast.LENGTH_SHORT).show()
                    },
                    onFailure = { error ->
                        android.util.Log.e(TAG, "Error en toggle favorito: ${error.message}")
                        Toast.makeText(this@PostDetailActivity, "Error al procesar favorito", Toast.LENGTH_SHORT).show()
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Exception en performFavoriteAction: ${e.message}")
                Toast.makeText(this@PostDetailActivity, "Error inesperado", Toast.LENGTH_SHORT).show()
            } finally {
                isLoadingFavorite = false
                updateFavoriteButtonLoading(false)
            }
        }
    }

    private fun loadInitialFavoriteState(apiId: String, userId: String, token: String) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                android.util.Log.d(TAG, "=== loadInitialFavoriteState ===")

                val result = withContext(Dispatchers.IO) {
                    favoritoRepository.verificarSiFavorito(userId, apiId, token)
                }

                result.fold(
                    onSuccess = { esFavorito ->
                        android.util.Log.d(TAG, "Estado inicial de favorito cargado: $esFavorito")
                        currentPost?.isFavorite = esFavorito
                        updateFavoriteButton()
                    },
                    onFailure = { error ->
                        android.util.Log.e(TAG, "Error al cargar estado inicial de favorito: ${error.message}")
                        currentPost?.isFavorite = false
                        updateFavoriteButton()
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Exception en loadInitialFavoriteState: ${e.message}")
                currentPost?.isFavorite = false
                updateFavoriteButton()
            }
        }
    }

    private fun updateFavoriteButtonLoading(loading: Boolean) {
        binding.btnFavorite.isEnabled = !loading

        if (loading) {
            binding.btnFavorite.alpha = 0.5f
        } else {
            binding.btnFavorite.alpha = 1.0f
            updateFavoriteButton()
        }
    }

    // ============================================
    // MÉTODOS PARA LLAMADAS API REALES - REACCIONES
    // ============================================

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
                        android.util.Log.d(TAG, "Toggle like exitoso")
                        android.util.Log.d(TAG, "Estado final: tiene=${estadoFinal.tieneReaccion}, tipo=${estadoFinal.tipoReaccion}")

                        currentLikeState = when {
                            estadoFinal.esLike() -> LikeState.LIKED
                            estadoFinal.esDislike() -> LikeState.DISLIKED
                            else -> LikeState.NONE
                        }

                        val message = when (currentLikeState) {
                            LikeState.LIKED -> "Te gusta esta publicación"
                            LikeState.NONE -> "Like removido"
                            LikeState.DISLIKED -> "Te gusta esta publicación"
                        }
                        Toast.makeText(this@PostDetailActivity, message, Toast.LENGTH_SHORT).show()

                        loadReactionCounts(apiId, token)
                    },
                    onFailure = { error ->
                        android.util.Log.e(TAG, "Error en toggle like: ${error.message}")
                        Toast.makeText(this@PostDetailActivity, "Error al procesar like", Toast.LENGTH_SHORT).show()
                        updateLikeButtons()
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Exception en performLikeAction: ${e.message}")
                Toast.makeText(this@PostDetailActivity, "Error inesperado", Toast.LENGTH_SHORT).show()
                updateLikeButtons()
            } finally {
                isLoadingReaction = false
                updateReactionButtonsLoading(false)
            }
        }
    }

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
                        android.util.Log.d(TAG, "Toggle dislike exitoso")
                        android.util.Log.d(TAG, "Estado final: tiene=${estadoFinal.tieneReaccion}, tipo=${estadoFinal.tipoReaccion}")

                        currentLikeState = when {
                            estadoFinal.esLike() -> LikeState.LIKED
                            estadoFinal.esDislike() -> LikeState.DISLIKED
                            else -> LikeState.NONE
                        }

                        val message = when (currentLikeState) {
                            LikeState.DISLIKED -> "No te gusta esta publicación"
                            LikeState.NONE -> "Dislike removido"
                            LikeState.LIKED -> "No te gusta esta publicación"
                        }
                        Toast.makeText(this@PostDetailActivity, message, Toast.LENGTH_SHORT).show()

                        loadReactionCounts(apiId, token)
                    },
                    onFailure = { error ->
                        android.util.Log.e(TAG, "Error en toggle dislike: ${error.message}")
                        Toast.makeText(this@PostDetailActivity, "Error al procesar dislike", Toast.LENGTH_SHORT).show()
                        updateLikeButtons()
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Exception en performDislikeAction: ${e.message}")
                Toast.makeText(this@PostDetailActivity, "Error inesperado", Toast.LENGTH_SHORT).show()
                updateLikeButtons()
            } finally {
                isLoadingReaction = false
                updateReactionButtonsLoading(false)
            }
        }
    }

    private fun loadInitialReactionState(apiId: String, userId: String, token: String) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                android.util.Log.d(TAG, "=== loadInitialReactionState ===")

                val result = withContext(Dispatchers.IO) {
                    publicacionRepository.verificarReaccionUsuario(apiId, userId, token)
                }

                result.fold(
                    onSuccess = { verificacion ->
                        android.util.Log.d(TAG, "Estado inicial cargado")
                        android.util.Log.d(TAG, "Tiene reacción: ${verificacion.tieneReaccion}, Tipo: ${verificacion.tipoReaccion}")

                        currentLikeState = when {
                            verificacion.esLike() -> LikeState.LIKED
                            verificacion.esDislike() -> LikeState.DISLIKED
                            else -> LikeState.NONE
                        }

                        updateLikeButtons()
                    },
                    onFailure = { error ->
                        android.util.Log.e(TAG, "Error al cargar estado inicial: ${error.message}")
                        currentLikeState = LikeState.NONE
                        updateLikeButtons()
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Exception en loadInitialReactionState: ${e.message}")
                currentLikeState = LikeState.NONE
                updateLikeButtons()
            }
        }
    }

    private fun loadReactionCounts(apiId: String, token: String) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                android.util.Log.d(TAG, "=== loadReactionCounts ===")

                val result = withContext(Dispatchers.IO) {
                    publicacionRepository.obtenerConteoReacciones(apiId, token)
                }

                result.fold(
                    onSuccess = { conteo ->
                        android.util.Log.d(TAG, "Conteos cargados - Likes: ${conteo.likes}, Dislikes: ${conteo.dislikes}")

                        currentLikes = conteo.likes
                        currentDislikes = conteo.dislikes

                        updateReactionCounters()
                    },
                    onFailure = { error ->
                        android.util.Log.e(TAG, "Error al cargar conteos: ${error.message}")
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Exception en loadReactionCounts: ${e.message}")
            }
        }
    }

    private fun updateReactionCounters() {
        android.util.Log.d(TAG, "Contadores actualizados - Likes: $currentLikes, Dislikes: $currentDislikes")
    }

    private fun updateReactionButtonsLoading(loading: Boolean) {
        binding.btnLike.isEnabled = !loading
        binding.btnDislike.isEnabled = !loading

        if (loading) {
            binding.btnLike.alpha = 0.5f
            binding.btnDislike.alpha = 0.5f
        } else {
            binding.btnLike.alpha = 1.0f
            binding.btnDislike.alpha = 1.0f
            updateLikeButtons()
        }
    }

    // ============================================
    // MÉTODOS PARA LLAMADAS API REALES - COMENTARIOS
    // ============================================

    /**
     * Cargar comentarios reales desde la API
     */
    private fun loadCommentsReal() {
        val apiId = currentPost?.apiId
        val token = sessionManager.getAccessToken()
        val currentUser = sessionManager.getCurrentUser()

        if (apiId == null || token == null || currentUser == null) {
            android.util.Log.w(TAG, "No se puede cargar comentarios: apiId=$apiId, token presente=${token != null}")
            loadCommentsMock() // Fallback a datos mock
            return
        }

        if (isLoadingComments) {
            android.util.Log.d(TAG, "Ya se están cargando comentarios, ignorando")
            return
        }

        isLoadingComments = true
        android.util.Log.d(TAG, "=== loadCommentsReal ===")
        android.util.Log.d(TAG, "Cargando comentarios para publicación: $apiId")

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    comentarioRepository.obtenerComentariosConvertidos(apiId, currentUser.id, token)
                }

                result.fold(
                    onSuccess = { comentariosReales ->
                        android.util.Log.d(TAG, "Comentarios reales cargados: ${comentariosReales.size}")

                        comments.clear()
                        comments.addAll(comentariosReales)
                        commentAdapter.notifyDataSetChanged()

                        if (comentariosReales.isEmpty()) {
                            android.util.Log.d(TAG, "No hay comentarios, mostrando mensaje")
                        }
                    },
                    onFailure = { error ->
                        android.util.Log.e(TAG, "Error al cargar comentarios reales: ${error.message}")
                        Toast.makeText(this@PostDetailActivity, "Error al cargar comentarios", Toast.LENGTH_SHORT).show()
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Exception en loadCommentsReal: ${e.message}")
                Toast.makeText(this@PostDetailActivity, "Error inesperado al cargar comentarios", Toast.LENGTH_SHORT).show()
            } finally {
                isLoadingComments = false
            }
        }
    }

    /**
     * Crear comentario real usando la API
     */
    private fun handleCreateCommentReal() {
        val commentText = binding.etNewComment.text.toString().trim()
        if (commentText.isEmpty()) {
            Toast.makeText(this, "Escribe un comentario", Toast.LENGTH_SHORT).show()
            return
        }

        val apiId = currentPost?.apiId
        val token = sessionManager.getAccessToken()
        val currentUser = sessionManager.getCurrentUser()

        if (apiId == null || token == null || currentUser == null) {
            Toast.makeText(this, "Error: Sesión no válida", Toast.LENGTH_SHORT).show()
            return
        }

        if (isCreatingComment) {
            android.util.Log.d(TAG, "Ya se está creando un comentario, ignorando")
            return
        }

        isCreatingComment = true
        android.util.Log.d(TAG, "=== handleCreateCommentReal ===")
        android.util.Log.d(TAG, "Creando comentario: ${commentText.take(50)}...")

        // Deshabilitar botón y mostrar loading
        binding.btnSendComment.isEnabled = false

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    comentarioRepository.crearComentarioConvertido(
                        idPublicacion = apiId,
                        comentario = commentText,
                        idUsuario = currentUser.id,
                        token = token
                    )
                }

                result.fold(
                    onSuccess = { nuevoComentario ->
                        android.util.Log.d(TAG, "Comentario creado exitosamente")

                        // Limpiar el campo de texto
                        binding.etNewComment.text.clear()

                        // Agregar a la lista local
                        comments.add(nuevoComentario)
                        commentAdapter.notifyItemInserted(comments.size - 1)

                        // Scroll al nuevo comentario
                        binding.rvComments.scrollToPosition(comments.size - 1)

                        Toast.makeText(this@PostDetailActivity, "Comentario agregado", Toast.LENGTH_SHORT).show()
                    },
                    onFailure = { error ->
                        android.util.Log.e(TAG, "Error al crear comentario: ${error.message}")
                        Toast.makeText(this@PostDetailActivity, "Error al crear comentario", Toast.LENGTH_SHORT).show()
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Exception en handleCreateCommentReal: ${e.message}")
                Toast.makeText(this@PostDetailActivity, "Error inesperado", Toast.LENGTH_SHORT).show()
            } finally {
                isCreatingComment = false
                binding.btnSendComment.isEnabled = true
            }
        }
    }

    /**
     * Crear respuesta real usando la API
     */
    private fun handleReplySubmitReal(parentComment: Comment, replyText: String) {
        if (replyText.trim().isEmpty()) {
            Toast.makeText(this, "Escribe una respuesta", Toast.LENGTH_SHORT).show()
            return
        }

        val token = sessionManager.getAccessToken()
        val currentUser = sessionManager.getCurrentUser()

        if (token == null || currentUser == null) {
            Toast.makeText(this, "Error: Sesión no válida", Toast.LENGTH_SHORT).show()
            return
        }

        // Necesitamos el ID del comentario API, no el hash local
        // Por ahora usamos el approach de convertir el hash de vuelta o usar una búsqueda
        val comentarioApiId = findApiIdFromComment(parentComment)
        if (comentarioApiId == null) {
            android.util.Log.w(TAG, "No se pudo encontrar API ID para el comentario")
            Toast.makeText(this, "Error: No se puede responder al comentario", Toast.LENGTH_SHORT).show()
            return
        }

        android.util.Log.d(TAG, "=== handleReplySubmitReal ===")
        android.util.Log.d(TAG, "Creando respuesta para comentario: $comentarioApiId")

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    comentarioRepository.crearRespuestaConvertida(
                        idComentarioPadre = comentarioApiId,
                        comentario = replyText.trim(),
                        idUsuario = currentUser.id,
                        token = token
                    )
                }

                result.fold(
                    onSuccess = { nuevaRespuesta ->
                        android.util.Log.d(TAG, "Respuesta creada exitosamente")

                        parentComment.replies.add(nuevaRespuesta)
                        commentAdapter.notifyDataSetChanged()

                        Toast.makeText(this@PostDetailActivity, "Respuesta agregada", Toast.LENGTH_SHORT).show()
                    },
                    onFailure = { error ->
                        android.util.Log.e(TAG, "Error al crear respuesta: ${error.message}")
                        Toast.makeText(this@PostDetailActivity, "Error al crear respuesta", Toast.LENGTH_SHORT).show()
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Exception en handleReplySubmitReal: ${e.message}")
                Toast.makeText(this@PostDetailActivity, "Error inesperado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Helper para encontrar el API ID de un comentario a partir del objeto Comment local
     */
    private fun findApiIdFromComment(comment: Comment): String? {
        return comment.apiId
    }

    // ============================================
    // MÉTODOS EXISTENTES (MANTENER COMPATIBILIDAD)
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
        val apiId = intent.getStringExtra(EXTRA_POST_API_ID)

        android.util.Log.d(TAG, "=== loadPostData ===")
        android.util.Log.d(TAG, "Post API ID: $apiId")

        if (!apiId.isNullOrEmpty()) {
            android.util.Log.d(TAG, "Cargando desde API con ID: $apiId")
            loadPostFromApi(apiId)
        } else {
            showError("Error: Se requiere ID de publicación válido")
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
                        android.util.Log.d(TAG, "Publicación cargada: ${publicacion.titulo}")

                        currentPost = convertirDetalleAPost(publicacion, currentUser?.id ?: "")
                        displayPostData(currentPost!!)

                        // Caramos el carrusel de multimedia
                        setupMediaCarousel(publicacion.multimedia)

                        // Cargar estado de reacciones y favoritos
                        if (currentUser != null) {
                            loadInitialReactionState(apiId, currentUser.id, token)
                            loadReactionCounts(apiId, token)
                            loadInitialFavoriteState(apiId, currentUser.id, token)
                        }

                        // Cargar comentarios reales
                        loadCommentsReal()
                    },
                    onFailure = { error ->
                        android.util.Log.e(TAG, "Error: ${error.message}")
                        showError("Error al cargar publicación: ${error.message}")
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Exception: ${e.message}")
                showError("Error inesperado: ${e.message}")
            } finally {
                setLoading(false)
            }
        }
    }

    private fun convertirDetalleAPost(detalle: PublicacionDetalle, currentUserId: String): Post {
        val imagenPreview = detalle.multimedia.firstOrNull { it.tipo == "imagen" }?.url ?: ""

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

        // Cargar imagen real desde Cloudinary
//        if (ImageUtils.isValidImageUrl(post.imageUrl)) {
//            android.util.Log.d(TAG, "Cargando imagen desde Cloudinary: ${post.imageUrl}")
//            ImageUtils.loadPostImage(
//                context = this,
//                imageUrl = post.imageUrl,
//                imageView = binding.ivPostImage1,
//                showPlaceholder = true
//            )
//            binding.ivPostImage2.visibility = View.GONE
//        } else {
//            android.util.Log.d(TAG, "URL no válida, usando placeholder")
//            binding.ivPostImage1.setImageResource(R.mipmap.ic_launcher)
//            binding.ivPostImage2.visibility = View.GONE
//        }

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

    // ============================================
    // MÉTODOS MOCK (FALLBACK Y COMPATIBILIDAD)
    // ============================================

    private fun loadCommentsMock() {
        if (comments.isEmpty()) {
            comments.clear()
            comments.addAll(Comment.getSampleComments())
            commentAdapter.notifyDataSetChanged()
            android.util.Log.d(TAG, "Comentarios mock cargados: ${comments.size}")
        }
    }

    private fun handleReplySubmitMock(parentComment: Comment, replyText: String) {
        val newReply = Comment.Reply(
            id = System.currentTimeMillis().toInt(),
            user = "Usuario Actual",
            text = replyText.trim(),
            timestamp = "Ahora",
            userLikeState = Comment.LikeState.NONE
        )

        parentComment.replies.add(newReply)
        Toast.makeText(this, "Respuesta agregada", Toast.LENGTH_SHORT).show()
        commentAdapter.notifyDataSetChanged()
    }

    // Manejo de likes en comentarios principales con API real
    private fun handleCommentLike(comment: Comment) {
        val currentUser = sessionManager.getCurrentUser()
        val token = sessionManager.getAccessToken()
        val commentApiId = comment.apiId

        if (currentUser == null || token == null || commentApiId == null) {
            Toast.makeText(this, "Error: Sesión no válida", Toast.LENGTH_SHORT).show()
            return
        }

        android.util.Log.d(TAG, "=== handleCommentLike ===")
        android.util.Log.d(TAG, "Comentario API ID: $commentApiId")

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    comentarioRepository.toggleLikeComentario(commentApiId, currentUser.id, token)
                }

                result.fold(
                    onSuccess = { estadoReaccion ->
                        android.util.Log.d(TAG, "Toggle like comentario exitoso: ${estadoReaccion.tipoReaccion}")

                        comment.userLikeState = when {
                            estadoReaccion.esLike -> Comment.LikeState.LIKED
                            estadoReaccion.esDislike -> Comment.LikeState.DISLIKED
                            else -> Comment.LikeState.NONE
                        }

                        val message = when {
                            estadoReaccion.esLike -> "Te gusta este comentario"
                            estadoReaccion.tieneReaccion -> "Te gusta este comentario"
                            else -> "Like removido"
                        }
                        Toast.makeText(this@PostDetailActivity, message, Toast.LENGTH_SHORT).show()
                        commentAdapter.notifyDataSetChanged()
                    },
                    onFailure = { error ->
                        android.util.Log.e(TAG, "Error en like comentario: ${error.message}")
                        Toast.makeText(this@PostDetailActivity, "Error al procesar like", Toast.LENGTH_SHORT).show()
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Exception en handleCommentLike: ${e.message}")
                Toast.makeText(this@PostDetailActivity, "Error inesperado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleCommentDislike(comment: Comment) {
        val currentUser = sessionManager.getCurrentUser()
        val token = sessionManager.getAccessToken()
        val commentApiId = comment.apiId

        if (currentUser == null || token == null || commentApiId == null) {
            Toast.makeText(this, "Error: Sesión no válida", Toast.LENGTH_SHORT).show()
            return
        }

        android.util.Log.d(TAG, "=== handleCommentDislike ===")
        android.util.Log.d(TAG, "Comentario API ID: $commentApiId")

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    comentarioRepository.toggleDislikeComentario(commentApiId, currentUser.id, token)
                }

                result.fold(
                    onSuccess = { estadoReaccion ->
                        android.util.Log.d(TAG, "Toggle dislike comentario exitoso: ${estadoReaccion.tipoReaccion}")

                        comment.userLikeState = when {
                            estadoReaccion.esLike -> Comment.LikeState.LIKED
                            estadoReaccion.esDislike -> Comment.LikeState.DISLIKED
                            else -> Comment.LikeState.NONE
                        }

                        val message = when {
                            estadoReaccion.esDislike -> "No te gusta este comentario"
                            estadoReaccion.tieneReaccion -> "No te gusta este comentario"
                            else -> "Dislike removido"
                        }
                        Toast.makeText(this@PostDetailActivity, message, Toast.LENGTH_SHORT).show()
                        commentAdapter.notifyDataSetChanged()
                    },
                    onFailure = { error ->
                        android.util.Log.e(TAG, "Error en dislike comentario: ${error.message}")
                        Toast.makeText(this@PostDetailActivity, "Error al procesar dislike", Toast.LENGTH_SHORT).show()
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Exception en handleCommentDislike: ${e.message}")
                Toast.makeText(this@PostDetailActivity, "Error inesperado", Toast.LENGTH_SHORT).show()
            }
        }
    }

}