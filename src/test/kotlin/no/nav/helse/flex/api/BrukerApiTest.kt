package no.nav.helse.flex.api

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.FellesTestOppsett
import no.nav.helse.flex.no.nav.helse.flex.api.VedleggRespons
import no.nav.helse.flex.objectMapper
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.shouldNotBeNullOrEmpty
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
internal class BrukerApiTest : FellesTestOppsett() {

    private lateinit var kvitteringId: String

    @Test
    @Order(1)
    fun `Last opp kvittering som bruker`() {
        val bilde = hentTestbilde("1200x800.jpeg")
        val multipartFile = MockMultipartFile("file", null, bilde.contentType.toString(), bilde.bytes)

        val response = mockMvc.perform(
            multipart("/api/v2/opplasting")
                .file(multipartFile)
                .header("Authorization", "Bearer ${tokenxToken("fnr-1")}")
        ).andExpect(status().isCreated).andReturn().response

        val vedleggRespons: VedleggRespons = objectMapper.readValue(response.contentAsString)
        vedleggRespons.id.shouldNotBeNullOrEmpty()

        kvitteringId = vedleggRespons.id!!
    }

    @Test
    @Order(1)
    fun `Last opp ugyldig bilde`() {
        val bilde = hentTestbilde("1200x800.heic")
        val multipartFile = MockMultipartFile("file", null, bilde.contentType.toString(), bilde.bytes)

        mockMvc.perform(
            multipart("/api/v2/opplasting")
                .file(multipartFile)
                .header("Authorization", "Bearer ${tokenxToken("fnr-1")}")
        ).andExpect(status().isInternalServerError)
    }

    @Test
    @Order(2)
    fun `Hent kvittering som bruker`() {
        val response = mockMvc.perform(
            get("/api/v2/kvittering/$kvitteringId")
                .header("Authorization", "Bearer ${tokenxToken("fnr-1")}")
        ).andExpect(status().isOk).andReturn().response

        response.contentType `should be equal to` MediaType.IMAGE_JPEG_VALUE
    }

    @Test
    @Order(3)
    fun `Hent kvittering med feil bruker`() {
        mockMvc.perform(
            get("/api/v2/kvittering/$kvitteringId")
                .header("Authorization", "Bearer ${tokenxToken("fnr-2")}")
        ).andExpect(status().isForbidden)
    }

    @Test
    @Order(4)
    fun `Slett kvittering med feil bruker`() {
        mockMvc.perform(
            delete("/api/v2/kvittering/$kvitteringId")
                .header(
                    "Authorization",
                    "Bearer ${tokenxToken(fnr = "fnr-2", clientId = "sykepengesoknad-backend-client-id")}"
                )
        ).andExpect(status().isForbidden)
    }

    @Test
    @Order(5)
    fun `Slett kvittering`() {
        mockMvc.perform(
            delete("/api/v2/kvittering/$kvitteringId")
                .header(
                    "Authorization",
                    "Bearer ${tokenxToken(fnr = "fnr-1", clientId = "sykepengesoknad-backend-client-id")}"
                )
        ).andExpect(status().isNoContent)
    }

    @Test
    @Order(6)
    fun `Hent slettet kvittering som bruker`() {
        mockMvc.perform(
            get("/api/v2/kvittering/$kvitteringId")
                .header("Authorization", "Bearer ${tokenxToken("fnr-1")}")
        ).andExpect(status().isNotFound)
    }

    @Test
    @Order(5)
    fun `Slett allerede slettet kvittering`() {
        mockMvc.perform(
            delete("/api/v2/kvittering/$kvitteringId")
                .header(
                    "Authorization",
                    "Bearer ${tokenxToken(fnr = "fnr-1", clientId = "sykepengesoknad-backend-client-id")}"
                )
        ).andExpect(status().isNoContent)
    }
}
