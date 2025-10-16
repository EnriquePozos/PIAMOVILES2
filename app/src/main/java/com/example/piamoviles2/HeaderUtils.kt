package com.example.piamoviles2

import android.app.Activity
import android.view.View
import android.widget.ImageView

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
        val ivProfileIcon = headerView.findViewById<ImageView>(R.id.ivProfileIcon)

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
        val ivProfileIcon = headerView.findViewById<ImageView>(R.id.ivProfileIcon)

        // Ocultar botón de regresar
        btnBack.visibility = View.GONE
        // Mostrar icono de perfil
        ivProfileIcon.visibility = View.VISIBLE

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
        val ivProfileIcon = headerView.findViewById<ImageView>(R.id.ivProfileIcon)

        // Ocultar ambos iconos
        btnBack.visibility = View.GONE
        ivProfileIcon.visibility = View.GONE
    }
}