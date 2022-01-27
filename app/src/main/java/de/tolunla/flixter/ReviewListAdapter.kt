package de.tolunla.flixter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.RoundedCornersTransformation
import de.tolunla.flixter.databinding.ReviewListItemBinding
import de.tolunla.flixter.model.Review
import de.tolunla.flixter.model.fetchReviews
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

class ReviewListAdapter(
    private val movieId: Int,
    private val reviewList: MutableList<Review>,
    private val client: OkHttpClient
) : RecyclerView.Adapter<ReviewListAdapter.ViewHolder>() {

    private var page = 1

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                if (dy > 0) {
                    (recyclerView.layoutManager as LinearLayoutManager).let {
                        val visibleItems = it.childCount
                        val totalItems = it.itemCount

                        if (visibleItems + it.findFirstVisibleItemPosition() >= totalItems) {
                            fetchReviews()
                        }
                    }
                }
            }
        })

        fetchReviews()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ReviewListItemBinding.inflate(inflater, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val binding = holder.binding
        val review = reviewList[position]
        val author = review.author

        binding.displayName.text = if (author.name.isNullOrBlank()) author.username else author.name
        binding.content.text = (review.content ?: "").split('\n')[0]
        binding.rating.text = (review.author.rating ?: "").toString()

        binding.profileImage.load(
            "https://secure.gravatar.com/avatar/${
                author.avatar?.split('/')?.last()
            }"
        ) {
            crossfade(true)
            placeholder(R.drawable.ic_launcher)
            error(R.drawable.ic_launcher)
            transformations(RoundedCornersTransformation(90f))
        }
    }

    override fun getItemCount() = reviewList.size

    private fun fetchReviews() {
        CoroutineScope(Dispatchers.Main).launch {
            val res = fetchReviews(client, BuildConfig.TMDB_API_KEY, movieId, page)

            res.onSuccess {
                if (it.isEmpty()) return@launch

                reviewList.addAll(it)
                notifyItemRangeInserted(itemCount - it.size, it.size)
                this@ReviewListAdapter.page++
            }

            res.onFailure {
                it.printStackTrace()
            }
        }
    }

    inner class ViewHolder(val binding: ReviewListItemBinding) :
        RecyclerView.ViewHolder(binding.root)
}