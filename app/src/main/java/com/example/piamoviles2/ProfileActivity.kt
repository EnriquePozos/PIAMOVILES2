package com.example.piamoviles2

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.semantics.text
//import androidx.glance.visibility
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.piamoviles2.databinding.ActivityProfileBinding
import com.example.piamoviles2.utils.SessionManager
import com.example.piamoviles2.utils.ImageUtils
import com.example.piamoviles2.data.repositories.PublicacionRepository
import kotlinx.coroutines.*
class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var postAdapter: PostAdapter
    private lateinit var sessionManager: SessionManager

    private lateinit var publicacionRepository: PublicacionRepository

    private var userPosts = mutableListOf<Post>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        publicacionRepository = PublicacionRepository()


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

            android.util.Log.d("PROFILE_DEBUG", "Mostrando perfil de: ${currentUser.alias}")
            android.util.Log.d("PROFILE_DEBUG", "URL imagen: ${currentUser.fotoPerfil}")
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
        binding.btnModifyProfile.setOnClickListener {
            val intent = Intent(this, EditProfileActivity::class.java)
            startActivity(intent)
        }

        binding.btnAddRecipe.setOnClickListener {
            val intent = Intent(this, CreatePostActivity::class.java)
            startActivity(intent)
        }

        binding.btnViewDrafts.setOnClickListener {
            val intent = Intent(this, DraftsActivity::class.java)
            startActivity(intent)
        }

        binding.btnViewFavorites.setOnClickListener {
            val intent = Intent(this, FavoritesActivity::class.java)
            startActivity(intent)
        }
    }


// CARGAR POSTS REALES

    private fun loadUserPosts() {
        val currentUser = sessionManager.getCurrentUser()
        val token = sessionManager.getAccessToken()

        if (currentUser == null || token == null) {
            android.util.Log.e("PROFILE_DEBUG", "  Error: Usuario o token no válido")
            Toast.makeText(this, "Error: Sesión no válida", Toast.LENGTH_SHORT).show()
            return
        }

        setLoadingPosts(true)

        // Llamada a API con corrutinas
        CoroutineScope(Dispatchers.Main).launch {
            try {
                android.util.Log.d("PROFILE_DEBUG", "=== Cargando publicaciones del usuario ===")
                android.util.Log.d("PROFILE_DEBUG", "User ID: ${currentUser.id}")

                val result = withContext(Dispatchers.IO) {
                    publicacionRepository.obtenerPublicacionesUsuarioConvertidas(
                        idAutor = currentUser.id,
                        incluirBorradores = false, // Solo publicaciones públicas
                        token = token
                    )
                }

                result.fold(
                    onSuccess = { posts ->
                        android.util.Log.d("PROFILE_DEBUG", "  Publicaciones cargadas: ${posts.size}")

                        userPosts.clear()
                        userPosts.addAll(posts)
                        updateUI()

                        if (posts.isEmpty()) {
                            android.util.Log.d("PROFILE_DEBUG", "Usuario no tiene publicaciones")
                        } else {
                            Toast.makeText(this@ProfileActivity, "${posts.size} recetas cargadas", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onFailure = { error ->
                        android.util.Log.e("PROFILE_DEBUG", "  Error al cargar publicaciones", error)
                        handlePostsError(error)
                    }
                )

            } catch (e: Exception) {
                android.util.Log.e("PROFILE_DEBUG", "  Exception al cargar publicaciones", e)
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
            android.util.Log.d("PROFILE_DEBUG", "Mostrando loading de posts...")
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
                // Error genérico - usar datos de ejemplo como fallback
                android.util.Log.w("PROFILE_DEBUG", "Error en API, usando datos de ejemplo")
                userPosts.clear()
                updateUI()
                //Toast.makeText(this, "Error al cargar publicaciones reales, mostrando ejemplos", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun loadSamplePostsAsFallback() {
        userPosts.clear()
        userPosts.addAll(getUserPosts()) // Método original con datos de ejemplo
        updateUI()
    }

    private fun getUserPosts(): List<Post> {
        return Post.getSamplePosts().filter { it.isOwner && !it.isDraft }
    }

    private fun updateUI() {
        android.util.Log.d("PROFILE_DEBUG", "Actualizando UI - Posts: ${userPosts.size}")

        if (userPosts.isEmpty()) {
            binding.rvUserPosts.visibility = View.GONE
            binding.layoutEmptyPosts.visibility = View.VISIBLE
            android.util.Log.d("PROFILE_DEBUG", "Mostrando estado vacío")
        } else {
            binding.rvUserPosts.visibility = View.VISIBLE
            android.util.Log.d("PROFILE_DEBUG", "Mostrando ${userPosts.size} publicaciones")

            //   LOGS PARA DEBUGGING:
            userPosts.forEachIndexed { index, post ->
                android.util.Log.d("PROFILE_DEBUG", "Post $index: ${post.title}")
            }
        }

        //   LOG ANTES DE submitList:
        android.util.Log.d("PROFILE_DEBUG", "Llamando submitList con ${userPosts.size} elementos")
        postAdapter.submitList(userPosts.toList())

        // Scroll al inicio si hay publicaciones
        if (userPosts.isNotEmpty()) {
            binding.rvUserPosts.scrollToPosition(0)
        }
    }

    override fun onResume() {
        super.onResume()
        //   RECARGAR DATOS AL REGRESAR (incluyendo imagen actualizada)
        setupUserInfo()
        loadUserPosts()
        android.util.Log.d("PROFILE_DEBUG", "ProfileActivity refrescada en onResume")
    }
}