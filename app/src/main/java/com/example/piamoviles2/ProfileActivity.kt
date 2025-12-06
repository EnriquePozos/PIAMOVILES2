package com.example.piamoviles2

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.piamoviles2.databinding.ActivityProfileBinding
import com.example.piamoviles2.utils.SessionManager
import com.example.piamoviles2.utils.ImageUtils
import com.example.piamoviles2.data.repositories.PublicacionRepository
import kotlinx.coroutines.*

// Manejar si esta online o no
import com.example.piamoviles2.utils.NetworkMonitor

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var postAdapter: PostAdapter
    private lateinit var sessionManager: SessionManager

    private lateinit var publicacionRepository: PublicacionRepository

    private lateinit var networkMonitor: NetworkMonitor

    private var userPosts = mutableListOf<Post>()

    companion object {
        private const val TAG = "PROFILE_DEBUG"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        // CAMBIO: Agregar context para habilitar funcionalidad offline
        publicacionRepository = PublicacionRepository(context = this)
        networkMonitor = NetworkMonitor(this)


        setupHeader()
        setupUserInfo()
        setupRecyclerView()
        setupClickListeners()
        loadUserPosts()
    }

    private fun setupHeader() {
        val headerView = findViewById<View>(R.id.headerApp)
        HeaderUtils.setupHeaderWithBackButton(this, headerView)
    }

    private fun setupUserInfo() {
        val currentUser = sessionManager.getCurrentUser()

        if (currentUser != null) {
            //   CARGAR DATOS DEL USUARIO
            binding.tvUserAlias.text = currentUser.alias
            binding.tvUserEmail.text = currentUser.email

            //   CARGAR IMAGEN CON GLIDE
            ImageUtils.loadProfileImage(
                context = this,
                imageUrl = currentUser.fotoPerfil,
                circleImageView = binding.ivProfileAvatar,
                showPlaceholder = true
            )

            android.util.Log.d(TAG, "Mostrando perfil de: ${currentUser.alias}")
            android.util.Log.d(TAG, "URL imagen: ${currentUser.fotoPerfil}")
        } else {
            Toast.makeText(this, "Error al cargar perfil", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupRecyclerView() {
        postAdapter = PostAdapter { post ->
            val intent = Intent(this, PostDetailActivity::class.java)
            intent.putExtra(PostDetailActivity.EXTRA_POST_API_ID, post.apiId)
            //intent.putExtra(PostDetailActivity.EXTRA_POST_ID, post.id)
            startActivity(intent)
        }

        binding.rvUserPosts.apply {
            layoutManager = LinearLayoutManager(this@ProfileActivity)
            adapter = postAdapter
        }
    }

    private fun setupClickListeners() {

        if (!networkMonitor.isOnline()) {
            // 🚫 MODO OFFLINE - Deshabilitar funciones que requieren internet

            binding.btnModifyProfile.setOnClickListener {
                Toast.makeText(this, "Funcionalidad no disponible en modo offline", Toast.LENGTH_SHORT).show()
                android.util.Log.d(TAG, "Botón Modificar Perfil deshabilitado - Modo offline")
            }
            binding.btnModifyProfile.alpha = 0.5f
            binding.btnModifyProfile.isEnabled = false

            binding.btnViewFavorites.setOnClickListener {
                Toast.makeText(this, "Funcionalidad no disponible en modo offline", Toast.LENGTH_SHORT).show()
                android.util.Log.d(TAG, "Botón Ver Favoritos deshabilitado - Modo offline")
            }
            binding.btnViewFavorites.alpha = 0.5f
            binding.btnViewFavorites.isEnabled = false

            android.util.Log.d(TAG, "Modo OFFLINE - Botones de internet deshabilitados")

        } else {
            // ✅ MODO ONLINE - Habilitar todas las funciones

            binding.btnModifyProfile.setOnClickListener {
                val intent = Intent(this, EditProfileActivity::class.java)
                startActivity(intent)
            }
            binding.btnModifyProfile.alpha = 1.0f
            binding.btnModifyProfile.isEnabled = true

            binding.btnViewFavorites.setOnClickListener {
                val intent = Intent(this, FavoritesActivity::class.java)
                startActivity(intent)
            }
            binding.btnViewFavorites.alpha = 1.0f
            binding.btnViewFavorites.isEnabled = true

            android.util.Log.d(TAG, "Modo ONLINE - Todos los botones habilitados")
        }

        // ✅ MANTENER HABILITADO: Agregar receta
        binding.btnAddRecipe.setOnClickListener {
            val intent = Intent(this, CreatePostActivity::class.java)
            startActivity(intent)
        }

        // ✅ MANTENER HABILITADO: Ver borradores
        binding.btnViewDrafts.setOnClickListener {
            val intent = Intent(this, DraftsActivity::class.java)
            startActivity(intent)
        }

    }

    // CARGAR PUBLICACIONES - AHORA CON SOPORTE OFFLINE/ONLINE AUTOMÁTICO
    private fun loadUserPosts() {
        val currentUser = sessionManager.getCurrentUser()
        val token = sessionManager.getAccessToken()

        if (currentUser == null || token == null) {
            android.util.Log.e(TAG, "Error: Usuario o token no válido")
            Toast.makeText(this, "Error: Sesión no válida", Toast.LENGTH_SHORT).show()
            return
        }

        setLoadingPosts(true)

        // Llamada a repository con corrutinas
        CoroutineScope(Dispatchers.Main).launch {
            try {
                android.util.Log.d(TAG, "=== Cargando publicaciones del usuario ===")
                android.util.Log.d(TAG, "User ID: ${currentUser.id}")

                val result = withContext(Dispatchers.IO) {
                    // CAMBIO: Usar método que detecta automáticamente online/offline
                    publicacionRepository.obtenerPublicacionesUsuarioSegunConectividad(
                        idAutor = currentUser.id,
                        incluirBorradores = false, // Solo publicaciones públicas
                        token = token
                    )
                }

                result.fold(
                    onSuccess = { posts ->
                        android.util.Log.d(TAG, "Publicaciones cargadas: ${posts.size}")

                        userPosts.clear()
                        userPosts.addAll(posts)
                        updateUI()

                        if (posts.isEmpty()) {
                            android.util.Log.d(TAG, "Usuario no tiene publicaciones")
                        } else {
                            Toast.makeText(this@ProfileActivity, "${posts.size} recetas cargadas", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onFailure = { error ->
                        android.util.Log.e(TAG, "Error al cargar publicaciones", error)
                        handlePostsError(error)
                    }
                )

            } catch (e: Exception) {
                android.util.Log.e(TAG, "Exception al cargar publicaciones", e)
                Toast.makeText(this@ProfileActivity, "Error inesperado: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                setLoadingPosts(false)
            }
        }
    }

    // NUEVOS MÉTODOS DE SOPORTE
    private fun setLoadingPosts(loading: Boolean) {
        if (loading) {
            // Mostrar loading en el área de posts
            binding.rvUserPosts.visibility = View.GONE
            // Si tienes un ProgressBar para posts: binding.progressBarPosts.visibility = View.VISIBLE
            android.util.Log.d(TAG, "Mostrando loading de posts...")
        } else {
            // Ocultar loading
            // binding.progressBarPosts.visibility = View.GONE
            binding.rvUserPosts.visibility = View.VISIBLE
        }
    }

    private fun handlePostsError(error: Throwable) {
        when {
            error.message?.contains("404") == true -> {
                // Usuario no tiene publicaciones
                userPosts.clear()
                updateUI()
                Toast.makeText(this, "Aún no tienes publicaciones", Toast.LENGTH_SHORT).show()
            }
            error.message?.contains("401") == true || error.message?.contains("403") == true -> {
                // Error de autenticación
                Toast.makeText(this, "Error de autenticación", Toast.LENGTH_SHORT).show()
            }
            else -> {
                // Error genérico
                android.util.Log.w(TAG, "Error en carga de publicaciones: ${error.message}")
                userPosts.clear()
                updateUI()
                Toast.makeText(this, "Error al cargar publicaciones", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateUI() {
        android.util.Log.d(TAG, "Actualizando UI - Posts: ${userPosts.size}")

        if (userPosts.isEmpty()) {
            binding.rvUserPosts.visibility = View.GONE
            binding.layoutEmptyPosts.visibility = View.VISIBLE
            android.util.Log.d(TAG, "Mostrando estado vacío")
        } else {
            binding.rvUserPosts.visibility = View.VISIBLE
            binding.layoutEmptyPosts.visibility = View.GONE // AGREGADO: Ocultar estado vacío
            android.util.Log.d(TAG, "Mostrando ${userPosts.size} publicaciones")

            // LOGS PARA DEBUGGING:
            userPosts.forEachIndexed { index, post ->
                android.util.Log.d(TAG, "Post $index: ${post.title}")
                android.util.Log.d(TAG, "      Es Draft: ${post.isDraft}, API ID: ${post.apiId}")
            }
        }

        // LOG ANTES DE submitList:
        android.util.Log.d(TAG, "Llamando submitList con ${userPosts.size} elementos")
        postAdapter.submitList(userPosts.toList())

        // Scroll al inicio si hay publicaciones
        if (userPosts.isNotEmpty()) {
            binding.rvUserPosts.scrollToPosition(0)
        }
    }

    override fun onResume() {
        super.onResume()
        // RECARGAR DATOS AL REGRESAR (incluyendo imagen actualizada)
        setupUserInfo()
        loadUserPosts()
        android.util.Log.d(TAG, "ProfileActivity refrescada en onResume")
    }
}