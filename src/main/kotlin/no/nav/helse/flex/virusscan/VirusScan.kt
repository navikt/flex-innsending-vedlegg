package no.nav.helse.flex.no.nav.helse.flex.virusscan

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.http.HttpEntity
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

@Component
class VirusScan(

    private val virusScanRestTemplate: RestTemplate
) {

    fun scanForVirus(bytes: ByteArray): Array<ScanResult>? {
        val result = virusScanRestTemplate
            .exchange(
                "http://clamav.nais-system.svc.cluster.local" + "/scan",
                org.springframework.http.HttpMethod.PUT,
                HttpEntity(
                    bytes
                ),
                Array<ScanResult>::class.java
            )
        return result.body
    }
}

class ScanResult @JsonCreator constructor(
    @field:JsonAlias(
        "Filename"
    )
    @param:JsonProperty("filename")
    val filename: String,
    @JsonProperty("result") result: Result
) {

    @JsonAlias("Result")
    private val result: Result

    init {
        this.result = result
    }

    fun getResult(): Result {
        return result
    }

    override fun toString(): String {
        return javaClass.getSimpleName() + " [filename=" + filename + ", result=" + result + "]"
    }
}

enum class Result {
    FOUND,
    OK
}
