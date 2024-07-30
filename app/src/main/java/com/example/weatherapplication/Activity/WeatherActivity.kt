package com.example.weatherapplication.Activity

import android.os.Bundle
import android.util.Log
import androidx.appcompat.widget.SearchView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.weatherapplication.Model.CurrentResponseApi
import com.example.weatherapplication.Model.GeocodeResponse
import com.example.weatherapplication.R
import com.example.weatherapplication.ViewModel.WeatherViewModel
import com.example.weatherapplication.databinding.ActivityWeatherBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Calendar


class WeatherActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWeatherBinding
    private val weatherViewModel: WeatherViewModel by viewModels()
    private val apiKey = "4b03a2bc72bbb54c777ad25fd395a272"
    var city = "Trichy"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWeatherBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (query != null) {
                    city = query
                    fetchCoordinates(city) // Call fetchCoordinates when the query is submitted
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })

        fetchCoordinates(city)
    }

    private fun fetchCoordinates(city: String) {
        weatherViewModel.getCoordinates(city, 1, apiKey).enqueue(object : Callback<List<GeocodeResponse>> {
            override fun onResponse(call: Call<List<GeocodeResponse>>, response: Response<List<GeocodeResponse>>) {
                if (response.isSuccessful) {
                    val geocodeResponse = response.body()?.firstOrNull()
                    geocodeResponse?.let {
                        val lat = it.lat ?: 0.0
                        val lon = it.lon ?: 0.0
                        fetchWeather(lat, lon)
                    }
                } else {
                    val errorMessage = response.errorBody()?.string()
                    Log.e("WeatherActivity", "Failed to fetch coordinates: ${response.code()} - $errorMessage")
                    Toast.makeText(this@WeatherActivity, "Failed to get coordinates", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<GeocodeResponse>>, t: Throwable) {
                Log.e("WeatherActivity", "Failure: ${t.message}")
                t.printStackTrace() // Print the full stack trace
                Toast.makeText(this@WeatherActivity, t.toString(), Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun fetchWeather(lat: Double, lon: Double) {
        weatherViewModel.loadingCurrentWeather(lat, lon, "metric").enqueue(object : Callback<CurrentResponseApi> {
            override fun onResponse(call: Call<CurrentResponseApi>, response: Response<CurrentResponseApi>) {
                if (response.isSuccessful) {
                    val data = response.body()
                    data?.let {
                        Log.d("WeatherActivity", "Updating UI with data: $data")
                        updateUI(it)
                    }
                } else {
                    val errorMessage = response.errorBody()?.string()
                    Log.e("WeatherActivity", "Error: ${response.code()} - $errorMessage")
                    Toast.makeText(this@WeatherActivity, "Failed to get weather data", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<CurrentResponseApi>, t: Throwable) {
                Log.e("WeatherActivity", "Failure: ${t.message}")
                Toast.makeText(this@WeatherActivity, t.toString(), Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun formatTime(timestamp: Int?): String {
        return if (timestamp != null) {
            val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
            val date = java.util.Date(timestamp * 1000L)
            sdf.format(date)
        } else {
            "N/A"
        }
    }

    private fun updateUI(data: CurrentResponseApi) {
        binding.tvLocation.text = city
        binding.temperatureText.text = "${data.main?.temp}° C"
        binding.realFeelText.text = "Real Feel: ${data.main?.feelsLike}° C"
        binding.minTemperatureText.text = "${data.main?.tempMin}° C"
        binding.maxTemperatureText.text = "${data.main?.tempMax}° C"
        binding.latValue.text = data.coord?.lat.toString()
        binding.lonValue.text = data.coord?.lon.toString()
        binding.humidityValue.text = "${data.main?.humidity}%"
        binding.pressureValue.text = "${data.main?.pressure} hPa"
        binding.windSpeedText.text = "${data.wind?.speed} km/h"
        binding.sunriseTime.text = formatTime(data.sys?.sunrise)
        binding.sunsetTime.text = formatTime(data.sys?.sunset)

        // Update GIFs or images based on weather conditions
        data.weather?.get(0)?.let {
            val weatherCode = it.id
            val weatherDescription = it.description
            Log.d("code","showing code  $weatherCode $weatherDescription")
            if (weatherCode != null) {
                if (weatherDescription != null) {
                    updateWeatherGif(weatherCode, weatherDescription)
                }
            }
        }
    }

    private fun updateWeatherGif(weatherCode: Int, weatherDescription: String) {
        Log.d("WeatherUpdate", "Weather Code: $weatherCode, Description: $weatherDescription")
        when (weatherCode) {
            in 200..232 -> { // Thunderstorm
                binding.climateGif.setImageResource(R.drawable.storm_status)
                binding.backgroundImageView.setImageResource(R.drawable.storm_bg)
            }
            in 300..321 -> { // Drizzle
                binding.climateGif.setImageResource(R.drawable.drizzle_status)
                binding.backgroundImageView.setImageResource(R.drawable.drizzle_bg)
            }
            in 500..531 -> { // Rain
                binding.climateGif.setImageResource(R.drawable.rainy_status_2)
                binding.backgroundImageView.setImageResource(R.drawable.rainy_bg)
            }
            in 600..622 -> { // Snow
                binding.climateGif.setImageResource(R.drawable.snow_status)
                binding.backgroundImageView.setImageResource(R.drawable.snow_bg)
            }
            in 701..781 -> { // Atmosphere
                binding.climateGif.setImageResource(R.drawable.fog_mist_status)
                binding.backgroundImageView.setImageResource(R.drawable.fog_bg)
            }
            800 -> { // Clear
                val calendar = Calendar.getInstance()
                val hour = calendar.get(Calendar.HOUR_OF_DAY)
                if (hour in 6..18) { // Morning (6 AM to 6 PM)
                    binding.climateGif.setImageResource(R.drawable.clear_status)
                    binding.backgroundImageView.setImageResource(R.drawable.mrng_clear_bg)
                } else { // Night
                    binding.climateGif.setImageResource(R.drawable.clear_status)
                    binding.backgroundImageView.setImageResource(R.drawable.night_clear_bg)
                }
            }
            in 801..804 -> { // Clouds
                binding.climateGif.setImageResource(R.drawable.cloudy_status)
                binding.backgroundImageView.setImageResource(R.drawable.cloudy_bg)
            }
            else -> {
                binding.climateGif.setImageResource(R.drawable.clear_status)
                binding.backgroundImageView.setImageResource(R.drawable.night_clear_bg)
            }
        }
    }

}
