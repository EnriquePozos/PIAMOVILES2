package com.example.piamoviles2

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.piamoviles2.databinding.ActivityDraftsBinding
// Manejar si esta online o no
import com.example.piamoviles2.utils.NetworkMonitor


// ============================================
//   IMPORTS PARA API INTEGRATION
// ============================================
import com.example.piamoviles2.data.repositories.PublicacionRepository
import com.example.piamoviles2.utils.SessionManager
import kotlinx.coroutines.*

class DraftsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDraftsBinding
    private lateinit var draftAdapter: DraftAdapter
    private var draftPosts = mutableListOf<Post>()

    private lateinit var networkMonitor: NetworkMonitor

    // ============================================
    //   VARIABLES PARA API INTEGRATION
    // ============================================
    private lateinit var sessionManager: SessionManager
    private lateinit var publicacionRepository: PublicacionRepository
    private var isLoading = false
    private val TAG = "DRAFTS_DEBUG"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityDraftsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //   INICIALIZAR API COMPONENTS
        sessionManager = SessionManager(this)
        publicacionRepository = PublicacionRepository(this)
        networkMonitor = NetworkMonitor(this)

        setupHeader()
        setupRecyclerView()
        setupClickListeners()
        loadDraftPosts()
    }

    private fun setupHeader() {
        val headerView = findViewById<View>(R.id.headerApp)
        HeaderUtils.setupHeaderWithBackButton(this, headerView)
    }

    private fun setupRecyclerView() {
        // Usar DraftAdapter especializado
        draftAdapter = DraftAdapter(
            onEditDraft = { draft -> editDraft(draft) },
            onDeleteDraft = { draft -> showDeleteDraftDialog(draft) },
            onDraftClick = { draft -> editDraft(draft) } // Click en la card también edita
        )

        binding.rvDrafts.apply {
            layoutManager = LinearLayoutManager(this@DraftsActivity)
            adapter = draftAdapter
        }
    }

    private fun setupClickListeners() {
        // Botón para crear nueva receta - CONECTADO A CreatePostActivity
        binding.btnCreateNewRecipe.setOnClickListener {
            val intent = Intent(this, CreatePostActivity::class.java)
            startActivity(intent)
        }
    }

    // ============================================
    //   MÉTODO REEMPLAZADO CON API REAL
    // ============================================
    private fun loadDraftPosts() {
        val currentUser = sessionManager.getCurrentUser()
        val token = sessionManager.getAccessToken()

        if (currentUser == null || token == null) {
            android.util.Log.e(TAG, "  Error: Usuario o token no válido")
            Toast.makeText(this, "Error: Sesión no válida", Toast.LENGTH_SHORT).show()
            return
        }

        setLoadingDrafts(true)

        // Llamada a repository con corrutinas
        CoroutineScope(Dispatchers.Main).launch {
            try {
                android.util.Log.d(TAG, "=== Cargando borradores del usuario ===")
                android.util.Log.d(TAG, "User ID: ${currentUser.id}")

                val result = withContext(Dispatchers.IO) {
                    // CAMBIO: Usar método que detecta automáticamente online/offline
                    publicacionRepository.obtenerPublicacionesUsuarioSegunConectividad(
                        idAutor = currentUser.id,
                        incluirBorradores = true, //   TRUE para obtener borradores
                        token = token
                    )
                }

                result.fold(
                    onSuccess = { posts ->
                        // Filtrar solo borradores (por seguridad adicional)
                        val draftsList = posts.filter { it.isDraft }
                        android.util.Log.d(TAG, "  Borradores cargados: ${draftsList.size}")

                        draftPosts.clear()
                        draftPosts.addAll(draftsList)
                        updateUI()

                        if (draftsList.isEmpty()) {
                            android.util.Log.d(TAG, "Usuario no tiene borradores")
                            Toast.makeText(this@DraftsActivity, "No tienes borradores guardados", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@DraftsActivity, "${draftsList.size} borradores cargados", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onFailure = { error ->
                        android.util.Log.e(TAG, "  Error al cargar borradores", error)
                        // CAMBIO: Manejo simplificado ya que el método automático maneja online/offline
                        draftPosts.clear()
                        updateUI()
                        Toast.makeText(this@DraftsActivity, "Error al cargar borradores: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                )

            } catch (e: Exception) {
                android.util.Log.e(TAG, "  Exception al cargar borradores", e)
                Toast.makeText(this@DraftsActivity, "Error inesperado: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                setLoadingDrafts(false)
            }
        }
    }

    // ============================================
    //   NUEVOS MÉTODOS DE SOPORTE PARA API
    // ============================================
    private fun setLoadingDrafts(loading: Boolean) {
        isLoading = loading
        if (loading) {
            // Mostrar loading en el área de drafts
            binding.rvDrafts.visibility = View.GONE
            android.util.Log.d(TAG, "Mostrando loading de borradores...")
        } else {
            // Solo mostrar RecyclerView si hay borradores
            if (draftPosts.isNotEmpty()) {
                binding.rvDrafts.visibility = View.VISIBLE
            }
        }
    }

    // ============================================
    //   MÉTODO UPDATEUI MEJORADO
    // ============================================
    private fun updateUI() {
        android.util.Log.d(TAG, "Actualizando UI - Borradores: ${draftPosts.size}")

        if (draftPosts.isEmpty()) {
            // Mostrar estado vacío
            binding.rvDrafts.visibility = View.GONE
            binding.layoutEmptyDrafts.visibility = View.VISIBLE
            android.util.Log.d(TAG, "Mostrando estado vacío")
        } else {
            // Mostrar lista de borradores
            binding.rvDrafts.visibility = View.VISIBLE
            binding.layoutEmptyDrafts.visibility = View.GONE
            android.util.Log.d(TAG, "Mostrando ${draftPosts.size} borradores")

            //   LOGS PARA DEBUGGING:
            draftPosts.forEachIndexed { index, draft ->
                android.util.Log.d(TAG, "Borrador $index: ${draft.title}")
            }
        }

        //   LOG ANTES DE submitList:
        android.util.Log.d(TAG, "Llamando submitList con ${draftPosts.size} borradores")
        draftAdapter.submitList(draftPosts.toList())

        // Scroll al inicio si hay borradores
        if (draftPosts.isNotEmpty()) {
            binding.rvDrafts.scrollToPosition(0)
        }
    }

    // ============================================
    //   MÉTODOS EXISTENTES MANTENIDOS
    // ============================================
    private fun editDraft(draft: Post) {
        // CONECTADO A CreatePostActivity con ID del borrador
        android.util.Log.d(TAG, "Editando borrador: ${draft.title}")
        android.util.Log.d(TAG, "Borrador API ID: ${draft.apiId}")

        val intent = Intent(this, CreatePostActivity::class.java)
        intent.putExtra(CreatePostActivity.EXTRA_DRAFT_ID, draft.id)
        if (!draft.apiId.isNullOrEmpty() && networkMonitor.isOnline()) {
            intent.putExtra(CreatePostActivity.EXTRA_DRAFT_API_ID, draft.apiId)
        }
        startActivity(intent)
    }

    private fun showDeleteDraftDialog(draft: Post) {
        //   VERIFICAR SI PUEDE ELIMINARSE OFFLINE
        if (!networkMonitor.isOnline() && draft.isSynced) {
            // Mostrar mensaje informativo en lugar de diálogo de confirmación
            AlertDialog.Builder(this)
                .setTitle("No disponible offline")
                .setMessage("Este borrador ya fue sincronizado con el servidor.\n\nPara eliminarlo, necesitas estar conectado a internet.")
                .setPositiveButton("Entendido", null)
                .show()
            return
        }

        // Diálogo normal de confirmación
        val mensaje = if (draft.isSynced && networkMonitor.isOnline()) {
            "¿Estás seguro de que quieres eliminar \"${draft.title}\"?\n\nEste borrador está sincronizado y se eliminará del servidor."
        } else {
            "¿Estás seguro de que quieres eliminar \"${draft.title}\"?\n\nEsta acción no se puede deshacer."
        }

        AlertDialog.Builder(this)
            .setTitle("Eliminar borrador")
            .setMessage(mensaje)
            .setPositiveButton("Eliminar") { _, _ ->
                deleteDraft(draft)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // Convertida a función suspendida para usar corrutinas
    private fun deleteDraft(draft: Post) {
        // Iniciar una corrutina para la operación de red
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val token = sessionManager.getAccessToken() ?: ""
                when {
                    // CASO 1: ONLINE + SINCRONIZADO → Eliminar de API
                    networkMonitor.isOnline() && !draft.apiId.isNullOrEmpty() -> {
                        android.util.Log.d(TAG, "🌐 ONLINE - Eliminando de API: ${draft.apiId}")

                        val result = withContext(Dispatchers.IO) {
                            publicacionRepository.eliminarPublicacion(draft.apiId, token)
                        }

                        result.fold(
                            onSuccess = {
                                android.util.Log.d(TAG, "  Borrador eliminado de API")

                                // También eliminar de SQLite si existe localmente
                                eliminarDeSQLiteSiExiste(draft.apiId)

                                // Actualizar UI
                                draftPosts.remove(draft)
                                updateUI()
                                Toast.makeText(
                                    this@DraftsActivity,
                                    "Borrador \"${draft.title}\" eliminado",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            onFailure = { error ->
                                android.util.Log.e(TAG, "  Error al eliminar de API", error)
                                Toast.makeText(
                                    this@DraftsActivity,
                                    "Error al eliminar: ${error.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }

                    // CASO 2: OFFLINE + NO SINCRONIZADO → Eliminar de SQLite
                    !networkMonitor.isOnline() && !draft.isSynced -> {
                        android.util.Log.d(TAG, "📵 OFFLINE - Eliminando borrador local ID: ${draft.id}")

                        val result = withContext(Dispatchers.IO) {
                            publicacionRepository.eliminarPublicacionOffline(draft.id.toLong())
                        }

                        result.fold(
                            onSuccess = {
                                android.util.Log.d(TAG, "  Borrador local eliminado")

                                // Actualizar UI
                                draftPosts.remove(draft)
                                updateUI()
                                Toast.makeText(
                                    this@DraftsActivity,
                                    "Borrador \"${draft.title}\" eliminado",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            onFailure = { error ->
                                android.util.Log.e(TAG, "  Error al eliminar de SQLite", error)
                                Toast.makeText(
                                    this@DraftsActivity,
                                    "Error al eliminar: ${error.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }

                    // CASO 3: OFFLINE + SINCRONIZADO → NO PERMITIR
                    !networkMonitor.isOnline() && draft.isSynced -> {
                        android.util.Log.w(TAG, "⚠️ OFFLINE - No se puede eliminar borrador sincronizado")
                        Toast.makeText(
                            this@DraftsActivity,
                            "No puedes eliminar un borrador sincronizado sin conexión. Conéctate a internet.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                // Remover el borrador de la lista local y actualizar UI
                draftPosts.remove(draft)
                updateUI()
                Toast.makeText(this@DraftsActivity, "Borrador \"${draft.title}\" eliminado", Toast.LENGTH_SHORT).show()

            } catch (e: Exception) {
                android.util.Log.e(TAG, "  Excepción al eliminar borrador", e)
                Toast.makeText(this@DraftsActivity, "Error inesperado al eliminar: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private suspend fun eliminarDeSQLiteSiExiste(apiId: String) {
        try {
            withContext(Dispatchers.IO) {
                val db = publicacionRepository.database
                db?.publicacionLocalDao()?.obtenerPorApiId(apiId)?.let { publicacion ->
                    db.publicacionLocalDao().eliminar(publicacion)
                    android.util.Log.d(TAG, "  También eliminado de SQLite local")
                }
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "No se pudo eliminar de SQLite: ${e.message}")
        }
    }

    override fun onResume() {
        super.onResume()
        android.util.Log.d(TAG, "=== DraftsActivity onResume ===")

        // Recargar borradores cuando regrese a la pantalla
        // (en caso de que hayan cambiado desde otras pantallas)
        loadDraftPosts()

        android.util.Log.d(TAG, "Borradores refrescados en onResume")
    }
}