package failchat.youtube

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.headers
import io.ktor.client.request.request
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpMethod
import io.ktor.http.isSuccess
import io.ktor.http.takeFrom
import io.ktor.util.toByteArray
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mu.KLogging

class YoutubeClient(
        private val httpClient: HttpClient,
        private val objectMapper: ObjectMapper,
        private val youtubeHtmlParser: YoutubeHtmlParser
) {

    private companion object : KLogging() {
        const val liveChatUrl = "https://www.youtube.com/youtubei/v1/live_chat/get_live_chat"
        const val chatPageUrl = "https://www.youtube.com/live_chat?is_popout=1&v="
        const val metadataUrl = "https://www.youtube.com/youtubei/v1/updated_metadata?key="
        val metadataRequestContext = UpdatedMetadataRequest.Context(UpdatedMetadataRequest.Client(
                clientName = "WEB",
                clientVersion = "2.20210120.08.00"
        ))
    }

    private val viewCountParser = YoutubeViewCountParser()


    suspend fun getNewLiveChatSessionData(videoId: String): LiveChatRequestParameters {
        val response = httpClient.request<HttpResponse> {
            method = HttpMethod.Get
            url(chatPageUrl + videoId)
            header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:79.0) Gecko/20100101 Firefox/79.0")
            header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
            header("Accept-Language", "en-GB,en;q=0.5")
            header("DNT", "1")
            header("Connection", "keep-alive")
            header("Upgrade-Insecure-Requests", "1")
            header("TE", "Trailers")
        }

        if (!response.status.isSuccess()) {
            throw YoutubeClientException("Failed to get live chat page. Video id: '$videoId', status: ${response.status}")
        }

        val pageHtml = response.content.toByteArray().toString(Charsets.UTF_8)

        val youtubeConfig = youtubeHtmlParser.parseYoutubeConfig(pageHtml)
        val innertubeApiKey = youtubeHtmlParser.extractInnertubeApiKey(youtubeConfig)

        val ytInitialData = youtubeHtmlParser.parseInitialData(pageHtml)
        val initialContinuation = youtubeHtmlParser.extractInitialContinuation(ytInitialData)
        val channelName = youtubeHtmlParser.extractChannelName(ytInitialData)

        return LiveChatRequestParameters(
                videoId = videoId,
                channelName = channelName,
                innertubeApiKey = innertubeApiKey,
                nextContinuation = initialContinuation
        )
    }

    suspend fun getLiveChatResponse(parameters: LiveChatRequestParameters): LiveChatResponse {
        val requestBodyDto = LiveChatRequest(continuation = parameters.nextContinuation)
        val requestBody = objectMapper.writeValueAsString(requestBodyDto)

        val response = httpClient.request<HttpResponse> {
            method = HttpMethod.Post
            url {
                takeFrom(liveChatUrl).parameters.apply {
                    append("key", parameters.innertubeApiKey)
                }
            }
            headers {
                append("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:79.0) Gecko/20100101 Firefox/79.0")
                append("Accept", "*/*")
                append("Accept-Language", "en-GB,en;q=0.5")
                append("Origin", "https://www.youtube.com")
                append("DNT", "1")
                append("Connection", "keep-alive")
                append("TE", "Trailers")
            }
            body = requestBody
        }

        if (!response.status.isSuccess()) {
            throw YoutubeClientException("Failed to get live chat api response. Video id: '${parameters.videoId}', status: ${response.status}")
        }

        return objectMapper.readValue<LiveChatResponse>(response.content.toByteArray())
    }

    suspend fun getViewersCount(videoId: String, innertubeApiKey: String): Int {
        val requestBody = UpdatedMetadataRequest(metadataRequestContext, videoId)

        val response = httpClient.request<HttpResponse> {
            method = HttpMethod.Post
            url(metadataUrl + innertubeApiKey)
            header("Accept", "application/json")
            header("Accept", "Language: en-GB,en;q=0.5")
            header("DNT", "1")
            header("Connection", "keep-alive")
            header("Content", "Type: application/json")
            body = objectMapper.writeValueAsString(requestBody)
        }

        if (!response.status.isSuccess()) {
            throw YoutubeClientException("Failed to get metadata. Video id: '$videoId', status: ${response.status}")
        }

        val metadataResponse = objectMapper.readValue<MetadataResponse>(response.content.toByteArray())
        val updateViewershipAction = metadataResponse.actions.firstOrNull { it.updateViewershipAction != null }
                ?: throw YoutubeClientException("updateViewershipAction was not found in actions")

        val viewCountText = updateViewershipAction.updateViewershipAction!!.viewCount.videoViewCountRenderer.viewCount?.simpleText
                ?: return 0

        try {
            return viewCountParser.parse(viewCountText)
        } catch (e: IllegalArgumentException) {
            throw YoutubeClientException(cause = e)
        }
    }

    fun CoroutineScope.pollLiveChatActions(initialParameters: LiveChatRequestParameters): ReceiveChannel<LiveChatResponse.Action> {
        val channel = Channel<LiveChatResponse.Action>(50)

        launch {
            var parameters = initialParameters

            while (true) {
                val response = getLiveChatResponse(parameters)

                response.continuationContents.liveChatContinuation.actions.forEach { action ->
                    channel.send(action)
                }

                val continuationDto = response.continuationContents.liveChatContinuation.continuations.first().anyContinuation()
                parameters = parameters.copy(nextContinuation = continuationDto.continuation)

                delay(continuationDto.timeoutMs.toLong())
            }
        }

        return channel
    }

}
