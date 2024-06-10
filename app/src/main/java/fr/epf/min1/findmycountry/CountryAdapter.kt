package fr.epf.min1.findmycountry

import CountryInfo
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import android.util.Log
import org.json.JSONObject
class CountryAdapter(
    private val countryList: List<JSONObject>,
    private val favoriteCountries: MutableSet<String>,
    private val context: Context
) : RecyclerView.Adapter<CountryAdapter.CountryViewHolder>() {

    class CountryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val countryFlag: ImageView = view.findViewById(R.id.country_flag)
        val countryName: TextView = view.findViewById(R.id.country_name)
        val countryInfo: TextView = view.findViewById(R.id.country_info)
        val favoriteStar: ImageView = view.findViewById(R.id.favorite_star)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CountryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.country_item, parent, false)
        return CountryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CountryViewHolder, position: Int) {
        val countryObject = countryList[position]
        val countryName = countryObject.getJSONObject("name").optString("common", "N/A")
        val capital = countryObject.optJSONArray("capital")?.optString(0, "N/A") ?: "N/A"
        val region = countryObject.optString("region", "N/A")
        val area = countryObject.optDouble("area", 0.0)
        val population = countryObject.optInt("population", 0)
        val languages = countryObject.optJSONObject("languages")?.let { languagesObj ->
            languagesObj.keys().asSequence().map { key -> languagesObj.getString(key) }.joinToString(", ")
        } ?: "N/A"
        val currencies = countryObject.optJSONObject("currencies")?.keys()?.asSequence()?.joinToString(", ") ?: "N/A"

        val countryInfo = "Capital: $capital\n" +
                "Region: $region\n" +
                "Area: $area\n" +
                "Population: $population\n" +
                "Languages: $languages\n" +
                "Currencies: $currencies"

        holder.countryName.text = countryName
        holder.countryInfo.text = countryInfo

        // Load flag image using Glide
        val flagUrl = countryObject.optJSONObject("flags")?.optString("png", "")
        Glide.with(holder.countryFlag.context)
            .load(flagUrl)
            .into(holder.countryFlag)

        // Set favorite star icon
        val isFavorite = favoriteCountries.contains(countryName)
        holder.favoriteStar.setImageResource(if (isFavorite) R.drawable.ic_star_filled else R.drawable.ic_star_border)
        holder.favoriteStar.setColorFilter(if (isFavorite) 0xFFFFD700.toInt() else 0xFFB0B0B0.toInt()) // Gold for favorite, gray otherwise

        holder.favoriteStar.setOnClickListener {
            if (isFavorite) {
                favoriteCountries.remove(countryName)
            } else {
                favoriteCountries.add(countryName)
                // Fetch and log country info when favorite star is clicked
                fetchAndLogCountryInfo(countryName)
            }
            notifyItemChanged(holder.adapterPosition)
        }

        holder.itemView.setOnClickListener {
            val intent = Intent(context, CountryDetailActivity::class.java).apply {
                putExtra("country_name", countryName)
            }
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return countryList.size
    }

    private fun fetchAndLogCountryInfo(countryName: String) {
        val queue = Volley.newRequestQueue(context)
        val url = "https://restcountries.com/v3.1/name/$countryName"

        val jsonArrayRequest = JsonArrayRequest(
            Request.Method.GET, url, null,
            Response.Listener { response ->
                if (response.length() > 0) {
                    val countryObject = response.getJSONObject(0)
                    val officialName = countryObject.getJSONObject("name").optString("official", "N/A")
                    val capital = countryObject.optJSONArray("capital")?.optString(0, "N/A") ?: "N/A"
                    val region = countryObject.optString("region", "N/A")
                    val subregion = countryObject.optString("subregion", "N/A")
                    val population = countryObject.optInt("population", 0)
                    val area = countryObject.optDouble("area", 0.0)
                    val languages = countryObject.optJSONObject("languages")?.let { languagesObj ->
                        languagesObj.keys().asSequence().map { key -> languagesObj.getString(key) }.joinToString(", ")
                    } ?: "N/A"
                    val currencies = countryObject.optJSONObject("currencies")?.keys()?.asSequence()?.joinToString(", ") ?: "N/A"

                    // Log the country info
                    // You can log or handle the country info as needed
                }
            },
            Response.ErrorListener { error ->
                // Handle error
            }
        )

        queue.add(jsonArrayRequest)
    }
}