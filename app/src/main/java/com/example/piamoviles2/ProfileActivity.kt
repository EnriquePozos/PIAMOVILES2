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
        binding.ivProfileAvatar.setImageResource(R.mipmap.ic_launcher)
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

        // Agregar receta
        binding.btnAddRecipe.setOnClickListener {
            Toast.makeText(this, "Agregar receta (próximamente)", Toast.LENGTH_SHORT).show()
            // TODO: Navegar a CreatePostActivity
        }

        // Ver borradores
        binding.btnViewDrafts.setOnClickListener {
            Toast.makeText(this, "Ver borradores (próximamente)", Toast.LENGTH_SHORT).show()
            // TODO: Navegar a DraftsActivity
        }

        // Ver favoritos
        binding.btnViewFavorites.setOnClickListener {
            Toast.makeText(this, "Ver favoritos (próximamente)", Toast.LENGTH_SHORT).show()
            // TODO: Navegar a FavoritesActivity
        }
    }

    private fun loadUserPosts() {
        // Obtener posts del usuario (simulando datos del usuario actual)
        userPosts.clear()
        userPosts.addAll(getUserPosts())
        updateUI()
    }

    private fun getUserPosts(): List<Post> {
        // Simular posts del usuario actual
        return listOf(
            Post(
                id = 1001,
                title = "Tacos al pastor",
                description = "Mi receta especial de tacos al pastor con ingredientes frescos y marinado casero.",
                imageUrl = "user_tacos",
                author = "@Pozos",
                createdAt = "Hace 1 día",
                isOwner = true,
                likesCount = 15,
                commentsCount = 8
            ),
            Post(
                id = 1002,
                title = "Desayuno saludable",
                description = "Bowl nutritivo perfecto para empezar el día con energía y vitalidad.",
                imageUrl = "user_breakfast",
                author = "@Pozos",
                createdAt = "Hace 3 días",
                isOwner = true,
                likesCount = 22,
                commentsCount = 12
            )
        )
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
}