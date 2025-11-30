package com.example.piamoviles2

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.piamoviles2.databinding.ActivityCreatePostBinding
import java.text.SimpleDateFormat
import java.util.*
import com.example.piamoviles2.data.repositories.PublicacionRepository
import com.example.piamoviles2.utils.SessionManager
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import java.io.InputStream
import java.net.URL

class CreatePostActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreatePostBinding
    private lateinit var imagePickerHelper: ImagePickerHelper

    // Control de imágenes
    private var selectedImages = mutableListOf<Bitmap?>()
    private var currentImageSlot = 0

    // Modo de edición
    private var isEditMode = false
    private var editingPostId = -1
    private var editingDraftApiId: String? = null // API ID del borrador a editar

    // ============================================
    // VARIABLES PARA API INTEGRATION
    // ============================================
    private lateinit var publicacionRepository: PublicacionRepository
    private lateinit var sessionManager: SessionManager
    private val selectedImageFiles = mutableListOf<File>()
    private var isLoading = false

    companion object {
        const val EXTRA_DRAFT_ID = "extra_draft_id"
        const val EXTRA_POST_ID = "extra_post_id"
        const val EXTRA_DRAFT_API_ID = "extra_draft_api_id" // Para recibir API ID del borrador
        private const val MAX_IMAGES = 3
        private const val TAG = "CREATE_POST_DEBUG"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCreatePostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //   CÓDIGO EXISTENTE
        initializeImages()
        setupHeader()
        setupImagePicker()
        setupClickListeners()
        setupBackPressedHandler()

        // ============================================
        //   NUEVAS FUNCIONALIDADES API
        // ============================================
        setupApiComponents()
        loadUserData()

        //   Verificar modo de edición DESPUÉS de configurar API
        checkEditMode()

        android.util.Log.d(TAG, "CreatePostActivity iniciada")
    }

    // ============================================
    // NUEVOS MÉTODOS DE SETUP
    // ============================================
    private fun setupApiComponents() {
        publicacionRepository = PublicacionRepository()
        sessionManager = SessionManager(this)
    }

    private fun loadUserData() {
        if (!sessionManager.isLoggedIn()) {
            Toast.makeText(this, "Error: Sesión no válida", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val token = sessionManager.getAccessToken()
        val currentUser = sessionManager.getCurrentUser()

        android.util.Log.d(TAG, "Token: ${if (token?.isNotEmpty() == true) "  Cargado" else "  Vacío"}")
        android.util.Log.d(TAG, "User ID: ${currentUser?.id}")
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
                    cleanupAndFinish()
                }
                .setNeutralButton("Cancelar", null)
                .show()
        } else {
            finish()
        }
    }

    // ============================================
    //   MÉTODO CHECKEEDITMODE MEJORADO
    // ============================================
    private fun checkEditMode() {
        // Verificar si estamos editando un borrador (prioridad al API ID)
        editingDraftApiId = intent.getStringExtra(EXTRA_DRAFT_API_ID)

        if (editingDraftApiId != null) {
            // Editar borrador usando API ID
            isEditMode = true
            binding.tvScreenTitle.text = "Editar receta"
            android.util.Log.d(TAG, "Modo edición: Borrador API ID = $editingDraftApiId")
            loadDraftFromApi(editingDraftApiId!!)
            return
        }

        // Verificar ID local (legacy)
        editingPostId = intent.getIntExtra(EXTRA_DRAFT_ID, -1)
        if (editingPostId == -1) {
            editingPostId = intent.getIntExtra(EXTRA_POST_ID, -1)
        }

        if (editingPostId != -1) {
            isEditMode = true
            binding.tvScreenTitle.text = "Editar receta"
            android.util.Log.d(TAG, "Modo edición: Post local ID = $editingPostId")
            loadPostData(editingPostId)
        } else {
            // Modo creación
            android.util.Log.d(TAG, "Modo creación: Nueva receta")
        }
    }

    // ============================================
    //   CARGAR BORRADOR DESDE API
    // ============================================
    private fun loadDraftFromApi(apiId: String) {
        val token = sessionManager.getAccessToken()

        if (token == null) {
            Toast.makeText(this, "Sesión no válida", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        android.util.Log.d(TAG, "=== Cargando borrador desde API ===")
        android.util.Log.d(TAG, "API ID: $apiId")

        setLoading(true)

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    publicacionRepository.obtenerPublicacionPorId(apiId, token)
                }

                result.fold(
                    onSuccess = { publicacion ->
                        android.util.Log.d(TAG, "   Borrador cargado: ${publicacion.titulo}")

                        // Cargar datos en la interfaz
                        binding.etRecipeTitle.setText(publicacion.titulo)
                        binding.etRecipeDescription.setText(publicacion.descripcion)

                        // Cargar imágenes si existen
                        publicacion.multimedia?.let { multimedia ->
                            loadImagesFromUrls(multimedia.map { it.url })
                        }

                        // Actualizar botones para modo edición
                        updateUIForEditMode()

                    },
                    onFailure = { error ->
                        android.util.Log.e(TAG, "  Error al cargar borrador", error)
                        Toast.makeText(this@CreatePostActivity, "Error al cargar borrador: ${error.message}", Toast.LENGTH_LONG).show()
                        finish()
                    }
                )

            } catch (e: Exception) {
                android.util.Log.e(TAG, "  Exception al cargar borrador", e)
                Toast.makeText(this@CreatePostActivity, "Error inesperado: ${e.message}", Toast.LENGTH_LONG).show()
                finish()
            } finally {
                setLoading(false)
            }
        }
    }

    // ============================================
    //   CARGAR IMÁGENES DESDE URLs
    // ============================================
    private fun loadImagesFromUrls(imageUrls: List<String>) {
        android.util.Log.d(TAG, "=== Cargando ${imageUrls.size} imágenes desde URLs ===")

        imageUrls.forEachIndexed { index, url ->
            if (index < MAX_IMAGES) {
                android.util.Log.d(TAG, "Cargando imagen $index: $url")

                Glide.with(this)
                    .asBitmap()
                    .load(url)
                    .into(object : CustomTarget<Bitmap>() {
                        override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                            android.util.Log.d(TAG, "   Imagen $index cargada exitosamente")
                            selectedImages[index] = resource
                            updateImageUI(index, resource)

                            // Convertir a archivo para futuras actualizaciones
                            convertBitmapToFile(resource, index)
                        }

                        override fun onLoadCleared(placeholder: android.graphics.drawable.Drawable?) {
                            // No hacer nada
                        }

                        override fun onLoadFailed(errorDrawable: android.graphics.drawable.Drawable?) {
                            android.util.Log.e(TAG, "  Error al cargar imagen $index desde $url")
                        }
                    })
            }
        }
    }

    // ============================================
    //   ACTUALIZAR UI PARA MODO EDICIÓN
    // ============================================
    private fun updateUIForEditMode() {
        // Cambiar texto de botones para modo edición
        binding.btnSaveDraft.text = "Actualizar borrador"
        binding.btnPublish.text = "Publicar receta"

        android.util.Log.d(TAG, "UI actualizada para modo edición")
    }

    private fun loadPostData(postId: Int) {
        // Método legacy para compatibilidad
        val post = Post.getSamplePosts().find { it.id == postId }
        post?.let {
            binding.etRecipeTitle.setText(it.title)
            binding.etRecipeDescription.setText(it.description)
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

        // ============================================
        //   NUEVO: CONVERTIR BITMAP A FILE PARA API
        // ============================================
        convertBitmapToFile(resizedBitmap, currentImageSlot)
    }

    // ============================================
    // NUEVO MÉTODO PARA CONVERTIR BITMAP A FILE
    // ============================================
    private fun convertBitmapToFile(bitmap: Bitmap, slot: Int) {
        try {
            // Crear archivo temporal
            val tempFile = File(cacheDir, "image_slot_${slot}_${System.currentTimeMillis()}.jpg")

            // Convertir bitmap a archivo
            FileOutputStream(tempFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }

            // Agregar a la lista de archivos (reemplazar si ya existe para este slot)
            // Primero remover archivo anterior del slot si existe
            selectedImageFiles.removeAll { file ->
                if (file.name.contains("image_slot_${slot}_")) {
                    file.delete() // Eliminar archivo anterior
                    true
                } else {
                    false
                }
            }

            // Agregar nuevo archivo
            selectedImageFiles.add(tempFile)

            android.util.Log.d(TAG, "  Imagen convertida a archivo: ${tempFile.name}")
            android.util.Log.d(TAG, "Total archivos: ${selectedImageFiles.size}")

        } catch (e: Exception) {
            android.util.Log.e(TAG, "  Error al convertir bitmap a archivo", e)
        }
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

        // ============================================
        //   NUEVO: REMOVER ARCHIVO TAMBIÉN
        // ============================================
        selectedImageFiles.removeAll { file ->
            if (file.name.contains("image_slot_${slot}_")) {
                file.delete() // Eliminar archivo del cache
                android.util.Log.d(TAG, "Archivo eliminado: ${file.name}")
                true
            } else {
                false
            }
        }

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

    // ============================================
    //   MÉTODO SAVEEDRAFT MEJORADO (CREAR/ACTUALIZAR)
    // ============================================
    private fun saveDraft() {
        if (!validateForm() || isLoading) return

        val title = binding.etRecipeTitle.text.toString().trim()
        val description = binding.etRecipeDescription.text.toString().trim()

        val currentUser = sessionManager.getCurrentUser()
        val token = sessionManager.getAccessToken()

        if (currentUser == null || token == null) {
            Toast.makeText(this, "Sesión no válida", Toast.LENGTH_SHORT).show()
            return
        }

        setLoading(true)

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = if (isEditMode && editingDraftApiId != null) {
                    //   ACTUALIZAR borrador existente
                    android.util.Log.d(TAG, "=== Actualizando borrador existente ===")
                    withContext(Dispatchers.IO) {
                        publicacionRepository.actualizarPublicacion(
                            idPublicacion = editingDraftApiId!!,
                            titulo = title,
                            descripcion = description,
                            estatus = "borrador",
                            imagenes = selectedImageFiles.ifEmpty { null },
                            token = token
                        )
                    }
                } else {
                    //   CREAR nuevo borrador
                    android.util.Log.d(TAG, "=== Creando nuevo borrador ===")
                    withContext(Dispatchers.IO) {
                        publicacionRepository.crearPublicacion(
                            titulo = title,
                            descripcion = description,
                            estatus = "borrador",
                            idAutor = currentUser.id,
                            imagenes = selectedImageFiles.ifEmpty { null },
                            token = token
                        )
                    }
                }

                result.fold(
                    onSuccess = { publicacion ->
                        val action = if (isEditMode) "actualizado" else "guardado"
                        android.util.Log.d(TAG, "   Borrador $action: ${publicacion.id}")
                        Toast.makeText(this@CreatePostActivity, "Borrador $action correctamente", Toast.LENGTH_SHORT).show()
                        cleanupAndFinish()
                    },
                    onFailure = { error ->
                        android.util.Log.e(TAG, "  Error al guardar borrador", error)
                        Toast.makeText(this@CreatePostActivity, "Error al guardar borrador: ${error.message}", Toast.LENGTH_LONG).show()
                    }
                )

            } catch (e: Exception) {
                android.util.Log.e(TAG, "  Exception al guardar borrador", e)
                Toast.makeText(this@CreatePostActivity, "Error inesperado: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                setLoading(false)
            }
        }
    }

    // ============================================
    //   MÉTODO PUBLISHPOST MEJORADO (CREAR/ACTUALIZAR)
    // ============================================
    private fun publishPost() {
        if (!validateForm() || isLoading) return

        val title = binding.etRecipeTitle.text.toString().trim()
        val description = binding.etRecipeDescription.text.toString().trim()

        // Validar que tenga al menos una imagen para publicar
        if (selectedImages.all { it == null }) {
            Toast.makeText(this, "Agrega al menos una imagen para publicar", Toast.LENGTH_SHORT).show()
            return
        }

        val currentUser = sessionManager.getCurrentUser()
        val token = sessionManager.getAccessToken()

        if (currentUser == null || token == null) {
            Toast.makeText(this, "Sesión no válida", Toast.LENGTH_SHORT).show()
            return
        }

        setLoading(true)

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = if (isEditMode && editingDraftApiId != null) {
                    //   ACTUALIZAR borrador a publicado
                    android.util.Log.d(TAG, "=== Publicando borrador existente ===")
                    withContext(Dispatchers.IO) {
                        publicacionRepository.actualizarPublicacion(
                            idPublicacion = editingDraftApiId!!,
                            titulo = title,
                            descripcion = description,
                            estatus = "publicada",
                            imagenes = selectedImageFiles,
                            token = token
                        )
                    }
                } else {
                    //   CREAR nueva publicación
                    android.util.Log.d(TAG, "=== Creando nueva publicación ===")
                    withContext(Dispatchers.IO) {
                        publicacionRepository.crearPublicacion(
                            titulo = title,
                            descripcion = description,
                            estatus = "publicada",
                            idAutor = currentUser.id,
                            imagenes = selectedImageFiles,
                            token = token
                        )
                    }
                }

                result.fold(
                    onSuccess = { publicacion ->
                        android.util.Log.d(TAG, "   Receta publicada: ${publicacion.id}")
                        Toast.makeText(this@CreatePostActivity, "¡Receta publicada exitosamente!", Toast.LENGTH_LONG).show()

                        // Regresar al feed
                        val intent = Intent(this@CreatePostActivity, FeedActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                        startActivity(intent)
                        cleanupAndFinish()
                    },
                    onFailure = { error ->
                        android.util.Log.e(TAG, "  Error al publicar", error)
                        Toast.makeText(this@CreatePostActivity, "Error al publicar: ${error.message}", Toast.LENGTH_LONG).show()
                    }
                )

            } catch (e: Exception) {
                android.util.Log.e(TAG, "  Exception al publicar", e)
                Toast.makeText(this@CreatePostActivity, "Error inesperado: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                setLoading(false)
            }
        }
    }

    // ============================================
    // MÉTODOS AUXILIARES
    // ============================================
    private fun setLoading(loading: Boolean) {
        isLoading = loading

        // Deshabilitar botones durante carga
        binding.btnSaveDraft.isEnabled = !loading
        binding.btnPublish.isEnabled = !loading

        // Cambiar texto de botones según el modo y estado
        if (loading) {
            binding.btnSaveDraft.text = if (isEditMode) "Actualizando..." else "Guardando..."
            binding.btnPublish.text = "Publicando..."
        } else {
            binding.btnSaveDraft.text = if (isEditMode) "Actualizar borrador" else "Guardar como borrador"
            binding.btnPublish.text = "Publicar"
        }

        android.util.Log.d(TAG, "Loading state: $loading")
    }

    private fun cleanupAndFinish() {
        // Limpiar archivos temporales
        selectedImageFiles.forEach { file ->
            try {
                if (file.exists()) {
                    file.delete()
                    android.util.Log.d(TAG, "Archivo temporal eliminado: ${file.name}")
                }
            } catch (e: Exception) {
                android.util.Log.w(TAG, "No se pudo eliminar archivo: ${file.name}")
            }
        }

        // Limpiar lista
        selectedImageFiles.clear()

        finish()
    }

    // ============================================
    // MÉTODOS LEGACY (MANTENIDOS PARA COMPATIBILIDAD)
    // ============================================
    private fun createPostFromForm(title: String, description: String, isDraft: Boolean): Post {
        val currentTime = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        val timestamp = if (isDraft) "Guardado $currentTime" else currentTime

        return Post(
            id = if (isEditMode) editingPostId else generateNewId(),
            title = title,
            description = description,
            imageUrl = "user_recipe_${System.currentTimeMillis()}", // En una app real serían URLs
            author = "@${sessionManager.getCurrentUser()?.alias ?: "Usuario"}", // Usar alias real
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

    override fun onDestroy() {
        super.onDestroy()
        // Asegurar limpieza al destruir la actividad
        cleanupAndFinish()
    }
}