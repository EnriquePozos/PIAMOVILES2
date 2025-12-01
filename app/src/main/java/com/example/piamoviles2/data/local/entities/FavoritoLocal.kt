package com.example.piamoviles2.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad Room para favoritos pendientes de sincronización
 */
@Entity(tableName = "favoritos_pendientes")
data class FavoritoLocal(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val idUsuario: String,
    val idPublicacion: String,
    val accion: String, // "agregar" o "eliminar"

    // Control de sincronización
    val sincronizado: Boolean = false,
    val intentosSincronizacion: Int = 0,
    val fechaCreacion: Long = System.currentTimeMillis()
)