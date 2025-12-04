package com.example.piamoviles2.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad Room para publicaciones pendientes de sincronización
 */
@Entity(tableName = "publicaciones_pendientes")
data class PublicacionLocal(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val titulo: String,
    val descripcion: String?,
    val estatus: String, // "borrador" o "publicada"
    val idAutor: String,

    // Multimedia como JSON string (lista de rutas locales)
    val multimediaJson: String? = null,

    //Token para sincronización
    val token: String = "",


    // Control de sincronización
    val sincronizado: Boolean = false,
    val intentosSincronizacion: Int = 0,
    val fechaCreacion: Long = System.currentTimeMillis(),

    // ID de la API cuando se sincroniza
    val apiId: String? = null
)