package com.example.piamoviles2

import android.app.Activity
import android.view.View
import android.widget.ImageView
import com.example.piamoviles2.utils.ImageUtils
import com.example.piamoviles2.utils.SessionManager
import de.hdodenhof.circleimageview.CircleImageView

/**
 * Clase utilitaria para configurar el header reutilizable
 */
object HeaderUtils {

    /**
     * Configura el header para pantallas que necesitan botón de regresar
     */
    fun setupHeaderWithBackButton(
        activity: Activity,
        headerView: View,
        onBackClick: (() -> Unit)? = null
    ) {
        val btnBack = headerView.findViewById<ImageView>(R.id.btnBack)
        val ivProfileIcon = headerView.findViewById<CircleImageView>(R.id.ivProfileIcon)

        // Mostrar botón de regresar
        btnBack.visibility = View.VISIBLE
        // Ocultar icono de perfil
        ivProfileIcon.visibility = View.GONE

        // Configurar click del botón regresar
        btnBack.setOnClickListener {
            if (onBackClick != null) {
                onBackClick()
            } else {
                // Comportamiento por defecto: cerrar la activity
                activity.finish()
            }
        }
    }

    /**
     * Configura el header para la pantalla principal (con icono de perfil)
     */
    fun setupHeaderWithProfile(
        headerView: View,
        onProfileClick: (() -> Unit)? = null
    ) {
        val btnBack = headerView.findViewById<ImageView>(R.id.btnBack)
        val ivProfileIcon = headerView.findViewById<CircleImageView>(R.id.ivProfileIcon)

        // Ocultar botón de regresar
        btnBack.visibility = View.GONE
        // Mostrar icono de perfil
        ivProfileIcon.visibility = View.VISIBLE

        // CARGAR IMAGEN DE PERFIL CON GLIDE
        loadHeaderProfileImage(headerView)

        // Configurar click del perfil
        ivProfileIcon.setOnClickListener {
            onProfileClick?.invoke()
        }
    }

    /**
     * Configura el header básico (solo logo y título)
     */
    fun setupBasicHeader(headerView: View) {
        val btnBack = headerView.findViewById<ImageView>(R.id.btnBack)
        val ivProfileIcon = headerView.findViewById<CircleImageView>(R.id.ivProfileIcon)

        // Ocultar ambos iconos
        btnBack.visibility = View.GONE
        ivProfileIcon.visibility = View.GONE
    }

    /**
     Cargar imagen de perfil en el header
     */
    private fun loadHeaderProfileImage(headerView: View) {
        val context = headerView.context
        val ivProfileIcon = headerView.findViewById<CircleImageView>(R.id.ivProfileIcon)
        val sessionManager = SessionManager(context)

        val currentUser = sessionManager.getCurrentUser()
        val profileImageUrl = currentUser?.fotoPerfil

        // Cargar con Glide
        ImageUtils.loadProfileImage(
            context = context,
            imageUrl = profileImageUrl,
            circleImageView = ivProfileIcon,
            showPlaceholder = true
        )

        android.util.Log.d("HEADER_DEBUG", "Cargando imagen del header: $profileImageUrl")
    }

    /**
     Refrescar imagen del header (llamar después de actualizaciones)
     */
    fun refreshHeaderProfileImage(headerView: View) {
        loadHeaderProfileImage(headerView)
        android.util.Log.d("HEADER_DEBUG", "Header refrescado")
    }
}