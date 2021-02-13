package failchat.youtube

import failchat.ktorClient
import failchat.testObjectMapper
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import kotlinx.coroutines.runBlocking
import org.junit.Ignore
import org.junit.Test

@Ignore
class YoutubeClientTest {

    private val client = YoutubeClient(
            httpClient = ktorClient,
            objectMapper = testObjectMapper,
            youtubeHtmlParser = YoutubeHtmlParser(objectMapper = testObjectMapper)
    )

    @Test
    fun getViewersCountTest() = runBlocking<Unit> {
        val videoId = "5qap5aO4i9A"
        val innertubeApiKey = client.getNewLiveChatSessionData(videoId).innertubeApiKey

        val count = client.getViewersCount(videoId, innertubeApiKey)

        count shouldBeGreaterThanOrEqual 0
        println(count)
    }

}
