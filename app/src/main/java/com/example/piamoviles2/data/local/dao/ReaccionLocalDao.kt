package com.example.piamoviles2.data.local.dao

import androidx.room.*
import com.example.piamoviles2.data.local.entities.ReaccionLocal
import kotlinx.coroutines.flow.Flow

@Dao
interface ReaccionLocalDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(reaccion: ReaccionLocal): Long

    @Query("SELECT * FROM reacciones_pendientes WHERE sincronizado = 0 ORDER BY fechaCreacion ASC")
    suspend fun obtenerPendientes(): List<ReaccionLocal>

    @Query("SELECT * FROM reacciones_pendientes WHERE sincronizado = 0 ORDER BY fechaCreacion ASC")
    fun observarPendientes(): Flow<List<ReaccionLocal>>

    @Query("UPDATE reacciones_pendientes SET sincronizado = 1 WHERE id = :localId")
    suspend fun marcarComoSincronizado(localId: Long)

    @Query("UPDATE reacciones_pendientes SET intentosSincronizacion = intentosSincronizacion + 1 WHERE id = :localId")
    suspend fun incrementarIntentos(localId: Long)

    @Query("SELECT COUNT(*) FROM reacciones_pendientes WHERE sincronizado = 0")
    suspend fun contarPendientes(): Int

    @Delete
    suspend fun eliminar(reaccion: ReaccionLocal)
}