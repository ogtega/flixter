package de.tolunla.flixter

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import de.tolunla.flixter.databinding.ActivityMainBinding
import okhttp3.OkHttpClient

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val client = OkHttpClient.Builder().build()
    private val movieListAdapter = MovieListAdapter(mutableListOf(), client)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)

        binding.movieList.adapter = movieListAdapter
        binding.movieList.setHasFixedSize(true)
        setContentView(binding.root)
    }
}