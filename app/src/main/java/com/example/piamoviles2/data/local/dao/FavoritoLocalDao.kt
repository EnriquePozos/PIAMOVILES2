package com.example.piamoviles2.data.local.dao

import androidx.room.*
import com.example.piamoviles2.data.local.entities.FavoritoLocal
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoritoLocalDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(favorito: FavoritoLocal): Long

    @Query("SELECT * FROM favoritos_pendientes WHERE sincronizado = 0 ORDER BY fechaCreacion ASC")
    suspend fun obtenerPendientes(): List<FavoritoLocal>

    @Query("SELECT * FROM favoritos_pendientes WHERE sincronizado = 0 ORDER BY fechaCreacion ASC")
    fun observarPendientes(): Flow<List<FavoritoLocal>>

    @Query("UPDATE favoritos_pendientes SET sincronizado = 1 WHERE id = :localId")
    suspend fun marcarComoSincronizado(localId: Long)

    @Query("UPDATE favoritos_pendientes SET intentosSincronizacion = intentosSincronizacion + 1 WHERE id = :localId")
    suspend fun incrementarIntentos(localId: Long)

    @Query("SELECT COUNT(*) FROM favoritos_pendientes WHERE sincronizado = 0")
    suspend fun contarPendientes(): Int

    @Delete
    suspend fun eliminar(favorito: FavoritoLocal)
}