package no.nav.helse.flex.mockdispatcher

import no.nav.helse.flex.no.nav.helse.flex.virusscan.Result
import no.nav.helse.flex.no.nav.helse.flex.virusscan.ScanResult
import no.nav.helse.flex.serialisertTilString
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.QueueDispatcher
import okhttp3.mockwebserver.RecordedRequest

object ClamAvMockDispatcher : QueueDispatcher() {
    override fun dispatch(request: RecordedRequest): MockResponse {
        if (request.requestUrl?.encodedPath?.endsWith("/scan") != true) {
            return MockResponse().setResponseCode(404)
                .setBody("Har ikke implemetert mock api for ${request.requestUrl}")
        }

        if (responseQueue.peek() != null) {
            return responseQueue.take()
        }

        return MockResponse().setBody(
            listOf(ScanResult("test", Result.OK)).serialisertTilString()
        ).addHeader("Content-Type", "application/json")
    }
}
