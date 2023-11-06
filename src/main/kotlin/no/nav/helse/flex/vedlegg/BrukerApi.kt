package no.nav.helse.flex.no.nav.helse.flex.vedlegg

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.helse.flex.AbstractApiError
import no.nav.helse.flex.LogLevel
import no.nav.helse.flex.vedlegg.Vedlegg
import no.nav.helse.flex.vedlegg.VedleggService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.security.token.support.core.jwt.JwtTokenClaims
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.util.*

@Controller
@Tag(name = "vedlegg", description = "Operasjoner for å laste opp vedlegg")
class BrukerApi(
    private val tokenValidationContextHolder: TokenValidationContextHolder,
    private val vedleggService: VedleggService,

    @Value("\${SYKEPENGESOKNAD_FRONTEND_CLIENT_ID}")
    val sykepengesoknadFrontendClientId: String,

    @Value("\${SYKEPENGESOKNAD_BACKEND_CLIENT_ID}")
    val sykepengesoknadBackendClientId: String
) {

    @PostMapping("/api/v1/vedlegg")
    @ProtectedWithClaims(issuer = "tokenx", combineWithOr = true, claimMap = ["acr=Level4", "acr=idporten-loa-high"])
    @ResponseBody
    fun lagreVedlegg(@RequestParam("file") file: MultipartFile): ResponseEntity<VedleggRespons> {
        val fnr = validerTokenXClaims(sykepengesoknadFrontendClientId).hentFnr()

        val id = vedleggService.lagreVedlegg(
            fnr = fnr,
            mediaType = MediaType.parseMediaType(file.contentType!!),
            blobContent = file.bytes
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(VedleggRespons(id, "Lagret vedlegg med id: $id."))
    }

    @GetMapping("/api/v1/vedlegg/{blobNavn}")
    @Operation(description = "Hent vedlegg")
    @ProtectedWithClaims(issuer = "tokenx", combineWithOr = true, claimMap = ["acr=Level4", "acr=idporten-loa-high"])
    fun hentVedlegg(@PathVariable blobNavn: String): ResponseEntity<ByteArray> {
        if (blobNavn.matches("^[a-zA-Z0-9-]+$".toRegex())) {
            val fnr = validerTokenXClaims(sykepengesoknadFrontendClientId).hentFnr()

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

    @DeleteMapping("/api/v1/vedlegg/{blobNavn}")
    @ResponseBody
    @ProtectedWithClaims(issuer = "tokenx", combineWithOr = true, claimMap = ["acr=Level4", "acr=idporten-loa-high"])
    fun slettVedlegg(@PathVariable blobNavn: String): ResponseEntity<Any> {
        if (blobNavn.matches("^[a-zA-Z0-9-]+$".toRegex())) {
            val fnr = validerTokenXClaims(sykepengesoknadBackendClientId).hentFnr()
            val vedlegg = vedleggService.hentVedleggg(blobNavn) ?: return ResponseEntity.noContent().build()

            if (!vedleggEiesAvBruker(vedlegg, fnr)) {
                throw UkjentClientException("Vedlegg $blobNavn er forsøkt slettet av feil bruker.")
            }
            vedleggService.slettVedlegg(blobNavn)
            return ResponseEntity.noContent().build()
        }
        throw IllegalArgumentException("blobNavn validerer ikke")
    }

    private fun vedleggEiesAvBruker(vedlegg: Vedlegg, fnr: String): Boolean {
        return fnr == vedlegg.fnr
    }

    private fun JwtTokenClaims.hentFnr(): String {
        return this.getStringClaim("pid")
    }

    private fun validerTokenXClaims(vararg tillattClient: String): JwtTokenClaims {
        val context = tokenValidationContextHolder.tokenValidationContext
        val claims = context.getClaims("tokenx")
        val clientId = claims.getStringClaim("client_id")

        if (!tillattClient.toList().contains(clientId)) {
            throw UkjentClientException("Uventet client id $clientId")
        }
        return claims
    }
}

class UkjentClientException(message: String, grunn: Throwable? = null) : AbstractApiError(
    message = message,
    httpStatus = HttpStatus.FORBIDDEN,
    reason = "UKJENT_CLIENT",
    loglevel = LogLevel.WARN,
    grunn = grunn
)

data class VedleggRespons(val id: String? = null, val melding: String)
