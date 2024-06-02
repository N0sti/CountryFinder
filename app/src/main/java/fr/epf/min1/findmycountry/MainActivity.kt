package fr.epf.min1.findmycountry

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class MainActivity : AppCompatActivity() {
    private lateinit var countrySpinner: Spinner
    private lateinit var countryInfoTextView: TextView

    companion object {
        const val PERMISSION_INTERNET_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Initialize views
        countrySpinner = findViewById(R.id.country_spinner)
        countryInfoTextView = findViewById(R.id.country_info_text_view)

        // Check if the permission is granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
            // If the permission is not granted, request it from the user
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.INTERNET), PERMISSION_INTERNET_CODE)
        } else {
            // If the permission is already granted, fetch country data
            fetchCountryData()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_INTERNET_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, fetch country data
                fetchCountryData()
            } else {
                // Permission denied, you may want to handle this case
                Log.e("MainActivity", "Internet permission denied")
            }
        }
    }

    private fun fetchCountryData() {
        val queue = Volley.newRequestQueue(this)
        val url = "https://restcountries.com/v3.1/all"

        Log.d("MainActivity", "Starting request to $url")

        val jsonArrayRequest = JsonArrayRequest(
            Request.Method.GET, url, null,
            Response.Listener { response ->
                Log.d("MainActivity", "Received response: ${response.length()} countries")
                // Parse JSON response
                val countryList = ArrayList<String>()
                val countryInfoMap = HashMap<String, JSONObject>()
                for (i in 0 until response.length()) {
                    val countryObject = response.getJSONObject(i)
                    val countryName = countryObject.getJSONObject("name").getString("common")
                    countryList.add(countryName)
                    countryInfoMap[countryName] = countryObject
                }

                // Set up country spinner
                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, countryList)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                countrySpinner.adapter = adapter

                // Handle spinner item selection
                countrySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        adapterView: AdapterView<*>?,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        val selectedCountryName = adapterView?.getItemAtPosition(position).toString()
                        Log.d("MainActivity", "Selected country: $selectedCountryName")
                        displayCountryInfo(countryInfoMap[selectedCountryName])
                    }

                    override fun onNothingSelected(p0: AdapterView<*>?) {
                        Log.d("MainActivity", "No country selected")
                    }
                }
            },
            Response.ErrorListener { error ->
                Log.e("MainActivity", "Error fetching country data", error)
            }
        )

        queue.add(jsonArrayRequest)
    }

    private fun displayCountryInfo(countryObject: JSONObject?) {
        countryObject?.let {
            val countryName = it.getJSONObject("name").optString("common", "N/A")
            val capital = it.optJSONArray("capital")?.optString(0, "N/A") ?: "N/A"
            val region = it.optString("region", "N/A")
            val area = it.optDouble("area", 0.0).toString() + " km²"
            val population = it.optInt("population", 0).toString()
            val currency = it.optJSONObject("currencies")?.keys()?.asSequence()?.firstOrNull()
            val currencyName = if (currency != null) it.optJSONObject("currencies")?.getJSONObject(currency)?.optString("name", "N/A") else "N/A"
            val languages = it.optJSONObject("languages")?.let { languagesObj ->
                languagesObj.keys().asSequence().map { key -> languagesObj.getString(key) }.joinToString(", ")
            } ?: "N/A"

            val countryInfo = "Fiche pays synthétique :\n" +
                    "Nom : $countryName\n" +
                    "Capitale : $capital\n" +
                    "Région : $region\n" +
                    "Superficie : $area\n" +
                    "Population : $population\n" +
                    "Devise : $currencyName\n" +
                    "Langue : $languages"
            countryInfoTextView.text = countryInfo

            Log.d("MainActivity", "Displayed info for $countryName")
        } ?: Log.e("MainActivity", "Country object is null")
    }
}
