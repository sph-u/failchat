package failchat.youtube

import failchat.readResourceAsString
import failchat.testObjectMapper
import io.kotest.matchers.shouldBe
import org.junit.Test

class YoutubeHtmlParserTest {

    private val youtubeHtmlParser = YoutubeHtmlParser(testObjectMapper)

    @Test
    fun `should extract innertubeApiKey`() {
        // Given
        val html = readResourceAsString("/html/live_chat.html")

        // When
        val youtubeConfig = youtubeHtmlParser.parseYoutubeConfig(html)
        val actual = youtubeHtmlParser.extractInnertubeApiKey(youtubeConfig)

        // Then
        actual shouldBe "AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8"
    }

    @Test
    fun `should extract initial continuation`() {
        // Given
        val html = readResourceAsString("/html/live_chat.html")

        // When
        val initialData = youtubeHtmlParser.parseInitialData(html)
        val actual = youtubeHtmlParser.extractInitialContinuation(initialData)

        // Then
        actual shouldBe "0ofMyAOqARpeQ2lrcUp3b1lWVU5UU2pSbmExWkROazV5ZGtsSk9IVnRlblJtTUU5M0VnczFjV0Z3TldGUE5HazVRUm9UNnFqZHVRRU5DZ3MxY1dGd05XRlBOR2s1UVNBQ0tBRSUzRCivjJ_s9ebuAjAAOABAAUoVCAEQABgAIABQx6K47fXm7gJYA3gAULXAwuz15u4CWLTkrPfk4u4CggECCASIAQCgAd7Uue315u4C"
    }

    @Test
    fun `should extract channel name`() {
        // Given
        val html = readResourceAsString("/html/live_chat.html")

        // When
        val initialData = youtubeHtmlParser.parseInitialData(html)
        val actual = youtubeHtmlParser.extractChannelName(initialData)

        // Then
        actual shouldBe "ChilledCow"
    }

}
