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
    private lateinit var videoPickerHelper: VideoPickerHelper

    // Control de imágenes
    private val multimediaItems = mutableListOf<MultimediaItem>()
    private lateinit var multimediaAdapter: MultimediaAdapter
    private val MAX_MULTIMEDIA = 10 // Límite de items multimedia

    // Modo de edición
    private var isEditMode = false
    private var editingPostId = -1
    private var editingDraftApiId: String? = null // 🆕 API ID del borrador a editar

    // ============================================
    // VARIABLES PARA API INTEGRATION
    // ============================================
    private lateinit var publicacionRepository: PublicacionRepository
    private lateinit var sessionManager: SessionManager
    private var isLoading = false

    companion object {
        const val EXTRA_DRAFT_ID = "extra_draft_id"
        const val EXTRA_POST_ID = "extra_post_id"
        const val EXTRA_DRAFT_API_ID = "extra_draft_api_id" // 🆕 Para recibir API ID del borrador
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
        setupVideoPicker()
        setupMultimediaRecyclerView()
        setupClickListeners()
        setupBackPressedHandler()

        // ============================================
        //   NUEVAS FUNCIONALIDADES API
        // ============================================
        setupApiComponents()
        loadUserData()

        // 🆕 Verificar modo de edición DESPUÉS de configurar API
        checkEditMode()

        android.util.Log.d(TAG, "CreatePostActivity iniciada")
    }

    private fun setupMultimediaRecyclerView() {
        multimediaAdapter = MultimediaAdapter(multimediaItems) { position ->
            removeMultimediaItem(position)
        }

        binding.rvMultimedia.apply {
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(
                this@CreatePostActivity,
                androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL,
                false
            )
            adapter = multimediaAdapter
        }

        // Mostrar/ocultar mensaje de empty state
        updateEmptyState()

        android.util.Log.d(TAG, "RecyclerView de multimedia configurado")
    }

    private fun removeMultimediaItem(position: Int) {
        if (position in multimediaItems.indices) {
            val item = multimediaItems[position]

            // Limpiar recursos del item
            item.cleanup()

            // Eliminar del adapter
            multimediaAdapter.removeItem(position)
            updateEmptyState()

            Toast.makeText(this, "Elemento eliminado (${multimediaItems.size}/$MAX_MULTIMEDIA)", Toast.LENGTH_SHORT).show()
            android.util.Log.d(TAG, "✅ Item eliminado - Total restante: ${multimediaItems.size}")
        }
    }

    private fun updateEmptyState() {
        if (multimediaItems.isEmpty()) {
            binding.tvEmptyMultimedia.visibility = View.VISIBLE
            binding.rvMultimedia.visibility = View.GONE
        } else {
            binding.tvEmptyMultimedia.visibility = View.GONE
            binding.rvMultimedia.visibility = View.VISIBLE
        }
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
//        repeat(MAX_IMAGES) {
//            selectedImages.add(null)
//        }
        android.util.Log.d(TAG, "Imágenes inicializadas")
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

    private fun setupVideoPicker() {
        videoPickerHelper = VideoPickerHelper(this) { videoResult ->
            if (videoResult != null) {
                addVideoToMultimedia(videoResult)
            } else {
                Toast.makeText(this, "Error al cargar el video", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupClickListeners() {
        // 🆕 Botón para agregar multimedia
        binding.btnAddMultimedia.setOnClickListener {
            if (multimediaItems.size >= MAX_MULTIMEDIA) {
                Toast.makeText(this, "Máximo $MAX_MULTIMEDIA elementos permitidos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            showMultimediaSourceDialog()
        }

        // Botones de acción
        binding.btnSaveDraft.setOnClickListener { saveDraft() }
        binding.btnPublish.setOnClickListener { publishPost() }
    }

    private fun showMultimediaSourceDialog() {
        AlertDialog.Builder(this)
            .setTitle("Agregar multimedia")
            .setItems(arrayOf("📷 Foto (Cámara)", "🖼️ Imagen (Galería)", "🎥 Video")) { _, which ->
                when (which) {
                    0 -> imagePickerHelper.openCamera()
                    1 -> imagePickerHelper.openGallery()
                    2 -> openVideoPicker()
                }
            }
            .show()
    }

    private fun openVideoPicker() {
        // Mostrar opciones: Cámara o Galería
        AlertDialog.Builder(this)
            .setTitle("Seleccionar video")
            .setItems(arrayOf("📹 Grabar video", "🎬 Galería de videos")) { _, which ->
                when (which) {
                    0 -> videoPickerHelper.openCamera()
                    1 -> videoPickerHelper.openGallery()
                }
            }
            .show()
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
        val hasMultimedia = multimediaItems.isNotEmpty() // ✅ CORRECTO

        if (title.isNotEmpty() || description.isNotEmpty() || hasMultimedia) {
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
    // 🆕 MÉTODO CHECKEEDITMODE MEJORADO
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
    // 🆕 CARGAR BORRADOR DESDE API
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
                        android.util.Log.d(TAG, "✅ Borrador cargado: ${publicacion.titulo}")

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
                        android.util.Log.e(TAG, "❌ Error al cargar borrador", error)
                        Toast.makeText(this@CreatePostActivity, "Error al cargar borrador: ${error.message}", Toast.LENGTH_LONG).show()
                        finish()
                    }
                )

            } catch (e: Exception) {
                android.util.Log.e(TAG, "❌ Exception al cargar borrador", e)
                Toast.makeText(this@CreatePostActivity, "Error inesperado: ${e.message}", Toast.LENGTH_LONG).show()
                finish()
            } finally {
                setLoading(false)
            }
        }
    }

    // ============================================
    // 🆕 CARGAR IMÁGENES DESDE URLs
    // ============================================
    private fun loadImagesFromUrls(imageUrls: List<String>) {
        android.util.Log.d(TAG, "=== Cargando ${imageUrls.size} imágenes desde URLs ===")

        imageUrls.forEach { url ->
            android.util.Log.d(TAG, "Cargando imagen desde: $url")

            Glide.with(this)
                .asBitmap()
                .load(url)
                .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        android.util.Log.d(TAG, "✅ Imagen cargada exitosamente desde URL")

                        // Crear item multimedia
                        val item = MultimediaItem.crearImagen(resource)

                        // Convertir a archivo
                        convertBitmapToFileForItem(resource, item)

                        // Agregar al adapter
                        multimediaAdapter.addItem(item)
                        updateEmptyState()
                    }

                    override fun onLoadCleared(placeholder: android.graphics.drawable.Drawable?) {
                        // No hacer nada
                    }

                    override fun onLoadFailed(errorDrawable: android.graphics.drawable.Drawable?) {
                        android.util.Log.e(TAG, "❌ Error al cargar imagen desde $url")
                    }
                })
        }
    }

    // ============================================
    // 🆕 ACTUALIZAR UI PARA MODO EDICIÓN
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

    private fun addImageToSlot(bitmap: Bitmap) {
        val resizedBitmap = ImagePickerHelper.resizeBitmap(bitmap)

        // Crear item de multimedia
        val item = MultimediaItem.crearImagen(resizedBitmap)

        // Convertir a archivo
        convertBitmapToFileForItem(resizedBitmap, item)

        // Agregar al adapter
        multimediaAdapter.addItem(item)
        updateEmptyState()

        Toast.makeText(this, "Imagen agregada (${multimediaItems.size}/$MAX_MULTIMEDIA)", Toast.LENGTH_SHORT).show()
        android.util.Log.d(TAG, "✅ Imagen agregada - Total: ${multimediaItems.size}")
    }

    private fun addVideoToMultimedia(videoResult: VideoPickerHelper.VideoResult) {
        try {
            android.util.Log.d(TAG, "=== Agregando video a multimedia ===")

            // Crear item multimedia de tipo VIDEO
            val item = MultimediaItem.crearVideo(
                uri = videoResult.uri,
                thumbnail = videoResult.thumbnail
            )

            // Asignar el archivo del video
            item.file = videoResult.file

            // Agregar al adapter
            multimediaAdapter.addItem(item)
            updateEmptyState()

            val durationSec = videoResult.durationMs / 1000
            val sizeMB = String.format("%.1f", videoResult.sizeBytes / (1024.0 * 1024.0))

            Toast.makeText(
                this,
                "Video agregado: ${durationSec}s, ${sizeMB}MB (${multimediaItems.size}/$MAX_MULTIMEDIA)",
                Toast.LENGTH_LONG
            ).show()

            android.util.Log.d(TAG, "✅ Video agregado - Total: ${multimediaItems.size}")

        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Error al agregar video", e)
            Toast.makeText(this, "Error al procesar video: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // ============================================
    // NUEVO MÉTODO PARA CONVERTIR BITMAP A FILE
    // ============================================
    private fun convertBitmapToFileForItem(bitmap: Bitmap, item: MultimediaItem) {
        try {
            val tempFile = File(cacheDir, "multimedia_${item.id}.jpg")

            FileOutputStream(tempFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }

            item.file = tempFile

            android.util.Log.d(TAG, "✅ Imagen convertida a archivo: ${tempFile.name}")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Error al convertir bitmap a archivo", e)
        }
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
    // 🆕 MÉTODO SAVEEDRAFT MEJORADO (CREAR/ACTUALIZAR)
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
                val archivosMultimedia = multimediaAdapter.getFiles() // ✅ OBTENER ARCHIVOS DEL ADAPTER

                val result = if (isEditMode && editingDraftApiId != null) {
                    android.util.Log.d(TAG, "=== Actualizando borrador existente ===")
                    withContext(Dispatchers.IO) {
                        publicacionRepository.actualizarPublicacion(
                            idPublicacion = editingDraftApiId!!,
                            titulo = title,
                            descripcion = description,
                            estatus = "borrador",
                            imagenes = archivosMultimedia.ifEmpty { null }, // ✅ USAR ARCHIVOS DEL ADAPTER
                            token = token
                        )
                    }
                } else {
                    android.util.Log.d(TAG, "=== Creando nuevo borrador ===")
                    withContext(Dispatchers.IO) {
                        publicacionRepository.crearPublicacion(
                            titulo = title,
                            descripcion = description,
                            estatus = "borrador",
                            idAutor = currentUser.id,
                            imagenes = archivosMultimedia.ifEmpty { null }, // ✅ USAR ARCHIVOS DEL ADAPTER
                            token = token
                        )
                    }
                }

                result.fold(
                    onSuccess = { publicacion ->
                        val action = if (isEditMode) "actualizado" else "guardado"
                        android.util.Log.d(TAG, "✅ Borrador $action: ${publicacion.id}")
                        Toast.makeText(this@CreatePostActivity, "Borrador $action correctamente", Toast.LENGTH_SHORT).show()
                        cleanupAndFinish()
                    },
                    onFailure = { error ->
                        android.util.Log.e(TAG, "❌ Error al guardar borrador", error)
                        Toast.makeText(this@CreatePostActivity, "Error al guardar borrador: ${error.message}", Toast.LENGTH_LONG).show()
                    }
                )

            } catch (e: Exception) {
                android.util.Log.e(TAG, "❌ Exception al guardar borrador", e)
                Toast.makeText(this@CreatePostActivity, "Error inesperado: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                setLoading(false)
            }
        }
    }

    // ============================================
    // 🆕 MÉTODO PUBLISHPOST MEJORADO (CREAR/ACTUALIZAR)
    // ============================================
    private fun publishPost() {
        if (!validateForm() || isLoading) return

        val title = binding.etRecipeTitle.text.toString().trim()
        val description = binding.etRecipeDescription.text.toString().trim()

        // Validar que tenga al menos una imagen para publicar
        if (multimediaAdapter.isEmpty()) { // ✅ CORRECTO
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
                val archivosMultimedia = multimediaAdapter.getFiles() // ✅ OBTENER ARCHIVOS

                val result = if (isEditMode && editingDraftApiId != null) {
                    android.util.Log.d(TAG, "=== Publicando borrador existente ===")
                    withContext(Dispatchers.IO) {
                        publicacionRepository.actualizarPublicacion(
                            idPublicacion = editingDraftApiId!!,
                            titulo = title,
                            descripcion = description,
                            estatus = "publicada",
                            imagenes = archivosMultimedia, // ✅ USAR ARCHIVOS DEL ADAPTER
                            token = token
                        )
                    }
                } else {
                    android.util.Log.d(TAG, "=== Creando nueva publicación ===")
                    withContext(Dispatchers.IO) {
                        publicacionRepository.crearPublicacion(
                            titulo = title,
                            descripcion = description,
                            estatus = "publicada",
                            idAutor = currentUser.id,
                            imagenes = archivosMultimedia, // ✅ USAR ARCHIVOS DEL ADAPTER
                            token = token
                        )
                    }
                }

                result.fold(
                    onSuccess = { publicacion ->
                        android.util.Log.d(TAG, "✅ Receta publicada: ${publicacion.id}")
                        Toast.makeText(this@CreatePostActivity, "¡Receta publicada exitosamente!", Toast.LENGTH_LONG).show()

                        val intent = Intent(this@CreatePostActivity, FeedActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                        startActivity(intent)
                        cleanupAndFinish()
                    },
                    onFailure = { error ->
                        android.util.Log.e(TAG, "❌ Error al publicar", error)
                        Toast.makeText(this@CreatePostActivity, "Error al publicar: ${error.message}", Toast.LENGTH_LONG).show()
                    }
                )

            } catch (e: Exception) {
                android.util.Log.e(TAG, "❌ Exception al publicar", e)
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
        // Limpiar todos los items multimedia
        multimediaItems.forEach { item ->
            item.cleanup()
        }

        multimediaItems.clear()

        android.util.Log.d(TAG, "Limpieza completada - Todos los archivos temporales eliminados")

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