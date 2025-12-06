package com.example.piamoviles2

import android.app.Application
import android.util.Log
import androidx.work.*
import com.example.piamoviles2.sync.SyncWorker
import com.example.piamoviles2.utils.NetworkMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * Clase Application personalizada
 * Se ejecuta al iniciar la app
 */
class MyApplication : Application() {

    companion object {
        private const val TAG = "MY_APP"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "🚀 Aplicación iniciada")

        // Configurar WorkManager para sincronización periódica
        setupPeriodicSync()

        // Observar cambios de conectividad para sincronizar inmediatamente
        setupNetworkObserver()
    }

    /**
     * Configura sincronización periódica cada 30 minutos
     */
    private fun setupPeriodicSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED) // Solo con internet
            .setRequiresBatteryNotLow(true) // Solo si la batería no está baja
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            30, TimeUnit.MINUTES // Cada 30 minutos
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                10, TimeUnit.SECONDS
            )
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            SyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )

        Log.d(TAG, "✅ Sincronización periódica configurada (cada 30 min)")
    }

    /**
     * Observa cambios de conectividad para sincronizar inmediatamente
     */
    private fun setupNetworkObserver() {
        val networkMonitor = NetworkMonitor(this)

        CoroutineScope(Dispatchers.IO).launch {
            networkMonitor.observeNetworkState().collect { isConnected ->
                if (isConnected) {
                    Log.d(TAG, "🌐 Conexión restaurada - Iniciando sincronización inmediata")
                    enqueueImmediateSync()
                } else {
                    Log.d(TAG, "📵 Conexión perdida")
                }
            }
        }
    }

    /**
     * Encola una sincronización inmediata
     */
    private fun enqueueImmediateSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueue(syncRequest)
        Log.d(TAG, "📤 Sincronización inmediata encolada")
    }
}