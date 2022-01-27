package de.tolunla.flixter.model

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
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
data class MovieModel(
    val id: Int,
    val title: String,
    val overview: String,
    @SerialName("poster_path") val posterPath: String?,
    @SerialName("release_date") val releaseDate: String,
    @SerialName("vote_average") val rating: Float,
    @SerialName("backdrop_path") val backdropPath: String?,
    @SerialName("genres") val genres: List<Genre> = listOf(),
    @SerialName("vote_count") val voteCount: Int,
    val runtime: Int = 0
)

@Serializable
data class Genre(val id: Int, val name: String)

suspend fun fetchMovie(
    client: OkHttpClient,
    apiKey: String,
    id: Int
): Result<MovieModel> {
    val request = Request.Builder()
        .url("https://api.themoviedb.org/3/movie/$id?api_key=$apiKey")
        .build()

    return withContext(Dispatchers.IO) {
        kotlin.runCatching {
            client.newCall(request).execute().use { req ->
                if (!req.isSuccessful || req.code != 200) throw IOException("Request failed with code $req")
                format.decodeFromString<MovieModel>(req.body!!.string())
            }
        }
    }
}

suspend fun fetchNowPlaying(
    client: OkHttpClient,
    apiKey: String,
    page: Int = 1
): Result<List<MovieModel>> {
    val request = Request.Builder()
        .url("https://api.themoviedb.org/3/movie/now_playing?api_key=$apiKey&page=$page")
        .build()

    return withContext(Dispatchers.IO) {
        kotlin.runCatching {
            client.newCall(request).execute().use { req ->
                if (!req.isSuccessful || req.code != 200) throw IOException("Request failed with code $req")
                val res = Json.parseToJsonElement(req.body!!.string())

                res.jsonObject["results"]!!.jsonArray.map {
                    format.decodeFromJsonElement(
                        MovieModel.serializer(),
                        it
                    )
                }
            }
        }
    }
}
