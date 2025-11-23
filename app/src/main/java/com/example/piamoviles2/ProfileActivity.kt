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

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var postAdapter: PostAdapter
    private lateinit var sessionManager: SessionManager
    private var userPosts = mutableListOf<Post>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

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
            // ✅ CARGAR DATOS DEL USUARIO
            binding.tvUserAlias.text = currentUser.alias
            binding.tvUserEmail.text = currentUser.email

            // ✅ CARGAR IMAGEN CON GLIDE
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
            intent.putExtra(PostDetailActivity.EXTRA_POST_ID, post.id)
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

    private fun loadUserPosts() {
        userPosts.clear()
        userPosts.addAll(getUserPosts())
        updateUI()
    }

    private fun getUserPosts(): List<Post> {
        return Post.getSamplePosts().filter { it.isOwner && !it.isDraft }
    }

    private fun updateUI() {
        if (userPosts.isEmpty()) {
            binding.rvUserPosts.visibility = View.GONE
            // Si tienes un layout para posts vacíos, mostrarlo aquí
        } else {
            binding.rvUserPosts.visibility = View.VISIBLE
            // Si tienes un layout para posts vacíos, ocultarlo aquí
        }

        postAdapter.submitList(userPosts.toList())
    }

    override fun onResume() {
        super.onResume()
        // ✅ RECARGAR DATOS AL REGRESAR (incluyendo imagen actualizada)
        setupUserInfo()
        loadUserPosts()
        android.util.Log.d("PROFILE_DEBUG", "ProfileActivity refrescada en onResume")
    }
}