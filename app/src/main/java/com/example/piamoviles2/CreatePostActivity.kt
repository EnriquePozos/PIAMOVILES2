package com.example.piamoviles2

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.piamoviles2.databinding.ActivityCreatePostBinding
import java.text.SimpleDateFormat
import java.util.*

class CreatePostActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreatePostBinding
    private lateinit var imagePickerHelper: ImagePickerHelper

    // Control de imágenes
    private var selectedImages = mutableListOf<Bitmap?>()
    private var currentImageSlot = 0

    // Modo de edición
    private var isEditMode = false
    private var editingPostId = -1

    companion object {
        const val EXTRA_DRAFT_ID = "extra_draft_id"
        const val EXTRA_POST_ID = "extra_post_id"
        private const val MAX_IMAGES = 3
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCreatePostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeImages()
        setupHeader()
        setupImagePicker()
        setupClickListeners()
        setupBackPressedHandler()
        checkEditMode()
    }

    private fun initializeImages() {
        // Inicializar lista de imágenes
        repeat(MAX_IMAGES) {
            selectedImages.add(null)
        }
    }

    private fun setupHeader() {
        val headerView = findViewById<View>(R.id.headerApp)
        HeaderUtils.setupHeaderWithBackButton(this, headerView)
    }

    private fun setupImagePicker() {
        imagePickerHelper = ImagePickerHelper(this) { bitmap ->
            if (bitmap != null) {
                addImageToSlot(bitmap)
            } else {
                Toast.makeText(this, "Error al cargar la imagen", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupClickListeners() {
        // Botones de selección de imagen
        binding.btnCamera.setOnClickListener {
            if (hasAvailableImageSlot()) {
                imagePickerHelper.openCamera()
            } else {
                Toast.makeText(this, "Máximo 3 imágenes permitidas", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnGallery.setOnClickListener {
            if (hasAvailableImageSlot()) {
                imagePickerHelper.openGallery()
            } else {
                Toast.makeText(this, "Máximo 3 imágenes permitidas", Toast.LENGTH_SHORT).show()
            }
        }

        // Botones para agregar imágenes en slots específicos
        binding.layoutAddImage1.setOnClickListener { selectImageForSlot(0) }
        binding.layoutAddImage2.setOnClickListener { selectImageForSlot(1) }
        binding.layoutAddImage3.setOnClickListener { selectImageForSlot(2) }

        // Botones para remover imágenes
        binding.btnRemoveImage1.setOnClickListener { removeImage(0) }
        binding.btnRemoveImage2.setOnClickListener { removeImage(1) }
        binding.btnRemoveImage3.setOnClickListener { removeImage(2) }

        // Botones de acción
        binding.btnSaveDraft.setOnClickListener { saveDraft() }
        binding.btnPublish.setOnClickListener { publishPost() }
    }

    private fun setupBackPressedHandler() {
        // ✅ SOLUCIÓN: Usar OnBackPressedDispatcher moderno
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleBackNavigation()
            }
        })
    }

    private fun handleBackNavigation() {
        // Verificar si hay cambios sin guardar
        val title = binding.etRecipeTitle.text.toString().trim()
        val description = binding.etRecipeDescription.text.toString().trim()
        val hasImages = selectedImages.any { it != null }

        if (title.isNotEmpty() || description.isNotEmpty() || hasImages) {
            AlertDialog.Builder(this)
                .setTitle("¿Salir sin guardar?")
                .setMessage("Tienes cambios sin guardar. ¿Quieres guardar como borrador antes de salir?")
                .setPositiveButton("Guardar borrador") { _, _ ->
                    if (validateForm()) {
                        saveDraft()
                    } else {
                        finish()
                    }
                }
                .setNegativeButton("Salir sin guardar") { _, _ ->
                    finish()
                }
                .setNeutralButton("Cancelar", null)
                .show()
        } else {
            finish()
        }
    }

    private fun checkEditMode() {
        // Verificar si estamos editando un borrador o post existente
        editingPostId = intent.getIntExtra(EXTRA_DRAFT_ID, -1)
        if (editingPostId == -1) {
            editingPostId = intent.getIntExtra(EXTRA_POST_ID, -1)
        }

        if (editingPostId != -1) {
            isEditMode = true
            binding.tvScreenTitle.text = "Editar receta"
            loadPostData(editingPostId)
        }
    }

    private fun loadPostData(postId: Int) {
        // Buscar el post/borrador por ID
        val post = Post.getSamplePosts().find { it.id == postId }
        post?.let {
            binding.etRecipeTitle.setText(it.title)
            binding.etRecipeDescription.setText(it.description)

            // En una app real, aquí cargarías las imágenes desde URLs o storage local
            // Por ahora simulamos que tiene al menos una imagen
            // loadImageFromUrl(it.imageUrl, 0)
        }
    }

    private fun hasAvailableImageSlot(): Boolean {
        return selectedImages.any { it == null }
    }

    private fun selectImageForSlot(slot: Int) {
        if (selectedImages[slot] == null && hasAvailableImageSlot()) {
            currentImageSlot = slot
            showImageSourceDialog()
        }
    }

    private fun showImageSourceDialog() {
        AlertDialog.Builder(this)
            .setTitle("Seleccionar imagen")
            .setItems(arrayOf("Cámara", "Galería")) { _, which ->
                when (which) {
                    0 -> imagePickerHelper.openCamera()
                    1 -> imagePickerHelper.openGallery()
                }
            }
            .show()
    }

    private fun addImageToSlot(bitmap: Bitmap) {
        val resizedBitmap = ImagePickerHelper.resizeBitmap(bitmap)
        selectedImages[currentImageSlot] = resizedBitmap
        updateImageUI(currentImageSlot, resizedBitmap)
    }

    private fun updateImageUI(slot: Int, bitmap: Bitmap?) {
        when (slot) {
            0 -> {
                if (bitmap != null) {
                    binding.ivImage1.setImageBitmap(bitmap)
                    binding.ivImage1.visibility = View.VISIBLE
                    binding.layoutAddImage1.visibility = View.GONE
                    binding.btnRemoveImage1.visibility = View.VISIBLE
                } else {
                    binding.ivImage1.visibility = View.GONE
                    binding.layoutAddImage1.visibility = View.VISIBLE
                    binding.btnRemoveImage1.visibility = View.GONE
                }
            }
            1 -> {
                if (bitmap != null) {
                    binding.ivImage2.setImageBitmap(bitmap)
                    binding.ivImage2.visibility = View.VISIBLE
                    binding.layoutAddImage2.visibility = View.GONE
                    binding.btnRemoveImage2.visibility = View.VISIBLE
                } else {
                    binding.ivImage2.visibility = View.GONE
                    binding.layoutAddImage2.visibility = View.VISIBLE
                    binding.btnRemoveImage2.visibility = View.GONE
                }
            }
            2 -> {
                if (bitmap != null) {
                    binding.ivImage3.setImageBitmap(bitmap)
                    binding.ivImage3.visibility = View.VISIBLE
                    binding.layoutAddImage3.visibility = View.GONE
                    binding.btnRemoveImage3.visibility = View.VISIBLE
                } else {
                    binding.ivImage3.visibility = View.GONE
                    binding.layoutAddImage3.visibility = View.VISIBLE
                    binding.btnRemoveImage3.visibility = View.GONE
                }
            }
        }
    }

    private fun removeImage(slot: Int) {
        selectedImages[slot] = null
        updateImageUI(slot, null)
        Toast.makeText(this, "Imagen eliminada", Toast.LENGTH_SHORT).show()
    }

    private fun validateForm(): Boolean {
        val title = binding.etRecipeTitle.text.toString().trim()
        val description = binding.etRecipeDescription.text.toString().trim()

        if (title.isEmpty()) {
            binding.etRecipeTitle.error = "El título es obligatorio"
            binding.etRecipeTitle.requestFocus()
            return false
        }

        if (title.length < 3) {
            binding.etRecipeTitle.error = "El título debe tener al menos 3 caracteres"
            binding.etRecipeTitle.requestFocus()
            return false
        }

        if (description.isEmpty()) {
            binding.etRecipeDescription.error = "La descripción es obligatoria"
            binding.etRecipeDescription.requestFocus()
            return false
        }

        if (description.length < 10) {
            binding.etRecipeDescription.error = "La descripción debe tener al menos 10 caracteres"
            binding.etRecipeDescription.requestFocus()
            return false
        }

        return true
    }

    private fun saveDraft() {
        if (!validateForm()) return

        val title = binding.etRecipeTitle.text.toString().trim()
        val description = binding.etRecipeDescription.text.toString().trim()

        // Crear/actualizar borrador
        val draftPost = createPostFromForm(title, description, isDraft = true)

        // TODO: En una app real, guardarías en base de datos local
        // draftRepository.saveDraft(draftPost)

        Toast.makeText(this, "Borrador guardado correctamente", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun publishPost() {
        if (!validateForm()) return

        val title = binding.etRecipeTitle.text.toString().trim()
        val description = binding.etRecipeDescription.text.toString().trim()

        // Validar que tenga al menos una imagen para publicar
        if (selectedImages.all { it == null }) {
            Toast.makeText(this, "Agrega al menos una imagen para publicar", Toast.LENGTH_SHORT).show()
            return
        }

        // Crear/actualizar publicación
        val post = createPostFromForm(title, description, isDraft = false)

        // TODO: En una app real, subirías al servidor
        // postRepository.publishPost(post)

        Toast.makeText(this, "¡Receta publicada exitosamente!", Toast.LENGTH_LONG).show()

        // Regresar al feed o perfil
        val intent = Intent(this, FeedActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
        finish()
    }

    private fun createPostFromForm(title: String, description: String, isDraft: Boolean): Post {
        val currentTime = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        val timestamp = if (isDraft) "Guardado $currentTime" else currentTime

        return Post(
            id = if (isEditMode) editingPostId else generateNewId(),
            title = title,
            description = description,
            imageUrl = "user_recipe_${System.currentTimeMillis()}", // En una app real serían URLs
            author = "@Pozos", // En una app real sería el usuario actual
            createdAt = timestamp,
            isOwner = true,
            isFavorite = false,
            isDraft = isDraft,
            likesCount = 0,
            commentsCount = 0
        )
    }

    private fun generateNewId(): Int {
        // Generar ID único (en una app real sería manejado por la base de datos)
        return (3000..9999).random()
    }
}