package com.example.piamoviles2.data.local.dao

import androidx.room.*
import com.example.piamoviles2.data.local.entities.ComentarioLocal
import kotlinx.coroutines.flow.Flow

@Dao
interface ComentarioLocalDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(comentario: ComentarioLocal): Long

    @Update
    suspend fun actualizar(comentario: ComentarioLocal)

    @Delete
    suspend fun eliminar(comentario: ComentarioLocal)

    @Query("SELECT * FROM comentarios_pendientes WHERE sincronizado = 0 ORDER BY fechaCreacion ASC")
    suspend fun obtenerPendientes(): List<ComentarioLocal>

    @Query("SELECT * FROM comentarios_pendientes WHERE sincronizado = 0 ORDER BY fechaCreacion ASC")
    fun observarPendientes(): Flow<List<ComentarioLocal>>

    @Query("UPDATE comentarios_pendientes SET sincronizado = 1, apiId = :apiId WHERE id = :localId")
    suspend fun marcarComoSincronizado(localId: Long, apiId: String)

    @Query("UPDATE comentarios_pendientes SET intentosSincronizacion = intentosSincronizacion + 1 WHERE id = :localId")
    suspend fun incrementarIntentos(localId: Long)

    @Query("SELECT COUNT(*) FROM comentarios_pendientes WHERE sincronizado = 0")
    suspend fun contarPendientes(): Int
}