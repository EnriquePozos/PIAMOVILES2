package com.example.piamoviles2.data.local.dao

import androidx.room.*
import com.example.piamoviles2.data.local.entities.PublicacionLocal
import kotlinx.coroutines.flow.Flow

@Dao
interface PublicacionLocalDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(publicacion: PublicacionLocal): Long

    @Update
    suspend fun actualizar(publicacion: PublicacionLocal)

    @Delete
    suspend fun eliminar(publicacion: PublicacionLocal)

    @Query("SELECT * FROM publicaciones_pendientes WHERE sincronizado = 0 ORDER BY fechaCreacion ASC")
    suspend fun obtenerPendientes(): List<PublicacionLocal>

    @Query("SELECT * FROM publicaciones_pendientes WHERE sincronizado = 0 ORDER BY fechaCreacion ASC")
    fun observarPendientes(): Flow<List<PublicacionLocal>>

    @Query("UPDATE publicaciones_pendientes SET sincronizado = 1, apiId = :apiId WHERE id = :localId")
    suspend fun marcarComoSincronizado(localId: Long, apiId: String)

    @Query("UPDATE publicaciones_pendientes SET intentosSincronizacion = intentosSincronizacion + 1 WHERE id = :localId")
    suspend fun incrementarIntentos(localId: Long)

    @Query("SELECT COUNT(*) FROM publicaciones_pendientes WHERE sincronizado = 0")
    suspend fun contarPendientes(): Int

    // Obtiene publicacion local por us ID
    @Query("SELECT * FROM publicaciones_pendientes WHERE id = :localId LIMIT 1")
    suspend fun obtenerPorId(localId: Long): PublicacionLocal?

    // ============================================
    // NUEVO: MÉTODOS PARA FEED OFFLINE
    // ============================================

    /**
     * Obtiene todas las publicaciones locales para mostrar en el feed cuando está offline
     * Ordenadas por fecha de creación descendente (más recientes primero)
     */
    @Query("SELECT * FROM publicaciones_pendientes WHERE idAutor = :idAutor ORDER BY fechaCreacion DESC")
    suspend fun obtenerPublicacionesParaFeed(idAutor: String?): List<PublicacionLocal>

    /**
     * Observa cambios en todas las publicaciones locales para el feed
     * Útil para actualizaciones en tiempo real
     */
    @Query("SELECT * FROM publicaciones_pendientes WHERE idAutor = :idAutor ORDER BY fechaCreacion DESC")
    fun observarPublicacionesParaFeed(idAutor: String?): Flow<List<PublicacionLocal>>
}