package no.nav.helse.flex.vedlegg

import no.nav.helse.flex.FellesTestOppsett
import org.amshove.kluent.`should be`
import org.amshove.kluent.`should be equal to`
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
internal class VedleggTest : FellesTestOppsett() {

    @Autowired
    private lateinit var vedleggService: VedleggService

    @Test
    @Order(1)
    fun `Lagrer vedlegg`() {
        val bilde = hentTestbilde("1200x800.jpeg")

        vedleggService.lagreVedlegg("fnr-1", "blob-1", bilde.contentType, bilde.bytes)
    }

    @Test
    @Order(2)
    fun `Henter lagret vedlegg`() {
        vedleggService.hentVedleggg("blob-1")?.let {
            it.contentType `should be equal to` MediaType.IMAGE_JPEG_VALUE
            it.filnavn `should be equal to` "vedlegg-blob-1.jpeg"
        }
    }

    @Test
    @Order(3)
    fun `Sletter vedlegg som finnes`() {
        vedleggService.slettVedlegg("blob-1")

        vedleggService.hentVedleggg("blob-1") `should be` null
    }

    @Test
    @Order(4)
    fun `Sletter vedlegg som allerede er slettet`() {
        vedleggService.slettVedlegg("blob-1")
    }

    @Test
    @Order(5)
    fun `Henter vedlegg som ikke finnes`() {
        vedleggService.hentVedleggg("blob-1") `should be` null
    }
}
