package com.example.piamoviles2.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.piamoviles2.utils.NetworkMonitor

/**
 * Worker para sincronización automática en segundo plano
 * Se ejecuta periódicamente cuando hay conexión
 */
class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "SYNC_WORKER"
        const val WORK_NAME = "SyncPendingData"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "🔄 Iniciando trabajo de sincronización...")

        val networkMonitor = NetworkMonitor(applicationContext)

        // Verificar conectividad
        if (!networkMonitor.isOnline()) {
            Log.d(TAG, "❌ Sin conexión - Reintentando más tarde")
            return Result.retry()
        }

        return try {
            val syncManager = SyncManager.getInstance(applicationContext)
            val exitoso = syncManager.sincronizarTodo()

            if (exitoso) {
                Log.d(TAG, "✅ Sincronización completada exitosamente")
                Result.success()
            } else {
                Log.d(TAG, "⚠️ Sincronización completada con errores - Reintentando")
                Result.retry()
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error en sincronización", e)
            Result.retry()
        }
    }
}