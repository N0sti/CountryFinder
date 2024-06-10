import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "country_info")
data class CountryInfo(
    @PrimaryKey
    val countryName: String,
    val officialName: String,
    val capital: String,
    val region: String,
    val subregion: String,
    val population: Int,
    val area: Double,
    val languages: String,
    val currencies: String
)
