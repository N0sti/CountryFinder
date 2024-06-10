import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CountryInfoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(countryInfo: CountryInfo)

    @Query("SELECT * FROM country_info WHERE countryName = :countryName")
    fun getCountryInfo(countryName: String): CountryInfo?
}
