import android.content.Context
import androidx.room.Room

object CountryInfoDatabaseProvider {
    private var database: CountryInfoDatabase? = null

    // Utiliser le contexte de l'application pour éviter les fuites de mémoire
    fun getInstance(context: Context): CountryInfoDatabase {
        return database ?: synchronized(this) {
            // Réinitialiser la base de données si elle est nulle ou fermée
            database ?: buildDatabase(context).also { database = it }
        }
    }

    private fun buildDatabase(context: Context): CountryInfoDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            CountryInfoDatabase::class.java,
            "country_info_database"
        ).build()
    }
}
