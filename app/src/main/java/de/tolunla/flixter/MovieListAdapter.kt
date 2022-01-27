package de.tolunla.flixter

import android.content.Intent
import android.content.res.Configuration
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.RoundedCornersTransformation
import de.tolunla.flixter.databinding.MovieListItemBinding
import de.tolunla.flixter.model.MovieModel
import de.tolunla.flixter.model.fetchMovie
import de.tolunla.flixter.model.fetchNowPlaying
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import java.text.SimpleDateFormat
import java.util.*

class MovieListAdapter(
    private val movieList: MutableList<MovieModel>,
    private val client: OkHttpClient
) :
    RecyclerView.Adapter<MovieListAdapter.ViewHolder>() {

    private var page = 1
    private var orientation: Int = Configuration.ORIENTATION_PORTRAIT
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val yearFormat = SimpleDateFormat("yyyy", Locale.getDefault())

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        orientation = recyclerView.context.resources.configuration.orientation

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                if (dy > 0) {
                    (recyclerView.layoutManager as LinearLayoutManager).let {
                        val visibleItems = it.childCount
                        val totalItems = it.itemCount

                        if (visibleItems + it.findFirstVisibleItemPosition() >= totalItems) {
                            fetchMovies()
                        }
                    }
                }
            }
        })

        fetchMovies()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = MovieListItemBinding.inflate(inflater, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val binding = holder.binding
        val movie = movieList[position]

        binding.title.text = movie.title
        binding.overview.text = movie.overview
        binding.rating.text = String.format("%.1f", movie.rating / 2)
        binding.rating.visibility = if (movie.voteCount > 0) View.VISIBLE else View.GONE

        dateFormat.parse(movie.releaseDate)?.let {
            binding.year.text = yearFormat.format(it)
        }

        binding.root.setOnClickListener { view ->
            CoroutineScope(Dispatchers.Default).launch {
                val movieModel = fetchMovie(client, BuildConfig.TMDB_API_KEY, movie.id)

                movieModel.onSuccess {
                    val intent = Intent(view.context, DetailActivity::class.java).apply {
                        putExtra(MovieModel::class.java.name, Json.encodeToString(it))
                    }

                    view.context.startActivity(intent)
                }

                movieModel.onFailure {
                    it.printStackTrace()
                }
            }
        }

        binding.image.load(
            "https://image.tmdb.org/t/p/w500${
                if (orientation == Configuration.ORIENTATION_PORTRAIT)
                    movie.posterPath else movie.backdropPath
            }"
        ) {
            crossfade(true)
            placeholder(R.drawable.ic_launcher)
            error(R.drawable.ic_launcher)
            transformations(RoundedCornersTransformation(12f))
        }
    }

    override fun getItemCount() = movieList.size

    private fun fetchMovies() {
        CoroutineScope(Dispatchers.Main).launch {

            val res = fetchNowPlaying(client, BuildConfig.TMDB_API_KEY, page)

            res.onSuccess {
                if (it.isEmpty()) return@launch

                movieList.addAll(it)
                notifyItemRangeInserted(itemCount - it.size, it.size)
                this@MovieListAdapter.page++
            }

            res.onFailure {
                it.printStackTrace()
            }
        }
    }

    inner class ViewHolder(val binding: MovieListItemBinding) :
        RecyclerView.ViewHolder(binding.root)
}