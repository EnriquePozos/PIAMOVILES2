package com.example.piamoviles2

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.piamoviles2.databinding.ActivityDraftsBinding

class DraftsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDraftsBinding
    private lateinit var draftAdapter: DraftAdapter
    private var draftPosts = mutableListOf<Post>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityDraftsBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
        // Botón para crear nueva receta
        binding.btnCreateNewRecipe.setOnClickListener {
            Toast.makeText(this, "Crear nueva receta (próximamente)", Toast.LENGTH_SHORT).show()
            // TODO: Navegar a CreatePostActivity
        }
    }

    private fun loadDraftPosts() {
        // Filtrar solo los borradores del usuario actual
        draftPosts.clear()
        draftPosts.addAll(getAllDraftPosts())
        updateUI()
    }

    private fun getAllDraftPosts(): List<Post> {
        // Obtener todos los posts y filtrar solo los borradores del usuario
        return Post.getSamplePosts().filter { it.isDraft && it.isOwner }
    }

    private fun updateUI() {
        if (draftPosts.isEmpty()) {
            // Mostrar estado vacío
            binding.rvDrafts.visibility = View.GONE
            binding.layoutEmptyDrafts.visibility = View.VISIBLE
            //binding.tvDraftsCount.text = "0 borradores"
        } else {
            // Mostrar lista de borradores
            binding.rvDrafts.visibility = View.VISIBLE
            binding.layoutEmptyDrafts.visibility = View.GONE

            val count = draftPosts.size
            //binding.tvDraftsCount.text = if (count == 1) "1 borrador" else "$count borradores"
        }

        // Actualizar adaptador
        draftAdapter.submitList(draftPosts.toList())
    }

    private fun editDraft(draft: Post) {
        Toast.makeText(this, "Editar borrador: ${draft.title}", Toast.LENGTH_SHORT).show()
        // TODO: Navegar a CreatePostActivity con el ID del borrador para editarlo
        // val intent = Intent(this, CreatePostActivity::class.java)
        // intent.putExtra(CreatePostActivity.EXTRA_DRAFT_ID, draft.id)
        // startActivity(intent)
    }

    private fun showDeleteDraftDialog(draft: Post) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar borrador")
            .setMessage("¿Estás seguro de que quieres eliminar \"${draft.title}\"?\n\nEsta acción no se puede deshacer.")
            .setPositiveButton("Eliminar") { _, _ ->
                deleteDraft(draft)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteDraft(draft: Post) {
        // Remover el borrador de la lista
        draftPosts.remove(draft)
        updateUI()

        Toast.makeText(this, "Borrador \"${draft.title}\" eliminado", Toast.LENGTH_SHORT).show()

        // TODO: En una app real, aquí eliminarías el borrador de la base de datos
        // draftRepository.deleteDraft(draft.id)
    }

    override fun onResume() {
        super.onResume()
        // Recargar borradores cuando regrese a la pantalla
        // (en caso de que hayan cambiado desde otras pantallas)
        loadDraftPosts()
    }
}