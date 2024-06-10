package fr.epf.min1.findmycountry

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class MainActivity : AppCompatActivity() {
    private lateinit var countryRecyclerView: RecyclerView
    private lateinit var sortSpinner: Spinner
    private lateinit var searchBar: EditText
    private lateinit var searchButton: ImageButton
    private lateinit var showAllButton: Button
    private val countryList = ArrayList<JSONObject>()
    private val filteredCountryList = ArrayList<JSONObject>()
    private val favoriteCountries = mutableSetOf<String>()
    private lateinit var adapter: CountryAdapter

    companion object {
        const val PERMISSION_INTERNET_CODE = 1001
        const val TIMEOUT_MS = 180000 // 3 minutes in milliseconds
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Initialize views
        countryRecyclerView = findViewById(R.id.country_recycler_view)
        sortSpinner = findViewById(R.id.sort_spinner)
        searchBar = findViewById(R.id.search_bar)
        searchButton = findViewById(R.id.search_button)
        showAllButton = findViewById(R.id.show_all_button)
        countryRecyclerView.layoutManager = LinearLayoutManager(this)

        // Initialize adapter
        adapter = CountryAdapter(filteredCountryList, favoriteCountries, this)
        countryRecyclerView.adapter = adapter

        // Add spacing between items
        val spacingInPixels = resources.getDimensionPixelSize(R.dimen.item_spacing)
        countryRecyclerView.addItemDecoration(SpacingItemDecoration(spacingInPixels))

        // Set up sort spinner
        ArrayAdapter.createFromResource(
            this,
            R.array.sort_options,
            android.R.layout.simple_spinner_item
        ).also { arrayAdapter ->
            arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            sortSpinner.adapter = arrayAdapter
        }

        sortSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                sortCountries(position)
                // Show "Show All" button if "Favorites" filter is selected
                if (position == 5) {
                    showAllButton.visibility = View.VISIBLE
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        }

        searchButton.setOnClickListener {
            filterCountries(searchBar.text.toString())
        }

        showAllButton.setOnClickListener {
            showAllCountries()
            // Hide "Show All" button
            showAllButton.visibility = View.GONE
        }

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
    private fun addToFavorites(countryName: String) {
        favoriteCountries.add(countryName)
        Log.d("MainActivity", "Pays ajouté aux favoris : $countryName")
        // Assurez-vous de mettre à jour l'affichage après l'ajout du pays aux favoris
        adapter.notifyDataSetChanged()
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
                countryList.clear()
                for (i in 0 until response.length()) {
                    val countryObject = response.getJSONObject(i)
                    countryList.add(countryObject)
                }

                // Notify adapter of data change
                filterCountries(searchBar.text.toString())
                sortCountries(sortSpinner.selectedItemPosition)
            },
            Response.ErrorListener { error ->
                Log.e("MainActivity", "Error fetching country data", error)
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

    private fun filterCountries(query: String) {
        filteredCountryList.clear()
        if (query.isBlank()) {
            filteredCountryList.addAll(countryList)
        } else {
            val lowerCaseQuery = query.lowercase()
            filteredCountryList.addAll(countryList.filter {
                it.getJSONObject("name").optString("common", "").lowercase().contains(lowerCaseQuery) ||
                        it.optJSONArray("capital")?.join(",")?.lowercase()?.contains(lowerCaseQuery) ?: false
            })
        }
        adapter.notifyDataSetChanged()
        sortCountries(sortSpinner.selectedItemPosition)
    }

    private fun sortCountries(sortOption: Int) {
        when (sortOption) {
            0 -> filteredCountryList.sortBy { it.optDouble("area", 0.0) }
            1 -> filteredCountryList.sortByDescending { it.optDouble("area", 0.0) }
            2 -> filteredCountryList.sortBy { it.optInt("population", 0) }
            3 -> filteredCountryList.sortByDescending { it.optInt("population", 0) }
            4 -> filteredCountryList.sortBy { it.getJSONObject("name").optString("common", "N/A") }
            5 -> {
                val favoriteList = filteredCountryList.filter { favoriteCountries.contains(it.getJSONObject("name").optString("common", "")) }
                filteredCountryList.clear()
                filteredCountryList.addAll(favoriteList)
                // Show "Show All" button if the list is filtered by favorites
                showAllButton.visibility = View.VISIBLE

            }
        }

        adapter.notifyDataSetChanged()
    }
    

    private fun showAllCountries() {
        // Clear the search bar
        searchBar.text.clear()

        // Reset the filtered list to show all countries
        filteredCountryList.clear()
        filteredCountryList.addAll(countryList)

        // Reset the sort spinner to the first option ("Superficie croissante")
        sortSpinner.setSelection(0)

        // Sort the countries by the default option ("Superficie croissante")
        sortCountries(0)
    }
}