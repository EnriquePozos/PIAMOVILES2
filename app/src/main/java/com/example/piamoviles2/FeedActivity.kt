package com.example.piamoviles2

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.piamoviles2.databinding.ActivityFeedBinding
import androidx.appcompat.app.AlertDialog
import com.example.piamoviles2.utils.SessionManager
import androidx.activity.OnBackPressedCallback

// ============================================
// IMPORTS PARA API INTEGRATION
// ============================================
import com.example.piamoviles2.data.repositories.PublicacionRepository
import kotlinx.coroutines.*

// Manejar si esta online o no
import com.example.piamoviles2.utils.NetworkMonitor

class FeedActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFeedBinding
    private lateinit var postAdapter: PostAdapter
    private lateinit var sessionManager: SessionManager

    private lateinit var networkMonitor: NetworkMonitor

    // ============================================
    // VARIABLES PARA API INTEGRATION
    // ============================================
    private lateinit var publicacionRepository: PublicacionRepository
    private var allPosts = mutableListOf<Post>()
    private var filteredPosts = mutableListOf<Post>()
    private var isLoading = false

    companion object {
        private const val TAG = "FEED_DEBUG"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityFeedBinding.inflate(layoutInflater)
        setContentView(binding.root)
        networkMonitor = NetworkMonitor(this)

        // ============================================
        // SETUP INICIAL
        // ============================================
        setupApiComponents()
        setupHeader()
        setupRecyclerView()
        setupSearchView()
        setupSwipeRefresh()
        setupClickListeners()
        setupBackPressedHandler()

        // ============================================
        // CARGAR DATOS DE LA API
        // ============================================
        loadPosts()

        android.util.Log.d(TAG, "FeedActivity iniciada")
    }

    private fun setupClickListeners() {
        binding.rvPosts.setOnLongClickListener {
            android.util.Log.d(TAG, "Click largo detectado en RecyclerView")
            showOptionsMenu()
            true
        }

        binding.root.setOnLongClickListener {
            android.util.Log.d(TAG, "Click largo detectado en root view")
            showOptionsMenu()
            true
        }

        binding.searchView.setOnLongClickListener {
            android.util.Log.d(TAG, "Click largo detectado en SearchView")
            showOptionsMenu()
            true
        }
    }
    private fun setupBackPressedHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Aquí decides qué hacer en back press
                showExitConfirmation() // Confirmar salir de la app
            }
        })
    }
    private fun showExitConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Salir de la aplicación")
            .setMessage("¿Quieres cerrar la aplicación?")
            .setPositiveButton("Salir") { _, _ ->
                finishAffinity() // Cerrar app
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    private fun logoutModal() {
        AlertDialog.Builder(this)
            .setTitle("Cerrar sesión")
            .setMessage("¿Estás seguro de que quieres cerrar sesión?")
            .setPositiveButton("Cerrar sesión") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    private fun performLogout() {
        sessionManager.logout()
        Toast.makeText(this, "Sesión cerrada exitosamente", Toast.LENGTH_SHORT).show()

        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }

    private fun showOptionsMenu() {
        val options = arrayOf(
            "Actualizar feed",
            "Crear nueva receta",
            "Mi perfil",
            "Cerrar sesión"
        )

        AlertDialog.Builder(this)
            .setTitle("Opciones")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> loadPosts()
                    1 -> startActivity(Intent(this, CreatePostActivity::class.java))
                    2 -> startActivity(Intent(this, ProfileActivity::class.java))
                    3 -> logoutModal()
                }
            }
            .show()
    }


    // ============================================
    // SETUP COMPONENTS
    // ============================================
    private fun setupApiComponents() {
        sessionManager = SessionManager(this)
        // Pasar context para habilitar funcionalidad offline
        publicacionRepository = PublicacionRepository(context = this)

        // Verificar sesión válida
        if (!sessionManager.isLoggedIn()) {
            Toast.makeText(this, "Sesión expirada. Por favor, inicia sesión nuevamente", Toast.LENGTH_LONG).show()
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
            return
        }
    }

    private fun setupHeader() {
        // Configurar header con icono de perfil
        val headerView = findViewById<android.view.View>(R.id.headerApp)
        HeaderUtils.setupHeaderWithProfile(headerView) {
            // Navegar a pantalla de perfil
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupRecyclerView() {

        postAdapter = PostAdapter { post ->
            // Navegar a detalle de la publicación
            android.util.Log.d(TAG, "Navegando a detalle: ${post.title}")
            android.util.Log.d(TAG, "API ID: ${post.apiId}")

            val intent = Intent(this, PostDetailActivity::class.java)
            intent.putExtra(PostDetailActivity.EXTRA_POST_ID, post.id)
            if (!post.apiId.isNullOrEmpty()) {
                intent.putExtra(PostDetailActivity.EXTRA_POST_API_ID, post.apiId)
            }

            if (!networkMonitor.isOnline()) {
                Toast.makeText(this, "Conectividad no disponible, conectatet a una red", Toast.LENGTH_SHORT).show()
                return@PostAdapter
            }
            startActivity(intent)
        }

        binding.rvPosts.apply {
            layoutManager = LinearLayoutManager(this@FeedActivity)
            adapter = postAdapter
        }
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterPosts(newText ?: "")
                return true
            }
        })
    }

    // ============================================
    // NUEVO: SETUP SWIPE REFRESH
    // ============================================
    private fun setupSwipeRefresh() {
        // Si tienes SwipeRefreshLayout en tu XML, descomenta esto:
        /*
        binding.swipeRefreshLayout.setOnRefreshListener {
            loadPosts()
        }
        */
    }

    // ============================================
    // LOAD POSTS (OFFLINE U ONLINE)
    // ============================================
    private fun loadPosts() {
        if (isLoading) {
            android.util.Log.d(TAG, "Ya está cargando, ignorando nueva petición")
            return
        }

        val token = sessionManager.getAccessToken()
        val currentUser = sessionManager.getCurrentUser()

        if (token == null || currentUser == null) {
            Toast.makeText(this, "Error: Sesión no válida", Toast.LENGTH_SHORT).show()
            return
        }

        setLoading(true)

        // Llamada a repository con corrutinas
        CoroutineScope(Dispatchers.Main).launch {
            try {
                android.util.Log.d(TAG, "Iniciando carga de publicaciones...")

                val result = withContext(Dispatchers.IO) {
                    // CAMBIO: Usar método que detecta automáticamente online/offline
                    publicacionRepository.obtenerFeedSegunConectividad(
                        token = token,
                        currentUserId = currentUser.id
                    )
                }

                result.fold(
                    onSuccess = { posts ->
                        android.util.Log.d(TAG, "  Feed cargado: ${posts.size} publicaciones")

                        // Actualizar listas
                        allPosts.clear()
                        allPosts.addAll(posts)

                        // Aplicar filtro actual si existe
                        val currentQuery = binding.searchView.query.toString()
                        if (currentQuery.isNotEmpty()) {
                            filterPosts(currentQuery)
                        } else {
                            filteredPosts.clear()
                            filteredPosts.addAll(allPosts)
                        }

                        updateUI()

                        // Toast solo si es la primera carga
                        if (posts.isNotEmpty()) {
                            Toast.makeText(this@FeedActivity, "Feed actualizado: ${posts.size} recetas", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onFailure = { error ->
                        android.util.Log.e(TAG, "  Error al cargar feed", error)
                        handleApiError(error)
                    }
                )

            } catch (e: Exception) {
                android.util.Log.e(TAG, "  Exception al cargar feed", e)
                Toast.makeText(this@FeedActivity, "Error inesperado: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                setLoading(false)
            }
        }
    }

    // ============================================
    // HANDLE API ERRORS
    // ============================================
    private fun handleApiError(error: Throwable) {
        when {
            error.message?.contains("404") == true -> {
                // No hay publicaciones
                Toast.makeText(this, "No hay publicaciones disponibles", Toast.LENGTH_SHORT).show()
                showEmptyState("No hay recetas aún", "¡Sé el primero en compartir una receta!")
            }
            error.message?.contains("401") == true || error.message?.contains("403") == true -> {
                // Error de autenticación
                Toast.makeText(this, "Sesión expirada. Por favor, inicia sesión nuevamente", Toast.LENGTH_LONG).show()
                sessionManager.logout()
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                finish()
            }
            error.message?.contains("timeout") == true || error.message?.contains("network") == true -> {
                // Error de conectividad
                Toast.makeText(this, "Error de conexión. Verifica tu internet", Toast.LENGTH_LONG).show()
                showEmptyState("Sin conexión", "Verifica tu conexión a internet y desliza para actualizar")
            }
            else -> {
                // Error genérico
                Toast.makeText(this, "Error al cargar publicaciones: ${error.message}", Toast.LENGTH_LONG).show()

                // Si no hay posts cargados, mostrar datos de ejemplo
                if (allPosts.isEmpty()) {
                    android.util.Log.w(TAG, "Cargando datos de ejemplo como fallback")
                    loadSampleDataAsFallback()
                }
            }
        }
    }

    // ============================================
    // FALLBACK CON DATOS DE EJEMPLO
    // ============================================
    private fun loadSampleDataAsFallback() {
        allPosts.clear()
        allPosts.addAll(Post.getSamplePosts())
        filteredPosts.clear()
        filteredPosts.addAll(allPosts)
        updateUI()

        Toast.makeText(this, "Mostrando datos de ejemplo", Toast.LENGTH_SHORT).show()
    }

    // ============================================
    // UI STATE MANAGEMENT
    // ============================================
    private fun setLoading(loading: Boolean) {
        isLoading = loading

        if (loading) {
            // Mostrar loading
            binding.rvPosts.visibility = View.GONE
            // Si tienes un ProgressBar: binding.progressBarLoading.visibility = View.VISIBLE
            android.util.Log.d(TAG, "Mostrando loading...")
        } else {
            // Ocultar loading
            // binding.progressBarLoading.visibility = View.GONE
            binding.rvPosts.visibility = View.VISIBLE
            // Si tienes SwipeRefreshLayout: binding.swipeRefreshLayout.isRefreshing = false
        }
    }

    private fun showEmptyState(title: String, subtitle: String) {
        // Si tienes layouts de estado vacío en tu XML, mostrarlos aquí
        // binding.layoutEmptyState.visibility = View.VISIBLE
        // binding.tvEmptyTitle.text = title
        // binding.tvEmptySubtitle.text = subtitle
        binding.rvPosts.visibility = View.GONE

        android.util.Log.d(TAG, "Mostrando estado vacío: $title - $subtitle")
    }

    private fun filterPosts(query: String) {
        filteredPosts.clear()

        if (query.isEmpty()) {
            filteredPosts.addAll(allPosts)
        } else {
            val searchQuery = query.lowercase()
            filteredPosts.addAll(
                allPosts.filter { post ->
                    post.title.lowercase().contains(searchQuery) ||
                            post.description.lowercase().contains(searchQuery) ||
                            post.author.lowercase().contains(searchQuery)
                }
            )
        }

        updateUI()
        android.util.Log.d(TAG, "Filtro aplicado: '$query' - ${filteredPosts.size} resultados")
    }

    private fun updateUI() {
        if (filteredPosts.isEmpty() && !isLoading) {
            // Estado vacío
            val query = binding.searchView.query.toString()
            if (query.isNotEmpty()) {
                showEmptyState("Sin resultados", "No se encontraron recetas para '$query'")
            } else {
                showEmptyState("No hay recetas", "¡Sé el primero en compartir una receta!")
            }
        } else {
            // Mostrar lista
            binding.rvPosts.visibility = View.VISIBLE
            // binding.layoutEmptyState.visibility = View.GONE
        }

        // Actualizar adapter
        postAdapter.submitList(filteredPosts.toList())
        android.util.Log.d(TAG, "UI actualizada: ${filteredPosts.size} posts mostrados")
    }

    // ============================================
    // LIFECYCLE METHODS
    // ============================================
    override fun onResume() {
        super.onResume()
        // Recargar datos cuando regrese a la pantalla
        // (ej: después de crear una nueva publicación)
        android.util.Log.d(TAG, "onResume - Recargando feed")
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cancelar corrutinas si es necesario
        android.util.Log.d(TAG, "FeedActivity destruida")
    }

    // ============================================
    // MÉTODOS AUXILIARES PARA DEBUGGING
    // ============================================
    private fun showDebugInfo() {
        val currentUser = sessionManager.getCurrentUser()
        val token = sessionManager.getAccessToken()

        android.util.Log.d(TAG, "=== DEBUG INFO ===")
        android.util.Log.d(TAG, "Usuario: ${currentUser?.alias}")
        android.util.Log.d(TAG, "Token existe: ${token != null}")
        android.util.Log.d(TAG, "Posts cargados: ${allPosts.size}")
        android.util.Log.d(TAG, "Posts filtrados: ${filteredPosts.size}")
        android.util.Log.d(TAG, "==================")
    }


}