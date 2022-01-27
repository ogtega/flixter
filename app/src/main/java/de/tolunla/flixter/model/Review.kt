package de.tolunla.flixter.model

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

private val format = Json {
    ignoreUnknownKeys = true
}

@Serializable
data class Review(
    @SerialName("author_details") val author: Author,
    val content: String?,
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class Author(
    val name: String?,
    val username: String?,
    @SerialName("avatar_path") val avatar: String?,
    val rating: Int?
)

suspend fun fetchReviews(
    client: OkHttpClient,
    apiKey: String,
    id: Int,
    page: Int = 1
): Result<List<Review>> {
    val request = Request.Builder()
        .url("https://api.themoviedb.org/3/movie//$id/reviews?api_key=$apiKey&page=$page")
        .build()

    return withContext(Dispatchers.IO) {
        kotlin.runCatching {
            client.newCall(request).execute().use { req ->
                if (!req.isSuccessful || req.code != 200) throw IOException("Request failed with code $req")
                val res = Json.parseToJsonElement(req.body!!.string())

                res.jsonObject["results"]!!.jsonArray.map {
                    format.decodeFromJsonElement(
                        Review.serializer(),
                        it
                    )
                }
            }
        }
    }
}
