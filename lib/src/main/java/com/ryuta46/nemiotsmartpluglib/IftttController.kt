package com.ryuta46.nemiotsmartpluglib

import com.ryuta46.nemkotlin.net.HttpRequest
import com.ryuta46.nemkotlin.net.HttpResponse
import com.ryuta46.nemkotlin.net.HttpURLConnectionClient
import java.net.URI


class IftttController(val appKey: String) {
    companion object {
        private val TAG = "NemIoTUnlockTest"
    }


    private fun createHookUrl(event: String) = "https://maker.ifttt.com/trigger/$event/with/key/$appKey"

    fun turnOn() {
        hookEvent("turn_on")
    }

    fun turnOff() {
        hookEvent("turn_off")
    }

    private fun hookEvent(event: String) {
        val uri = URI(createHookUrl(event))
        val request = HttpRequest(uri, "GET", "", emptyMap())
        load(request)

    }


    private fun load(request: HttpRequest): HttpResponse {
        return HttpURLConnectionClient().load(request)
    }


}