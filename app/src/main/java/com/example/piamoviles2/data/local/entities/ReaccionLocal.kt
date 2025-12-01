package com.example.piamoviles2.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad Room para reacciones pendientes de sincronización
 */
@Entity(tableName = "reacciones_pendientes")
data class ReaccionLocal(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val idUsuario: String,
    val idPublicacion: String,
    val tipoReaccion: String, // "like" o "dislike"
    val accion: String, // "agregar" o "eliminar"

    // Control de sincronización
    val sincronizado: Boolean = false,
    val intentosSincronizacion: Int = 0,
    val fechaCreacion: Long = System.currentTimeMillis()
)