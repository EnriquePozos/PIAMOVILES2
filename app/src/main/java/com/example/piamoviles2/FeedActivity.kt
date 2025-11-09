package com.example.piamoviles2

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.piamoviles2.databinding.ActivityFeedBinding
import androidx.appcompat.app.AlertDialog
import com.example.piamoviles2.utils.SessionManager

class FeedActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFeedBinding
    private lateinit var postAdapter: PostAdapter

    private lateinit var sessionManager: SessionManager

    private var allPosts = mutableListOf<Post>()
    private var filteredPosts = mutableListOf<Post>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityFeedBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupHeader()
        setupRecyclerView()
        setupSearchView()
        //setupClickListeners()
        loadPosts()

        sessionManager = SessionManager(this)

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
            Toast.makeText(this, "Ver detalle: ${post.title}", Toast.LENGTH_SHORT).show()
            // TODO: Navegar a PostDetailActivity
            val intent = Intent(this, PostDetailActivity::class.java)
            intent.putExtra(PostDetailActivity.EXTRA_POST_ID, post.id)
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

//    private fun setupClickListeners() {
//        // Botón para crear nueva publicación
//        binding.fabCreatePost.setOnClickListener {
//            Toast.makeText(this, "Crear nueva publicación (próximamente)", Toast.LENGTH_SHORT).show()
//            // TODO: Navegar a CreatePostActivity
//        }
//    }

    private fun loadPosts() {
        // Cargar datos de ejemplo
        allPosts.clear()
        allPosts.addAll(Post.getSamplePosts())

        filteredPosts.clear()
        filteredPosts.addAll(allPosts)

        updateUI()
    }

    private fun filterPosts(query: String) {
        filteredPosts.clear()

        if (query.isEmpty()) {
            filteredPosts.addAll(allPosts)
        } else {
            filteredPosts.addAll(
                allPosts.filter { post ->
                    post.title.contains(query, ignoreCase = true) ||
                            post.description.contains(query, ignoreCase = true) ||
                            post.author.contains(query, ignoreCase = true)
                }
            )
        }

        updateUI()
    }

    private fun updateUI() {
        if (filteredPosts.isEmpty()) {
            binding.rvPosts.visibility = android.view.View.GONE
            binding.layoutEmptyState.visibility = android.view.View.VISIBLE
        } else {
            binding.rvPosts.visibility = android.view.View.VISIBLE
            binding.layoutEmptyState.visibility = android.view.View.GONE
        }

        postAdapter.submitList(filteredPosts.toList())
    }

//    override fun onBackPressed() {
//        // En el feed principal, salir de la app o volver al login
//        super.onBackPressed()
//        finishAffinity() // Cierra toda la app
//    }
@Deprecated("Deprecated in Java")
override fun onBackPressed() {
    showLogoutConfirmDialog()
}

    private fun showLogoutConfirmDialog() {
        AlertDialog.Builder(this)
            .setTitle("Cerrar sesión")
            .setMessage("¿Estás seguro de que quieres cerrar sesión?")
            .setPositiveButton("Cerrar sesión") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .setNeutralButton("Salir de la app") { _, _ ->
                // Cerrar la app completamente
                finishAffinity()
            }
            .show()
    }

    private fun performLogout() {
        // Limpiar sesión
        sessionManager.logout()

        // Log para debugging
        android.util.Log.d("LOGOUT_DEBUG", "Sesión limpiada, navegando al login")

        // Mostrar mensaje
        Toast.makeText(this, "Sesión cerrada exitosamente", Toast.LENGTH_SHORT).show()

        // Navegar al login y limpiar stack
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}