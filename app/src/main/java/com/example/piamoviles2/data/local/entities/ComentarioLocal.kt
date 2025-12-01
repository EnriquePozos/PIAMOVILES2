package com.example.piamoviles2.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad Room para comentarios pendientes de sincronización
 */
@Entity(tableName = "comentarios_pendientes")
data class ComentarioLocal(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val comentario: String,
    val idUsuario: String,
    val idPublicacion: String,
    val idComentario: String? = null, // Para respuestas

    // Control de sincronización
    val sincronizado: Boolean = false,
    val intentosSincronizacion: Int = 0,
    val fechaCreacion: Long = System.currentTimeMillis(),

    // ID de la API cuando se sincroniza
    val apiId: String? = null
)