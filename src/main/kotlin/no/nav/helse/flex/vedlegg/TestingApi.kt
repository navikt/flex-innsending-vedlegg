package no.nav.helse.flex.no.nav.helse.flex.vedlegg

import io.swagger.v3.oas.annotations.Operation
import no.nav.helse.flex.no.nav.helse.flex.virusscan.ScanResult
import no.nav.helse.flex.no.nav.helse.flex.virusscan.VirusScan
import no.nav.helse.flex.vedlegg.Vedlegg
import no.nav.helse.flex.vedlegg.VedleggService
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.util.*

@Controller
class TestingApi(
    private val vedleggService: VedleggService,
    private val virusScan: VirusScan

) {

    @PostMapping("/api/v1/virusscan")
    @Unprotected
    @ResponseBody
    fun virusscan(@RequestParam("file") file: MultipartFile): Array<ScanResult>? {
        val scanResult = virusScan.scanForVirus(file.bytes)
        return scanResult
    }

    @PostMapping("/api/v1/vedleggtest")
    @Unprotected
    @ResponseBody
    fun lagreVedleggtest(@RequestParam("file") file: MultipartFile): ResponseEntity<VedleggRespons> {
        val id = UUID.randomUUID().toString()
        val fnr = "12345678910"

        vedleggService.lagreVedlegg(fnr, id, MediaType.parseMediaType(file.contentType!!), file.bytes)
        return ResponseEntity.status(HttpStatus.CREATED).body(VedleggRespons(id, "Lagret vedlegg med id: $id."))
    }

    @GetMapping("/api/v1/vedleggtest/{blobNavn}")
    @Unprotected
    @Operation(description = "Hent vedlegg")
    fun hentVedleggtest(@PathVariable blobNavn: String): ResponseEntity<ByteArray> {
        if (blobNavn.matches("^[a-zA-Z0-9-]+$".toRegex())) {
            val fnr = "12345678910"

            val vedlegg = vedleggService.hentVedleggg(blobNavn) ?: return ResponseEntity.notFound().build()

            if (!vedleggEiesAvBruker(vedlegg, fnr)) {
                throw UkjentClientException("Vedlegg $blobNavn er forsøkt hentet av feil bruker.")
            }

            return ResponseEntity
                .ok()
                .contentType(MediaType.parseMediaType(vedlegg.contentType))
                .body(vedlegg.bytes)
        }
        throw IllegalArgumentException("blobNavn validerer ikke")
    }

    private fun vedleggEiesAvBruker(vedlegg: Vedlegg, fnr: String): Boolean {
        return fnr == vedlegg.fnr
    }
}
