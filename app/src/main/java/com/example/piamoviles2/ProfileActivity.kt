package com.example.piamoviles2

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.piamoviles2.databinding.ActivityProfileBinding

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var postAdapter: PostAdapter
    private var userPosts = mutableListOf<Post>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
        // Información de ejemplo del usuario
        binding.tvUserAlias.text = "@Pozos"
        binding.tvUserEmail.text = "kikepozos@gmail.com"

        // Avatar (por ahora usando el launcher icon)
        binding.ivProfileAvatar.setImageResource(R.mipmap.ic_foto_perfil_round)
    }

    private fun setupRecyclerView() {
        // Reutilizar PostAdapter existente
        postAdapter = PostAdapter { post ->
            // Navegar al detalle de la publicación
            val intent = Intent(this, PostDetailActivity::class.java)
            intent.putExtra(PostDetailActivity.EXTRA_POST_ID, post.id)
            startActivity(intent)
        }

        binding.rvUserPosts.apply {
            layoutManager = LinearLayoutManager(this@ProfileActivity)
            adapter = postAdapter
        }
    }

    private fun setupClickListeners() {
        // Modificar perfil
        binding.btnModifyProfile.setOnClickListener {
            val intent = Intent(this, EditProfileActivity::class.java)
            startActivity(intent)
        }

        // Agregar receta - ✅ CONECTADO A CreatePostActivity
        binding.btnAddRecipe.setOnClickListener {
            val intent = Intent(this, CreatePostActivity::class.java)
            startActivity(intent)
        }

        // Ver borradores - ✅ CONECTADO A DraftsActivity
        binding.btnViewDrafts.setOnClickListener {
            val intent = Intent(this, DraftsActivity::class.java)
            startActivity(intent)
        }

        // Ver favoritos - ✅ CONECTADO A FavoritesActivity
        binding.btnViewFavorites.setOnClickListener {
            val intent = Intent(this, FavoritesActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loadUserPosts() {
        // Obtener posts del usuario (simulando datos del usuario actual)
        userPosts.clear()
        userPosts.addAll(getUserPosts())
        updateUI()
    }

    private fun getUserPosts(): List<Post> {
        // Simular posts del usuario actual (solo publicados, no borradores)
        return Post.getSamplePosts().filter { it.isOwner && !it.isDraft }
    }

    private fun updateUI() {
        if (userPosts.isEmpty()) {
            binding.rvUserPosts.visibility = View.GONE
            binding.layoutEmptyPosts.visibility = View.VISIBLE
        } else {
            binding.rvUserPosts.visibility = View.VISIBLE
            binding.layoutEmptyPosts.visibility = View.GONE
        }

        postAdapter.submitList(userPosts.toList())
    }

    override fun onResume() {
        super.onResume()
        // Recargar posts cuando regrese a la pantalla
        // (en caso de que hayan agregado nuevas recetas)
        loadUserPosts()
    }
}