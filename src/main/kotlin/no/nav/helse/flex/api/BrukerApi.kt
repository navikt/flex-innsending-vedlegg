package no.nav.helse.flex.no.nav.helse.flex.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.helse.flex.AbstractApiError
import no.nav.helse.flex.LogLevel
import no.nav.helse.flex.kvittering.Kvittering
import no.nav.helse.flex.kvittering.Kvitteringer
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
@Tag(name = "kvitteringer", description = "Operasjoner for å laste opp reisetilskudd kvitteringer")
class BrukerApi(
    private val tokenValidationContextHolder: TokenValidationContextHolder,
    private val kvitteringer: Kvitteringer,

    @Value("\${SYKEPENGESOKNAD_FRONTEND_CLIENT_ID}")
    val sykepengesoknadFrontendClientId: String,

    @Value("\${SYKEPENGESOKNAD_BACKEND_CLIENT_ID}")
    val sykepengesoknadBackendClientId: String
) {

    @PostMapping("/api/v2/opplasting")
    @ProtectedWithClaims(issuer = "tokenx", combineWithOr = true, claimMap = ["acr=Level4", "acr=idporten-loa-high"])
    @ResponseBody
    fun lagreKvittering(@RequestParam("file") file: MultipartFile): ResponseEntity<VedleggRespons> {
        val id = UUID.randomUUID().toString()
        val fnr = validerTokenXClaims(sykepengesoknadFrontendClientId).hentFnr()

        kvitteringer.lagreKvittering(fnr, id, MediaType.parseMediaType(file.contentType!!), file.bytes)
        return ResponseEntity.status(HttpStatus.CREATED).body(VedleggRespons(id, "Lagret kvittering med id: $id."))
    }

    @GetMapping("/api/v2/kvittering/{blobNavn}")
    @Operation(description = "Hent kvittering")
    @ProtectedWithClaims(issuer = "tokenx", combineWithOr = true, claimMap = ["acr=Level4", "acr=idporten-loa-high"])
    fun hentKvittering(@PathVariable blobNavn: String): ResponseEntity<ByteArray> {
        if (blobNavn.matches("^[a-zA-Z0-9-]+$".toRegex())) {
            val fnr = validerTokenXClaims(sykepengesoknadFrontendClientId).hentFnr()

            val kvittering = kvitteringer.hentKvittering(blobNavn) ?: return ResponseEntity.notFound().build()

            if (!kvitteringEiesAvBruker(kvittering, fnr)) {
                throw UkjentClientException("Kvittering $blobNavn er forsøkt hentet av feil bruker.")
            }

            return ResponseEntity
                .ok()
                .contentType(MediaType.parseMediaType(kvittering.contentType))
                .body(kvittering.bytes)
        }
        throw IllegalArgumentException("blobNavn validerer ikke")
    }

    @DeleteMapping("/api/v2/kvittering/{blobNavn}")
    @ResponseBody
    @ProtectedWithClaims(issuer = "tokenx", combineWithOr = true, claimMap = ["acr=Level4", "acr=idporten-loa-high"])
    fun slettKvittering(@PathVariable blobNavn: String): ResponseEntity<Any> {
        if (blobNavn.matches("^[a-zA-Z0-9-]+$".toRegex())) {
            val fnr = validerTokenXClaims(sykepengesoknadBackendClientId).hentFnr()
            val kvittering = kvitteringer.hentKvittering(blobNavn) ?: return ResponseEntity.noContent().build()

            if (!kvitteringEiesAvBruker(kvittering, fnr)) {
                throw UkjentClientException("Kvittering $blobNavn er forsøkt slettet av feil bruker.")
            }
            kvitteringer.slettKvittering(blobNavn)
            return ResponseEntity.noContent().build()
        }
        throw IllegalArgumentException("blobNavn validerer ikke")
    }

    private fun kvitteringEiesAvBruker(kvittering: Kvittering, fnr: String): Boolean {
        return fnr == kvittering.fnr
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
