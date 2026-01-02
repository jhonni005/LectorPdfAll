package com.zonadev.lectordocumentos.data.local
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.zonadev.lectordocumentos.data.local.entities.LocalPdfEntity

/**
 * Base de datos principal de la aplicación.
 * Define las entidades y la versión de la base de datos.
 */
@Database(
    entities = [LocalPdfEntity::class], // Lista de tablas
    version = 1,                       // Versión del esquema (incrementar al modificar tablas)
    exportSchema = false               // Desactivado para evitar advertencias de compilación en proyectos simples
)
abstract class PdfDatabase : RoomDatabase() {

    // Acceso al DAO
    abstract fun pdfDao(): PdfDao

    companion object {
        @Volatile
        private var INSTANCE: PdfDatabase? = null

        fun getDatabase(context: Context): PdfDatabase {
            // Patrón Singleton para evitar múltiples instancias costosas de la BD
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PdfDatabase::class.java,
                    "pdf_app_database" // Nombre del archivo físico de la BD
                )
                    // Opcional: .fallbackToDestructiveMigration() si estás en desarrollo y cambias el esquema
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}