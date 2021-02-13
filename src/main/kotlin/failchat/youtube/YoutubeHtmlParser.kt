package failchat.youtube

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import java.util.regex.Pattern

class YoutubeHtmlParser(
        private val objectMapper: ObjectMapper
) {

    private companion object {
        val configStatementPattern = Pattern.compile("""ytcfg\.set\((\{.+\})\);""")!!
        val youtubeInitialDataPattern = Pattern.compile("""window\["ytInitialData"\] ?= ?(\{.+\});""")!!
    }

    fun parseYoutubeConfig(pageHtml: String): JsonNode {
        val matcher = configStatementPattern.matcher(pageHtml)
        val found = matcher.find()
        if (!found) {
            throw YoutubeClientException("Failed to extract youtube config from html page")
        }

        return objectMapper.readTree(matcher.group(1))
    }

    fun extractInnertubeApiKey(youtubeConfig: JsonNode): String {
        return youtubeConfig.get("INNERTUBE_API_KEY")?.textValue()
                ?: throw YoutubeClientException("Youtube config doesn't have 'INNERTUBE_API_KEY'")
    }

    fun parseInitialData(pageHtml: String): JsonNode {
        val matcher = youtubeInitialDataPattern.matcher(pageHtml)
        val found = matcher.find()
        if (!found) {
            throw YoutubeClientException("Failed to extract youtube initial data from html page")
        }
        return objectMapper.readTree(matcher.group(1))
    }

    fun extractInitialContinuation(ytInitialData: JsonNode): String {
        val continuation = ytInitialData.get("contents")
                ?.get("liveChatRenderer")
                ?.get("continuations")
                ?.get(0)

        val continuationData = continuation?.get("invalidationContinuationData")
                ?: continuation?.get("timedContinuationData")

        return continuationData?.get("continuation")
                ?.textValue()
                ?: throw YoutubeClientException("No continuation in youtube initial data")
    }

    fun extractChannelName(ytInitialData: JsonNode): String {
        return ytInitialData
                .get("contents")
                .get("liveChatRenderer")
                .get("participantsList")
                .get("liveChatParticipantsListRenderer")
                .get("participants")
                .first()
                .get("liveChatParticipantRenderer")
                .get("authorName")
                .get("simpleText")
                .textValue()
    }

}
