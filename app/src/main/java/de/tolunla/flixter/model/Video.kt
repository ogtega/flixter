package de.tolunla.flixter.model

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
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
import java.text.SimpleDateFormat
import java.util.*


private val format = Json {
    ignoreUnknownKeys = true
}

@Serializable
data class Video(
    val site: String,
    val key: String,
    val official: Boolean,
    @SerialName("published_at") val releaseDate: String
) : Comparable<Video> {
    override fun compareTo(other: Video): Int {
        val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())

        return official.compareTo(other.official)
            .and(site.compareTo(other.site))
            .and((fmt.parse(releaseDate)?.compareTo((fmt.parse(other.releaseDate))) ?: 0) * -1)
    }
}

fun watchYouTube(context: Context, id: String) {
    val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:$id"))
    val webIntent = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("http://www.youtube.com/watch?v=$id")
    )
    try {
        context.startActivity(appIntent)
    } catch (ex: ActivityNotFoundException) {
        context.startActivity(webIntent)
    }
}

suspend fun fetchMovieVideo(
    client: OkHttpClient,
    apiKey: String,
    id: Int
): Result<List<Video>> {
    val request = Request.Builder()
        .url("https://api.themoviedb.org/3/movie/$id/videos?api_key=$apiKey")
        .build()

    return withContext(Dispatchers.IO) {
        kotlin.runCatching {
            client.newCall(request).execute().use { req ->
                if (!req.isSuccessful || req.code != 200) throw IOException("Request failed with code $req")
                val res = Json.parseToJsonElement(req.body!!.string())

                res.jsonObject["results"]!!.jsonArray.map {
                    format.decodeFromJsonElement(
                        Video.serializer(),
                        it
                    )
                }.sorted()
            }
        }
    }
}
