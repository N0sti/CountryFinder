package fr.epf.min1.findmycountry

import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.Volley
import com.squareup.picasso.Picasso
import org.json.JSONObject

class CountryDetailActivity : AppCompatActivity() {
    private lateinit var countryNameTextView: TextView
    private lateinit var officialNameTextView: TextView
    private lateinit var capitalTextView: TextView
    private lateinit var regionTextView: TextView
    private lateinit var subregionTextView: TextView
    private lateinit var populationTextView: TextView
    private lateinit var areaTextView: TextView
    private lateinit var languagesTextView: TextView
    private lateinit var currenciesTextView: TextView
    private lateinit var flagImageView: ImageView

    companion object {
        const val TAG = "CountryDetailActivity"
        const val TIMEOUT_MS = 180000 // 3 minutes in milliseconds
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_country_detail)

        countryNameTextView = findViewById(R.id.country_name)
        officialNameTextView = findViewById(R.id.official_name)
        capitalTextView = findViewById(R.id.capital)
        regionTextView = findViewById(R.id.region)
        subregionTextView = findViewById(R.id.subregion)
        populationTextView = findViewById(R.id.population)
        areaTextView = findViewById(R.id.area)
        languagesTextView = findViewById(R.id.languages)
        currenciesTextView = findViewById(R.id.currencies)
        flagImageView = findViewById(R.id.flag_image)

        val countryName = intent.getStringExtra("country_name")
        if (countryName != null) {
            fetchCountryDetails(countryName)
        } else {
            Log.e(TAG, "Country name not found in intent extras")
        }
    }

    private fun fetchCountryDetails(countryName: String) {
        val queue = Volley.newRequestQueue(this)
        val url = "https://restcountries.com/v3.1/name/$countryName"

        Log.d(TAG, "Starting request to $url")

        val jsonArrayRequest = JsonArrayRequest(
            Request.Method.GET, url, null,
            Response.Listener { response ->
                Log.d(TAG, "Received response: ${response.length()} countries")
                if (response.length() > 0) {
                    val countryObject = response.getJSONObject(0)
                    displayCountryDetails(countryObject)
                }
            },
            Response.ErrorListener { error ->
                Log.e(TAG, "Error fetching country details", error)
            }
        )

        // Set the retry policy to wait up to 3 minutes for a response
        jsonArrayRequest.retryPolicy = DefaultRetryPolicy(
            TIMEOUT_MS,
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )

        queue.add(jsonArrayRequest)
    }

    private fun displayCountryDetails(country: JSONObject) {
        val name = country.getJSONObject("name").optString("common", "N/A")
        val officialName = country.getJSONObject("name").optString("official", "N/A")
        val capital = country.optJSONArray("capital")?.optString(0, "N/A") ?: "N/A"
        val region = country.optString("region", "N/A")
        val subregion = country.optString("subregion", "N/A")
        val population = country.optInt("population", 0).toString()
        val area = country.optDouble("area", 0.0).toString() + " km²"
        val languages = country.optJSONObject("languages")?.let { languagesObj ->
            languagesObj.keys().asSequence().map { key -> languagesObj.getString(key) }.joinToString(", ")
        } ?: "N/A"
        val currencies = country.optJSONObject("currencies")?.let { currenciesObj ->
            currenciesObj.keys().asSequence().map { key ->
                val currency = currenciesObj.getJSONObject(key)
                "${currency.optString("name", "N/A")} (${currency.optString("symbol", "N/A")})"
            }.joinToString(", ")
        } ?: "N/A"
        val flagUrl = country.optJSONObject("flags")?.optString("png", "")

        countryNameTextView.text = name
        officialNameTextView.text = "Official Name: $officialName"
        capitalTextView.text = "Capital: $capital"
        regionTextView.text = "Region: $region"
        subregionTextView.text = "Subregion: $subregion"
        populationTextView.text = "Population: $population"
        areaTextView.text = "Area: $area"
        languagesTextView.text = "Languages: $languages"
        currenciesTextView.text = "Currencies: $currencies"

        if (!flagUrl.isNullOrEmpty()) {
            Picasso.get().load(flagUrl).into(flagImageView)
        }
    }
}