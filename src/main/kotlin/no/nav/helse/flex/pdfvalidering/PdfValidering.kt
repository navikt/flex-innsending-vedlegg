package no.nav.helse.flex.no.nav.helse.flex.pdfvalidering

import no.nav.helse.flex.AbstractApiError
import no.nav.helse.flex.LogLevel
import org.apache.pdfbox.Loader
import org.springframework.http.HttpStatus

fun ByteArray.sjekkGyldigPdf() {
    try {
        // Åpner PDF-dokumentet for å verifisere
        Loader.loadPDF(this).use { document ->
            // Sjekk om dokumentet har sider, det kan være en ytterligere validering
            if (document.numberOfPages == 0) {
                throw TomPdfException()
            }
        }
    } catch (e: Exception) {
        if (e is TomPdfException) throw e
        // Håndter alle andre exceptions som kan indikere at dette ikke er en gyldig PDF
        throw UgyldigPdfException(e)
    }
}

class UgyldigPdfException(grunn: Throwable) : AbstractApiError(
    message = "Ugyldig PDF",
    httpStatus = HttpStatus.BAD_REQUEST,
    reason = "UGYLDIG_PDF",
    loglevel = LogLevel.ERROR,
    grunn = grunn
)

class TomPdfException() : AbstractApiError(
    message = "Tom PDF",
    httpStatus = HttpStatus.BAD_REQUEST,
    reason = "TOM_PDF",
    loglevel = LogLevel.ERROR
)
