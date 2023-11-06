package no.nav.helse.flex.no.nav.helse.flex.pdfvalidering

import org.apache.pdfbox.Loader
import org.springframework.web.multipart.MultipartFile

fun erGyldigPdf(file: MultipartFile) {
    try {
        // Åpner PDF-dokumentet for å verifisere
        Loader.loadPDF(file.bytes).use { document ->
            // Sjekk om dokumentet har sider, det kan være en ytterligere validering
            if (document.numberOfPages == 0) {
                throw IllegalArgumentException("PDF-dokumentet har ingen sider")
            }
        }
    } catch (e: Exception) {
        // Håndter alle andre exceptions som kan indikere at dette ikke er en gyldig PDF
        throw IllegalArgumentException("PDF-dokumentet er ikke gyldig", e)
    }
}
