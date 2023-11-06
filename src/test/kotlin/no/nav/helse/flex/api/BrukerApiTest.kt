package no.nav.helse.flex.api

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.ApiError
import no.nav.helse.flex.FellesTestOppsett
import no.nav.helse.flex.no.nav.helse.flex.vedlegg.VedleggRespons
import no.nav.helse.flex.no.nav.helse.flex.virusscan.Result
import no.nav.helse.flex.no.nav.helse.flex.virusscan.ScanResult
import no.nav.helse.flex.objectMapper
import no.nav.helse.flex.serialisertTilString
import okhttp3.mockwebserver.MockResponse
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.shouldNotBeNullOrEmpty
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.io.ByteArrayOutputStream

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
internal class BrukerApiTest : FellesTestOppsett() {

    private lateinit var vedleggId: String

    @Test
    @Order(1)
    fun `Last opp vedlegg som bruker`() {
        val bilde = hentTestbilde("1200x800.jpeg")
        val multipartFile = MockMultipartFile("file", null, bilde.contentType.toString(), bilde.bytes)

        val response = mockMvc.perform(
            multipart("/api/v1/vedlegg")
                .file(multipartFile)
                .header("Authorization", "Bearer ${tokenxToken("fnr-1")}")
        ).andExpect(status().isCreated).andReturn().response

        val vedleggRespons: VedleggRespons = objectMapper.readValue(response.contentAsString)
        vedleggRespons.id.shouldNotBeNullOrEmpty()

        vedleggId = vedleggRespons.id!!
    }

    @Test
    @Order(1)
    fun `Last opp ugyldig bilde`() {
        val bilde = hentTestbilde("1200x800.heic")
        val multipartFile = MockMultipartFile("file", null, bilde.contentType.toString(), bilde.bytes)

        mockMvc.perform(
            multipart("/api/v1/vedlegg")
                .file(multipartFile)
                .header("Authorization", "Bearer ${tokenxToken("fnr-1")}")
        ).andExpect(status().isBadRequest)
    }

    @Test
    @Order(2)
    fun `Hent vedlegg som bruker`() {
        val response = mockMvc.perform(
            get("/api/v1/vedlegg/$vedleggId")
                .header("Authorization", "Bearer ${tokenxToken("fnr-1")}")
        ).andExpect(status().isOk).andReturn().response

        response.contentType `should be equal to` MediaType.IMAGE_JPEG_VALUE
    }

    @Test
    @Order(3)
    fun `Hent vedlegg med feil bruker`() {
        mockMvc.perform(
            get("/api/v1/vedlegg/$vedleggId")
                .header("Authorization", "Bearer ${tokenxToken("fnr-2")}")
        ).andExpect(status().isForbidden)
    }

    @Test
    @Order(4)
    fun `Slett vedlegg med feil bruker`() {
        mockMvc.perform(
            delete("/api/v1/vedlegg/$vedleggId")
                .header(
                    "Authorization",
                    "Bearer ${tokenxToken(fnr = "fnr-2", clientId = "sykepengesoknad-backend-client-id")}"
                )
        ).andExpect(status().isForbidden)
    }

    @Test
    @Order(5)
    fun `Slett vedlegg`() {
        mockMvc.perform(
            delete("/api/v1/vedlegg/$vedleggId")
                .header(
                    "Authorization",
                    "Bearer ${tokenxToken(fnr = "fnr-1", clientId = "sykepengesoknad-backend-client-id")}"
                )
        ).andExpect(status().isNoContent)
    }

    @Test
    @Order(6)
    fun `Hent slettet vedlegg som bruker`() {
        mockMvc.perform(
            get("/api/v1/vedlegg/$vedleggId")
                .header("Authorization", "Bearer ${tokenxToken("fnr-1")}")
        ).andExpect(status().isNotFound)
    }

    @Test
    @Order(5)
    fun `Slett allerede slettet vedlegg`() {
        mockMvc.perform(
            delete("/api/v1/vedlegg/$vedleggId")
                .header(
                    "Authorization",
                    "Bearer ${tokenxToken(fnr = "fnr-1", clientId = "sykepengesoknad-backend-client-id")}"
                )
        ).andExpect(status().isNoContent)
    }

    @Test
    @Order(6)
    fun `Clam AV sier virus`() {
        val bilde = hentTestbilde("1200x800.jpeg")
        val multipartFile = MockMultipartFile("file", null, bilde.contentType.toString(), bilde.bytes)

        clamAvMockDispatcher.enqueue(
            MockResponse().setBody(
                listOf(ScanResult("test", Result.FOUND)).serialisertTilString()
            ).addHeader("Content-Type", "application/json")
        )
        mockMvc.perform(
            multipart("/api/v1/vedlegg")
                .file(multipartFile)
                .header("Authorization", "Bearer ${tokenxToken("fnr-1")}")
        ).andExpect(status().isBadRequest).tilReason() `should be equal to` "VIRUS_FUNNET"
    }

    @Test
    @Order(7)
    fun `Ustøttet contenttype`() {
        val multipartFile = MockMultipartFile("file", null, MediaType.APPLICATION_ATOM_XML.toString(), ByteArray(0))

        mockMvc.perform(
            multipart("/api/v1/vedlegg")
                .file(multipartFile)
                .header("Authorization", "Bearer ${tokenxToken("fnr-1")}")
        ).andExpect(status().isBadRequest).tilReason() `should be equal to` "USTØTTET_MEDIATYPE"
    }

    @Test
    @Order(8)
    fun `Ugyldig PDF`() {
        val multipartFile = MockMultipartFile("file", null, MediaType.APPLICATION_PDF.toString(), ByteArray(0))

        mockMvc.perform(
            multipart("/api/v1/vedlegg")
                .file(multipartFile)
                .header("Authorization", "Bearer ${tokenxToken("fnr-1")}")
        ).andExpect(status().isBadRequest).tilReason() `should be equal to` "UGYLDIG_PDF"
    }

    @Test
    @Order(9)
    fun `Tom PDF`() {
        fun createEmptyPdfByteArray(): ByteArray {
            PDDocument().use { document ->
                ByteArrayOutputStream().use { outputStream ->
                    document.save(outputStream)
                    return outputStream.toByteArray()
                }
            }
        }

        val multipartFile =
            MockMultipartFile("file", null, MediaType.APPLICATION_PDF.toString(), createEmptyPdfByteArray())

        mockMvc.perform(
            multipart("/api/v1/vedlegg")
                .file(multipartFile)
                .header("Authorization", "Bearer ${tokenxToken("fnr-1")}")
        ).andExpect(status().isBadRequest).tilReason() `should be equal to` "TOM_PDF"
    }

    @Test
    @Order(10)
    fun `Laster opp PDF med en side`() {
        fun createEmptyPdfByteArray(): ByteArray {
            PDDocument().use { document ->
                document.addPage(PDPage())
                ByteArrayOutputStream().use { outputStream ->
                    document.save(outputStream)
                    return outputStream.toByteArray()
                }
            }
        }

        val multipartFile =
            MockMultipartFile("file", null, MediaType.APPLICATION_PDF.toString(), createEmptyPdfByteArray())

        mockMvc.perform(
            multipart("/api/v1/vedlegg")
                .file(multipartFile)
                .header("Authorization", "Bearer ${tokenxToken("fnr-1")}")
        ).andExpect(status().isCreated)
    }

    fun ResultActions.tilReason(): String {
        val response: ApiError = objectMapper.readValue(this.andReturn().response.contentAsString)
        return response.reason
    }
}
