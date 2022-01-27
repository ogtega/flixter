package de.tolunla.flixter

import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import coil.load
import coil.transform.RoundedCornersTransformation
import com.google.android.material.chip.Chip
import com.google.android.youtube.player.YouTubeInitializationResult
import com.google.android.youtube.player.YouTubePlayer
import com.google.android.youtube.player.YouTubePlayerFragmentX
import de.tolunla.flixter.databinding.DetailActivityBinding
import de.tolunla.flixter.model.MovieModel
import de.tolunla.flixter.model.fetchMovieVideo
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import java.text.SimpleDateFormat
import java.util.*

class DetailActivity : AppCompatActivity() {
    private lateinit var movieModel: MovieModel
    private val client = OkHttpClient.Builder().build()
    private lateinit var binding: DetailActivityBinding
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val yearFormat = SimpleDateFormat("yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }

        binding = DetailActivityBinding.inflate(layoutInflater)
        setSupportActionBar(binding.toolbar)

        lifecycleScope.launch {
            movieModel = Json.decodeFromString(intent.getStringExtra(MovieModel::class.java.name)!!)

            binding.title.text = movieModel.title
            binding.overview.text = movieModel.overview
            binding.rating.text = String.format("%.1f", movieModel.rating / 2)

            dateFormat.parse(movieModel.releaseDate)?.let {
                binding.year.text = yearFormat.format(it)
            }

            movieModel.genres.map {
                Chip(this@DetailActivity).apply {
                    text = it.name
                }
            }.forEach {
                binding.genreChips.addView(it, binding.genreChips.childCount - 1)
            }

            binding.runtime.text =
                String.format("%dh %dm", movieModel.runtime / 60, movieModel.runtime % 60)

            binding.reviews.adapter = ReviewListAdapter(movieModel.id, mutableListOf(), client)
        }

        setContentView(binding.root)
    }

    override fun onStart() {
        super.onStart()

        lifecycleScope.launch {
            val videoRes = fetchMovieVideo(client, BuildConfig.TMDB_API_KEY, movieModel.id)

            videoRes.onSuccess { videos ->
                if (videos.isNotEmpty()) {
                    (supportFragmentManager.findFragmentById(R.id.youtube_player) as YouTubePlayerFragmentX).initialize(
                        BuildConfig.YOUTUBE_API_KEY,
                        object : YouTubePlayer.OnInitializedListener {
                            override fun onInitializationSuccess(
                                provider: YouTubePlayer.Provider?,
                                player: YouTubePlayer?,
                                b: Boolean
                            ) {
                                player?.let { p ->
                                    p.cueVideos(videos.filter { it.site.lowercase() == "youtube" }
                                        .map { it.key })
                                    p.setPlaybackEventListener(object :
                                        YouTubePlayer.PlaybackEventListener {
                                        override fun onPlaying() {
                                            p.setFullscreen(true)
                                        }

                                        override fun onPaused() {}

                                        override fun onStopped() {}

                                        override fun onBuffering(p0: Boolean) {}

                                        override fun onSeekTo(p0: Int) {}

                                    })
                                }
                            }

                            override fun onInitializationFailure(
                                provider: YouTubePlayer.Provider?,
                                result: YouTubeInitializationResult?
                            ) {
                                Log.d(this::class.java.name, result.toString())
                            }

                        })
                } else {
                    binding.youtubePlayer.visibility = View.GONE
                    binding.image.visibility = View.VISIBLE

                    binding.image.load("https://image.tmdb.org/t/p/w500${movieModel.backdropPath}") {
                        crossfade(true)
                        placeholder(R.drawable.ic_launcher)
                        error(R.drawable.ic_launcher)
                        transformations(RoundedCornersTransformation(12f, 12f))
                    }
                }
            }

            videoRes.onFailure {
                it.printStackTrace()
            }
        }
    }
}