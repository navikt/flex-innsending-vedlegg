package no.nav.helse.flex.api

import no.nav.helse.flex.FellesTestOppsett
import no.nav.helse.flex.no.nav.helse.flex.bucket.BucketKlient
import org.amshove.kluent.`should be equal to`
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.*

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
internal class MaskinApiTest : FellesTestOppsett() {

    private lateinit var kvitteringId: String

    @Autowired
    private lateinit var bucketKlient: BucketKlient

    @BeforeAll
    fun lagreKvittering() {
        kvitteringId = UUID.randomUUID().toString()
        val bilde = hentTestbilde("1200x800.jpeg")
        bucketKlient.lagreBlob(kvitteringId, bilde.contentType, mapOf("fnr" to "fnr-1"), bilde.bytes)
    }

    @Test
    @Order(1)
    fun `Hent kvittering`() {
        val azureToken = azureToken(subject = "sykepengesoknad-backend-client-id")

        val response = mockMvc.perform(
            get("/maskin/kvittering/$kvitteringId")
                .header("Authorization", "Bearer $azureToken")
        ).andExpect(status().isOk).andReturn().response

        response.contentType `should be equal to` MediaType.IMAGE_JPEG_VALUE
    }

    @Test
    @Order(2)
    fun `Hent kvittering med ukjent clientId`() {
        val azureToken = azureToken(subject = "ukjent-client-id")

        mockMvc.perform(
            get("/maskin/kvittering/$kvitteringId")
                .header("Authorization", "Bearer $azureToken")
        ).andExpect(status().isForbidden)
    }

    @Test
    @Order(3)
    fun `Hent kvittering som ikke finnes`() {
        val azureToken = azureToken(subject = "sykepengesoknad-backend-client-id")

        mockMvc.perform(
            get("/maskin/kvittering/ukjent-kvittering")
                .header("Authorization", "Bearer $azureToken")
        ).andExpect(status().isNotFound)
    }
}
