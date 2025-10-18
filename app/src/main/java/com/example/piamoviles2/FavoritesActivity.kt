package com.example.piamoviles2

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.piamoviles2.databinding.ActivityFavoritesBinding

class FavoritesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFavoritesBinding
    private lateinit var postAdapter: PostAdapter
    private var favoritePosts = mutableListOf<Post>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityFavoritesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupHeader()
        setupRecyclerView()
        loadFavoritePosts()
    }

    private fun setupHeader() {
        val headerView = findViewById<View>(R.id.headerApp)
        HeaderUtils.setupHeaderWithBackButton(this, headerView)
    }

    private fun setupRecyclerView() {
        // Reutilizar PostAdapter existente
        postAdapter = PostAdapter { post ->
            // Navegar al detalle de la publicación
            val intent = Intent(this, PostDetailActivity::class.java)
            intent.putExtra(PostDetailActivity.EXTRA_POST_ID, post.id)
            startActivity(intent)
        }

        binding.rvFavorites.apply {
            layoutManager = LinearLayoutManager(this@FavoritesActivity)
            adapter = postAdapter
        }
    }

    private fun loadFavoritePosts() {
        // Filtrar solo las publicaciones marcadas como favoritas
        favoritePosts.clear()
        favoritePosts.addAll(getAllFavoritePosts())
        updateUI()
    }

    private fun getAllFavoritePosts(): List<Post> {
        // Obtener todos los posts y filtrar solo los favoritos
        return Post.getSamplePosts().filter { it.isFavorite }
    }

    private fun updateUI() {
        if (favoritePosts.isEmpty()) {
            // Mostrar estado vacío
            binding.rvFavorites.visibility = View.GONE
            binding.layoutEmptyFavorites.visibility = View.VISIBLE
            //binding.tvFavoritesCount.text = "0 recetas"
        } else {
            // Mostrar lista de favoritos
            binding.rvFavorites.visibility = View.VISIBLE
            binding.layoutEmptyFavorites.visibility = View.GONE

            val count = favoritePosts.size
            //binding.tvFavoritesCount.text = if (count == 1) "1 receta" else "$count recetas"
        }

        // Actualizar adaptador
        postAdapter.submitList(favoritePosts.toList())
    }

    override fun onResume() {
        super.onResume()
        // Recargar favoritos cuando regrese a la pantalla
        // (en caso de que hayan cambiado desde otras pantallas)
        loadFavoritePosts()
    }
}