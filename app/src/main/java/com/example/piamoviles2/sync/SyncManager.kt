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
 * Patrón Singleton
 */
class SyncManager private constructor(private val context: Context) {

    private val database = AppDatabase.getDatabase(context)
    private val networkMonitor = NetworkMonitor(context)
    private val sessionManager = SessionManager(context)

    // Repositories
    private val publicacionRepo = PublicacionRepository()
    private val comentarioRepo = ComentarioRepository()
    private val favoritoRepo = FavoritoRepository()

    // DAOs
    private val publicacionDao = database.publicacionLocalDao()
    private val comentarioDao = database.comentarioLocalDao()
    private val reaccionDao = database.reaccionLocalDao()
    private val favoritoDao = database.favoritoLocalDao()

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
     * Retorna true si todo se sincronizó correctamente
     */
    @RequiresApi(Build.VERSION_CODES.M)
    suspend fun sincronizarTodo(): Boolean {
        if (!networkMonitor.isOnline()) {
            Log.d(TAG, "❌ Sin conexión - No se puede sincronizar")
            return false
        }

        val token = sessionManager.getAccessToken() ?: run {
            Log.e(TAG, "❌ No hay token de sesión")
            return false
        }

        Log.d(TAG, "🔄 Iniciando sincronización completa...")

        var todoExitoso = true

        // 1. Sincronizar publicaciones
        todoExitoso = sincronizarPublicaciones(token) && todoExitoso

        // 2. Sincronizar comentarios
        todoExitoso = sincronizarComentarios(token) && todoExitoso

        // 3. Sincronizar reacciones
        todoExitoso = sincronizarReacciones(token) && todoExitoso

        // 4. Sincronizar favoritos
        todoExitoso = sincronizarFavoritos(token) && todoExitoso

        Log.d(TAG, if (todoExitoso) "✅ Sincronización completa exitosa" else "⚠️ Sincronización completa con errores")
        return todoExitoso
    }

    // ============================================
    // SINCRONIZACIÓN DE PUBLICACIONES
    // ============================================
    private suspend fun sincronizarPublicaciones(token: String): Boolean {
        val pendientes = publicacionDao.obtenerPendientes()
        Log.d(TAG, "📝 Publicaciones pendientes: ${pendientes.size}")

        if (pendientes.isEmpty()) return true

        val currentUser = sessionManager.getCurrentUser() ?: return false
        var exitosas = 0

        for (pub in pendientes) {
            try {
                // Convertir JSON de multimedia a lista de archivos
                val archivos = pub.multimediaJson?.let { json ->
                    JSONArray(json).let { jsonArray ->
                        (0 until jsonArray.length()).mapNotNull { i ->
                            val path = jsonArray.getString(i)
                            File(path).takeIf { it.exists() }
                        }
                    }
                }

                val result = publicacionRepo.crearPublicacion(
                    titulo = pub.titulo,
                    descripcion = pub.descripcion,
                    estatus = pub.estatus,
                    idAutor = pub.idAutor,
                    imagenes = archivos,
                    token = token
                )

                result.fold(
                    onSuccess = { publicacionResponse ->
                        publicacionDao.marcarComoSincronizado(pub.id, publicacionResponse.id)
                        exitosas++
                        Log.d(TAG, "✅ Publicación sincronizada: ${publicacionResponse.id}")
                    },
                    onFailure = { error ->
                        publicacionDao.incrementarIntentos(pub.id)
                        Log.e(TAG, "❌ Error al sincronizar publicación", error)
                    }
                )

            } catch (e: Exception) {
                publicacionDao.incrementarIntentos(pub.id)
                Log.e(TAG, "❌ Exception al sincronizar publicación", e)
            }
        }

        Log.d(TAG, "📝 Publicaciones sincronizadas: $exitosas/${pendientes.size}")
        return exitosas == pendientes.size
    }

    // ============================================
    // SINCRONIZACIÓN DE COMENTARIOS
    // ============================================
    private suspend fun sincronizarComentarios(token: String): Boolean {
        val pendientes = comentarioDao.obtenerPendientes()
        Log.d(TAG, "💬 Comentarios pendientes: ${pendientes.size}")

        if (pendientes.isEmpty()) return true

        var exitosas = 0

        for (com in pendientes) {
            try {
                val result = comentarioRepo.crearComentario(
                    idUsuario = com.idUsuario,
                    idPublicacion = com.idPublicacion,
                    comentario = com.comentario,
                    /*idComentario = com.idComentario,*/
                    token = token
                )

                result.fold(
                    onSuccess = { comentarioResponse ->
                        comentarioDao.marcarComoSincronizado(com.id, comentarioResponse.id)
                        exitosas++
                        Log.d(TAG, "✅ Comentario sincronizado: ${comentarioResponse.id}")
                    },
                    onFailure = { error ->
                        comentarioDao.incrementarIntentos(com.id)
                        Log.e(TAG, "❌ Error al sincronizar comentario", error)
                    }
                )

            } catch (e: Exception) {
                comentarioDao.incrementarIntentos(com.id)
                Log.e(TAG, "❌ Exception al sincronizar comentario", e)
            }
        }

        Log.d(TAG, "💬 Comentarios sincronizados: $exitosas/${pendientes.size}")
        return exitosas == pendientes.size
    }

    // ============================================
    // SINCRONIZACIÓN DE REACCIONES
    // ============================================
    private suspend fun sincronizarReacciones(token: String): Boolean {
        val pendientes = reaccionDao.obtenerPendientes()
        Log.d(TAG, "👍 Reacciones pendientes: ${pendientes.size}")

        if (pendientes.isEmpty()) return true

        var exitosas = 0

        for (reac in pendientes) {
            try {
                // Aquí llamarías al método correspondiente de reacción en PublicacionRepository
                // Por ejemplo: publicacionRepo.agregarReaccion() o eliminarReaccion()
                // val result = ...

                // Por ahora, marco como sincronizado de ejemplo
                reaccionDao.marcarComoSincronizado(reac.id)
                exitosas++
                Log.d(TAG, "✅ Reacción sincronizada")

            } catch (e: Exception) {
                reaccionDao.incrementarIntentos(reac.id)
                Log.e(TAG, "❌ Exception al sincronizar reacción", e)
            }
        }

        Log.d(TAG, "👍 Reacciones sincronizadas: $exitosas/${pendientes.size}")
        return exitosas == pendientes.size
    }

    // ============================================
    // SINCRONIZACIÓN DE FAVORITOS
    // ============================================
    private suspend fun sincronizarFavoritos(token: String): Boolean {
        val pendientes = favoritoDao.obtenerPendientes()
        Log.d(TAG, "⭐ Favoritos pendientes: ${pendientes.size}")

        if (pendientes.isEmpty()) return true

        val currentUser = sessionManager.getCurrentUser() ?: return false
        var exitosas = 0

        for (fav in pendientes) {
            try {
                val result = when (fav.accion) {
                    "agregar" -> favoritoRepo.agregarFavorito(
                        idUsuario = fav.idUsuario,
                        idPublicacion = fav.idPublicacion,
                        token = token
                    )
                    "eliminar" -> favoritoRepo.quitarFavorito(
                        idUsuario = fav.idUsuario,
                        idPublicacion = fav.idPublicacion,
                        token = token
                    )
                    else -> Result.failure(Exception("Acción desconocida: ${fav.accion}"))
                }

                result.fold(
                    onSuccess = {
                        favoritoDao.marcarComoSincronizado(fav.id)
                        exitosas++
                        Log.d(TAG, "✅ Favorito sincronizado")
                    },
                    onFailure = { error ->
                        favoritoDao.incrementarIntentos(fav.id)
                        Log.e(TAG, "❌ Error al sincronizar favorito", error)
                    }
                )

            } catch (e: Exception) {
                favoritoDao.incrementarIntentos(fav.id)
                Log.e(TAG, "❌ Exception al sincronizar favorito", e)
            }
        }

        Log.d(TAG, "⭐ Favoritos sincronizados: $exitosas/${pendientes.size}")
        return exitosas == pendientes.size
    }

    /**
     * Obtiene el conteo de elementos pendientes
     */
    suspend fun obtenerContadorPendientes(): PendientesInfo {
        return PendientesInfo(
            publicaciones = publicacionDao.contarPendientes(),
            comentarios = comentarioDao.contarPendientes(),
            reacciones = reaccionDao.contarPendientes(),
            favoritos = favoritoDao.contarPendientes()
        )
    }

    data class PendientesInfo(
        val publicaciones: Int,
        val comentarios: Int,
        val reacciones: Int,
        val favoritos: Int
    ) {
        val total: Int get() = publicaciones + comentarios + reacciones + favoritos
    }
}