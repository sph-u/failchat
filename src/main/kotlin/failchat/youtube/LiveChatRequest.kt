package failchat.youtube

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode

data class LiveChatRequest(
        val context: Context = Context(),
        val continuation: String
) {
    data class Context(
            val client: Client = Client(),
            val request: Request = Request(),
            val user: ObjectNode = JsonNodeFactory.instance.objectNode(),
            val clientScreenNonce: String = "MC4xNzQ1MzczNjgyNTc0MTI1"
    )

    data class Client(
            val hl: String = "en-GB",
            val gl: String = "RU",
            val visitorData: String = "CgtvaTIycV9CTXMwSSjUiOP5BQ%3D%3D",
            val userAgent: String = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:79.0) Gecko/20100101 Firefox/79.0,gzip(gfe)",
            val clientName: String = "WEB",
            val clientVersion: String = "2.20200814.00.00",
            val osName: String = "Windows",
            val osVersion: String = "10.0",
            val browserName: String = "Firefox",
            val browserVersion: String = "79.0",
            val screenWidthPoints: Int = 1920,
            val screenHeightPoints: Int = 362,
            val screenPixelDensity: Int = 1,
            val utcOffsetMinutes: Int = 180,
            val userInterfaceTheme: String = "USER_INTERFACE_THEME_LIGHT"
    )

    data class Request(
            val internalExperimentFlags: ArrayNode = JsonNodeFactory.instance.arrayNode(),
            val consistencyTokenJars: ArrayNode = JsonNodeFactory.instance.arrayNode()
    )

}
