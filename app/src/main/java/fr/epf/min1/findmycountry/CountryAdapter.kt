package fr.epf.min1.findmycountry

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONObject

class CountryAdapter(
    private val countryList: List<JSONObject>,
    private val favoriteCountries: MutableSet<String>
) : RecyclerView.Adapter<CountryAdapter.CountryViewHolder>() {

    class CountryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val countryName: TextView = view.findViewById(R.id.country_name)
        val countryInfo: TextView = view.findViewById(R.id.country_info)
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
        val area = countryObject.optDouble("area", 0.0).toString() + " km²"
        val population = countryObject.optInt("population", 0).toString()
        val currency = countryObject.optJSONObject("currencies")?.keys()?.asSequence()?.firstOrNull()
        val currencyName = if (currency != null) countryObject.optJSONObject("currencies")?.getJSONObject(currency)?.optString("name", "N/A") else "N/A"
        val languages = countryObject.optJSONObject("languages")?.let { languagesObj ->
            languagesObj.keys().asSequence().map { key -> languagesObj.getString(key) }.joinToString(", ")
        } ?: "N/A"

        val countryInfo = "Capitale : $capital\n" +
                "Région : $region\n" +
                "Superficie : $area\n" +
                "Population : $population\n" +
                "Devise : $currencyName\n" +
                "Langue : $languages"

        holder.countryName.text = countryName
        holder.countryInfo.text = countryInfo

        holder.itemView.setOnClickListener {
            if (favoriteCountries.contains(countryName)) {
                favoriteCountries.remove(countryName)
            } else {
                favoriteCountries.add(countryName)
            }
            notifyItemChanged(position)
        }

        holder.itemView.setBackgroundColor(
            if (favoriteCountries.contains(countryName)) {
                0xFFD700 // Gold color for favorites
            } else {
                0xFFFFFF // White color for non-favorites
            }.toInt()
        )
    }

    override fun getItemCount(): Int {
        return countryList.size
    }
}
