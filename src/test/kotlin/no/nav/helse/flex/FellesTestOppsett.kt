package no.nav.helse.flex

import no.nav.helse.flex.no.nav.helse.flex.bildeprosessering.Bilde
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.apache.tika.Tika
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

private const val TESTBILDER = "src/test/resources/bilder/"

const val PROSESSERT_BILDE_BYTE_SIZE = 4028

@SpringBootTest
@AutoConfigureMockMvc
@EnableMockOAuth2Server
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class FellesTestOppsett() {

    val tika = Tika()

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var mockOAuth2Server: MockOAuth2Server

    fun hentTestbilde(filnavn: String): Bilde {
        val bytes = Files.readAllBytes(Paths.get("$TESTBILDER/$filnavn"))

        return Bilde(
            MediaType.parseMediaType(tika.detect(bytes)),
            bytes
        )
    }

    fun hentTestbilde(filnavn: String, mediaType: MediaType): Bilde {
        val bildefil = Paths.get("$TESTBILDER/$filnavn")

        return Bilde(
            mediaType,
            Files.readAllBytes(bildefil)
        )
    }

    fun finnMediaType(prosessertBilde: Bilde?): String = tika.detect(prosessertBilde!!.bytes)

    fun tokenxToken(
        fnr: String,
        audience: String = "flex-bucket-uploader-client-id",
        issuerId: String = "tokenx",
        clientId: String = "sykepengesoknad-frontend-client-id",
        claims: Map<String, Any> = mapOf(
            "acr" to "idporten-loa-high",
            "idp" to "idporten",
            "client_id" to clientId,
            "pid" to fnr
        )
    ): String {
        return mockOAuth2Server.issueToken(
            issuerId,
            clientId,
            DefaultOAuth2TokenCallback(
                issuerId = issuerId,
                subject = UUID.randomUUID().toString(),
                audience = listOf(audience),
                claims = claims,
                expiry = 3600
            )
        ).serialize()
    }

    fun azureToken(subject: String) = mockOAuth2Server.lagToken(
        subject = subject,
        issuerId = "azureator",
        clientId = subject,
        audience = "flex-bucket-uploader-client-id",
        claims = HashMap<String, String>()
    )

    private fun MockOAuth2Server.lagToken(
        subject: String,
        issuerId: String,
        clientId: String,
        audience: String,
        claims: Map<String, Any> = mapOf("acr" to "idporten-loa-high")
    ): String {
        return this.issueToken(
            issuerId,
            clientId,
            DefaultOAuth2TokenCallback(
                issuerId = issuerId,
                subject = subject,
                audience = listOf(audience),
                claims = claims,
                expiry = 3600
            )
        ).serialize()
    }
}
