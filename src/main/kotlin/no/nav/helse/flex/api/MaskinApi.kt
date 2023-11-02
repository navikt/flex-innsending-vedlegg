package no.nav.helse.flex.no.nav.helse.flex.api

import com.fasterxml.jackson.module.kotlin.readValue
import io.swagger.v3.oas.annotations.Hidden
import no.nav.helse.flex.kvittering.Kvitteringer
import no.nav.helse.flex.objectMapper
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.ResponseBody

@Controller
@Hidden
class MaskinApi(
    @Value("\${AZURE_APP_PRE_AUTHORIZED_APPS}")
    private val azurePreAuthorizedApps: String,
    private val tokenValidationContextHolder: TokenValidationContextHolder,
    private val kvitteringer: Kvitteringer
) {

    private val allowedClients: List<PreAuthorizedClient> = objectMapper.readValue(azurePreAuthorizedApps)

    @GetMapping("/maskin/kvittering/{blobNavn}")
    @ProtectedWithClaims(issuer = "azureator")
    @ResponseBody
    fun hentKvittering(@PathVariable blobNavn: String): ResponseEntity<ByteArray> {
        validateClientId(validClients())
        val kvittering = kvitteringer.hentKvittering(blobNavn) ?: return ResponseEntity.notFound().build()
        return ResponseEntity
            .ok()
            .contentType(MediaType.parseMediaType(kvittering.contentType))
            .body(kvittering.bytes)
    }

    private fun validClients() = listOf(
        NamespaceOgApp(namespace = "flex", app = "sykepengesoknad-backend"),
        NamespaceOgApp(namespace = "flex", app = "sykepengesoknad-arkivering-oppgave")
    )

    fun validateClientId(apps: List<NamespaceOgApp>) {
        val clientIds = allowedClients
            .filter { apps.contains(it.tilNamespaceOgApp()) }
            .map { it.clientId }

        val azp = tokenValidationContextHolder.hentAzpClaim()
        if (!clientIds.contains(azp)) {
            throw UkjentClientException("Ukjent azp claim $azp.")
        }
    }

    private fun TokenValidationContextHolder.hentAzpClaim(): String {
        try {
            return this.tokenValidationContext.getJwtToken("azureator").jwtTokenClaims.getStringClaim("azp")!!
        } catch (e: Exception) {
            throw UkjentClientException("Fant ikke azp claim.", e)
        }
    }

    private fun PreAuthorizedClient.tilNamespaceOgApp(): NamespaceOgApp {
        val splitt = name.split(":")
        return NamespaceOgApp(namespace = splitt[1], app = splitt[2])
    }
}

data class NamespaceOgApp(val namespace: String, val app: String)

data class PreAuthorizedClient(val name: String, val clientId: String)
