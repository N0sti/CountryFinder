package fr.epf.min1.findmycountry

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
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
    private val countryList = ArrayList<JSONObject>()
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
        countryRecyclerView.layoutManager = LinearLayoutManager(this)

        // Initialize adapter
        adapter = CountryAdapter(countryList, favoriteCountries)
        countryRecyclerView.adapter = adapter

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
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
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
                adapter.notifyDataSetChanged()
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

    private fun sortCountries(sortOption: Int) {
        when (sortOption) {
            0 -> countryList.sortBy { it.optDouble("area", 0.0) }
            1 -> countryList.sortByDescending { it.optDouble("area", 0.0) }
            2 -> countryList.sortBy { it.optInt("population", 0) }
            3 -> countryList.sortByDescending { it.optInt("population", 0) }
            4 -> countryList.sortBy { it.getJSONObject("name").optString("common", "N/A") }
            5 -> {
                val favoriteList = countryList.filter { favoriteCountries.contains(it.getJSONObject("name").optString("common", "")) }
                countryList.clear()
                countryList.addAll(favoriteList)
            }
        }
        adapter.notifyDataSetChanged()
    }
}
