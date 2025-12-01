package com.example.piamoviles2.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.piamoviles2.data.local.dao.*
import com.example.piamoviles2.data.local.entities.*

/**
 * Base de datos Room para almacenamiento offline
 * Patrón Singleton
 */
@Database(
    entities = [
        PublicacionLocal::class,
        ComentarioLocal::class,
        ReaccionLocal::class,
        FavoritoLocal::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun publicacionLocalDao(): PublicacionLocalDao
    abstract fun comentarioLocalDao(): ComentarioLocalDao
    abstract fun reaccionLocalDao(): ReaccionLocalDao
    abstract fun favoritoLocalDao(): FavoritoLocalDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "el_sazon_de_toto_offline.db"
                )
                    .fallbackToDestructiveMigration() // Solo para desarrollo
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}