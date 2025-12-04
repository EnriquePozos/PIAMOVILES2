package com.example.piamoviles2.sync

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.piamoviles2.data.local.AppDatabase
import com.example.piamoviles2.data.repositories.*
import com.example.piamoviles2.utils.NetworkMonitor
import com.example.piamoviles2.utils.SessionManager
import kotlinx.coroutines.*
import org.json.JSONArray
import java.io.File

/**
 * Gestor de sincronización de datos pendientes
 * Patrón Singleton - CORREGIDO para evitar duplicaciones
 */
class SyncManager private constructor(private val context: Context) {

    private val database = AppDatabase.getDatabase(context)
    private val networkMonitor = NetworkMonitor(context)
    private val sessionManager = SessionManager(context)
    private val publicacionRepo = PublicacionRepository(context = context)
    private val comentarioRepo = ComentarioRepository()
    private val favoritoRepo = FavoritoRepository()

    // DAOs
    private val publicacionDao = database.publicacionLocalDao()
    private val comentarioDao = database.comentarioLocalDao()
    private val reaccionDao = database.reaccionLocalDao()
    private val favoritoDao = database.favoritoLocalDao()

    // NUEVO: Control para evitar sincronizaciones simultáneas
    private var isSyncing = false

    companion object {
        private const val TAG = "SYNC_MANAGER"

        @Volatile
        private var INSTANCE: SyncManager? = null

        fun getInstance(context: Context): SyncManager {
            return INSTANCE ?: synchronized(this) {
                val instance = SyncManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }

    /**
     * Sincroniza TODOS los datos pendientes
     * CORREGIDO: Previene sincronizaciones simultáneas
     */
    @RequiresApi(Build.VERSION_CODES.M)
    suspend fun sincronizarTodo(): Boolean {
        if (isSyncing) {
            Log.d(TAG, "⚠️ Sincronización ya en progreso - Ignorando")
            return false
        }

        if (!networkMonitor.isOnline()) {
            Log.d(TAG, "❌ Sin conexión - No se puede sincronizar")
            return false
        }

        val token = sessionManager.getAccessToken() ?: run {
            Log.e(TAG, "❌ No hay token de sesión")
            return false
        }

        isSyncing = true
        Log.d(TAG, "🔄 Iniciando sincronización completa...")

        return try {
            var todoExitoso = true

            // 1. Sincronizar publicaciones (CON NUEVA LÓGICA)
            todoExitoso = sincronizarPublicacionesCorrect(token) && todoExitoso

            // 2. Sincronizar comentarios
            todoExitoso = sincronizarComentarios(token) && todoExitoso

            // 3. Sincronizar reacciones
            todoExitoso = sincronizarReacciones(token) && todoExitoso

            // 4. Sincronizar favoritos
            todoExitoso = sincronizarFavoritos(token) && todoExitoso

            Log.d(TAG, if (todoExitoso) "✅ Sincronización completa exitosa" else "⚠️ Sincronización completa con errores")
            todoExitoso

        } finally {
            isSyncing = false
        }
    }

    // ============================================
    // SINCRONIZACIÓN DE PUBLICACIONES CORREGIDA
    // ============================================
    private suspend fun sincronizarPublicacionesCorrect(token: String): Boolean {
        val pendientes = publicacionDao.obtenerPendientes()
        Log.d(TAG, "📝 Publicaciones pendientes: ${pendientes.size}")

        if (pendientes.isEmpty()) return true

        var exitosas = 0

        for (pub in pendientes) {
            try {
                Log.d(TAG, "Sincronizando publicación ID: ${pub.id}")

                // NUEVO: Usar el método del repository que ya maneja sincronización
                val result = publicacionRepo.sincronizarPublicacionesPendientes(token)

                result.fold(
                    onSuccess = { sincronizadas ->
                        exitosas += sincronizadas
                        Log.d(TAG, "✅ Sincronización exitosa: $sincronizadas publicaciones")
                    },
                    onFailure = { error ->
                        Log.e(TAG, "❌ Error al sincronizar publicaciones: ${error.message}")
                    }
                )

                // Romper el loop después del primer éxito (ya sincroniza todas)
                break

            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception al sincronizar publicaciones", e)
            }
        }

        Log.d(TAG, "📝 Publicaciones sincronizadas: $exitosas")
        return exitosas > 0 || pendientes.isEmpty()
    }

    // ============================================
    // SINCRONIZACIÓN DE COMENTARIOS
    // ============================================
    private suspend fun sincronizarComentarios(token: String): Boolean {
        // Esta lógica la implementaremos cuando tengas ComentarioRepository con soporte offline
        Log.d(TAG, "💬 Comentarios: Implementación pendiente")
        return true
    }

    // ============================================
    // SINCRONIZACIÓN DE REACCIONES
    // ============================================
    private suspend fun sincronizarReacciones(token: String): Boolean {
        // Esta lógica la implementaremos cuando tengas soporte offline para reacciones
        Log.d(TAG, "👍 Reacciones: Implementación pendiente")
        return true
    }

    // ============================================
    // SINCRONIZACIÓN DE FAVORITOS
    // ============================================
    private suspend fun sincronizarFavoritos(token: String): Boolean {
        // Esta lógica la implementaremos cuando tengas FavoritoRepository con soporte offline
        Log.d(TAG, "⭐ Favoritos: Implementación pendiente")
        return true
    }

    /**
     * Obtiene el conteo de elementos pendientes
     */
    suspend fun obtenerContadorPendientes(): PendientesInfo {
        return PendientesInfo(
            publicaciones = publicacionDao.contarPendientes(),
            comentarios = 0, // TODO: Implementar cuando ComentarioDao esté listo
            reacciones = 0,  // TODO: Implementar cuando ReaccionDao esté listo
            favoritos = 0    // TODO: Implementar cuando FavoritoDao esté listo
        )
    }

    data class PendientesInfo(
        val publicaciones: Int,
        val comentarios: Int,
        val reacciones: Int,
        val favoritos: Int
    ) {
        val total: Int get() = publicaciones + comentarios + reacciones + favoritos
        val isEmpty: Boolean get() = total == 0
    }
}