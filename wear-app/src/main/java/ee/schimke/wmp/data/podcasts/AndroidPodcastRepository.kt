package ee.schimke.wmp.data.podcasts

import com.google.android.horologist.media.model.Media
import com.prof.rssparser.Parser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import okhttp3.executeAsync
import java.io.IOException

class AndroidPodcastRepository(
    private val client: Call.Factory,
    private val parser: Parser
) {
    private val url = "https://feeds.libsyn.com/244409/rss".toHttpUrl()

    suspend fun query(): List<Media> {
        return withContext(Dispatchers.Default) {
            client.newCall(Request(url)).executeAsync().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("Request failed: ${response.code} ${response.message}")
                }

                val model = parser.parse(response.body.string())

                model.articles.mapIndexedNotNull { i, it ->
                    if (it.audio != null) {
                        Media(
                            it.guid ?: i.toString(),
                            it.audio!!,
                            it.title ?: "No Title",
                            it.itunesArticleData?.author.orEmpty(),
                            it.image
                        )
                    } else {
                        null
                    }
                }
            }
        }
    }
}