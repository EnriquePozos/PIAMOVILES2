package com.example.piamoviles2

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.piamoviles2.databinding.ActivityFavoritesBinding
import com.example.piamoviles2.utils.SessionManager
import com.example.piamoviles2.data.repositories.PublicacionRepository
import kotlinx.coroutines.*

class FavoritesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFavoritesBinding
    private lateinit var postAdapter: PostAdapter
    private lateinit var sessionManager: SessionManager
    private lateinit var publicacionRepository: PublicacionRepository

    private var favoritePosts = mutableListOf<Post>()

    companion object {
        private const val TAG = "FAVORITES_DEBUG"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityFavoritesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializar componentes
        sessionManager = SessionManager(this)
        publicacionRepository = PublicacionRepository(this)

        setupHeader()
        setupRecyclerView()
        loadFavoritePostsReal()
    }

    private fun setupHeader() {
        val headerView = findViewById<View>(R.id.headerApp)
        HeaderUtils.setupHeaderWithBackButton(this, headerView)
    }

    private fun setupRecyclerView() {
        // Reutilizar PostAdapter existente con navegación hacia PostDetailActivity
        postAdapter = PostAdapter { post ->
            android.util.Log.d(TAG, "Click en favorito: ${post.title}")
            android.util.Log.d(TAG, "Post ID: ${post.id}, API ID: ${post.apiId}")

            // Navegar al detalle de la publicación
            val intent = Intent(this, PostDetailActivity::class.java)
            intent.putExtra(PostDetailActivity.EXTRA_POST_ID, post.id)
            if (!post.apiId.isNullOrEmpty()) {
                intent.putExtra(PostDetailActivity.EXTRA_POST_API_ID, post.apiId)
            }
            startActivity(intent)
        }

        binding.rvFavorites.apply {
            layoutManager = LinearLayoutManager(this@FavoritesActivity)
            adapter = postAdapter
        }
    }

    private fun loadFavoritePostsReal() {
        val currentUser = sessionManager.getCurrentUser()
        val token = sessionManager.getAccessToken()

        if (currentUser == null || token == null) {
            android.util.Log.e(TAG, "Error: Usuario o token no válido")
            Toast.makeText(this, "Error: Sesión no válida", Toast.LENGTH_SHORT).show()
            showEmptyState()
            return
        }

        setLoading(true)

        lifecycleScope.launch {
            try {
                android.util.Log.d(TAG, "=== Cargando favoritos del usuario ===")
                android.util.Log.d(TAG, "User ID: ${currentUser.id}")

                val result = withContext(Dispatchers.IO) {
                    publicacionRepository.obtenerFavoritosUsuarioConvertidos(
                        idUsuario = currentUser.id,
                        token = token
                    )
                }

                result.fold(
                    onSuccess = { favorites ->
                        android.util.Log.d(TAG, "  Favoritos cargados: ${favorites.size}")

                        favoritePosts.clear()
                        favoritePosts.addAll(favorites)
                        updateUI()

                        if (favorites.isEmpty()) {
                            android.util.Log.d(TAG, "  No hay favoritos para mostrar")
                        }
                    },
                    onFailure = { error ->
                        android.util.Log.e(TAG, "Error al cargar favoritos: ${error.message}")
                        Toast.makeText(
                            this@FavoritesActivity,
                            "Error al cargar favoritos: ${error.message}",
                            Toast.LENGTH_LONG
                        ).show()
                        showEmptyState()
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Exception al cargar favoritos: ${e.message}")
                Toast.makeText(
                    this@FavoritesActivity,
                    "Error inesperado: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                showEmptyState()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun updateUI() {
        if (favoritePosts.isEmpty()) {
            showEmptyState()
        } else {
            showFavoritesList()
        }

        // Actualizar adaptador
        postAdapter.submitList(favoritePosts.toList())
    }

    private fun showEmptyState() {
        binding.rvFavorites.visibility = View.GONE
        binding.layoutEmptyFavorites.visibility = View.VISIBLE
    }

    private fun showFavoritesList() {
        binding.rvFavorites.visibility = View.VISIBLE
        binding.layoutEmptyFavorites.visibility = View.GONE
    }

    private fun setLoading(loading: Boolean) {
        if (loading) {
            android.util.Log.d(TAG, "Mostrando loading...")
            // Mostrar indicador de carga
            binding.rvFavorites.visibility = View.GONE
            binding.layoutEmptyFavorites.visibility = View.GONE
            // Si tienes un layout de loading, mostrarlo aquí
            // binding.layoutLoading.visibility = View.VISIBLE
        } else {
            android.util.Log.d(TAG, "Ocultando loading...")
            // Ocultar indicador de carga
            // binding.layoutLoading.visibility = View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        android.util.Log.d(TAG, "onResume - Recargando favoritos")
        // Recargar favoritos cuando regrese a la pantalla
        // (en caso de que hayan cambiado desde PostDetailActivity)
        loadFavoritePostsReal()
    }
}