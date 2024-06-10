import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [CountryInfo::class], version = 1)
abstract class CountryInfoDatabase : RoomDatabase() {
    abstract fun countryInfoDao(): CountryInfoDao
}
