package no.nav.helse.flex.kvittering

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
internal class KvitteringerTest : FellesTestOppsett() {

    @Autowired
    private lateinit var kvitteringer: Kvitteringer

    @Test
    @Order(1)
    fun `Lagrer kvittering`() {
        val bilde = hentTestbilde("1200x800.jpeg")

        kvitteringer.lagreKvittering("fnr-1", "blob-1", bilde.contentType, bilde.bytes)
    }

    @Test
    @Order(2)
    fun `Henter lagret kvittering`() {
        kvitteringer.hentKvittering("blob-1")?.let {
            it.contentType `should be equal to` MediaType.IMAGE_JPEG_VALUE
            it.filnavn `should be equal to` "kvittering-blob-1.jpeg"
        }
    }

    @Test
    @Order(3)
    fun `Sletter kvittering som finnes`() {
        kvitteringer.slettKvittering("blob-1")

        kvitteringer.hentKvittering("blob-1") `should be` null
    }

    @Test
    @Order(4)
    fun `Sletter kvittering som allerede er slettet`() {
        kvitteringer.slettKvittering("blob-1")
    }

    @Test
    @Order(5)
    fun `Henter kvittering som ikke finnes`() {
        kvitteringer.hentKvittering("blob-1") `should be` null
    }
}
