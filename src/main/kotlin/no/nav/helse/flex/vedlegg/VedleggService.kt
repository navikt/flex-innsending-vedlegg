package no.nav.helse.flex.vedlegg

import no.nav.helse.flex.AbstractApiError
import no.nav.helse.flex.LogLevel
import no.nav.helse.flex.logger
import no.nav.helse.flex.no.nav.helse.flex.bildeprosessering.Bilde
import no.nav.helse.flex.no.nav.helse.flex.bildeprosessering.Bildeprosessering
import no.nav.helse.flex.no.nav.helse.flex.bucket.BucketKlient
import no.nav.helse.flex.no.nav.helse.flex.bucket.BucketKlient.BlobContent
import no.nav.helse.flex.no.nav.helse.flex.pdfvalidering.sjekkGyldigPdf
import no.nav.helse.flex.no.nav.helse.flex.virusscan.Result
import no.nav.helse.flex.no.nav.helse.flex.virusscan.VirusScan
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import java.util.*

@Service
class VedleggService(
    private val bucketKlient: BucketKlient,
    private val bildeprosessering: Bildeprosessering,
    private val virusScan: VirusScan
) {

    private val log = logger()

    fun lagreVedlegg(fnr: String, id: String = UUID.randomUUID().toString(), mediaType: MediaType, blobContent: ByteArray): String {
        virusScan.scanForVirus(blobContent).let {
            if (it.any { scanResult -> scanResult.getResult() == Result.FOUND }) {
                throw VirusFunnetException()
            }
        }

        data class BlobContent(val contentType: MediaType, val bytes: ByteArray)

        val filTilOpplasting = when (mediaType) {
            MediaType.IMAGE_JPEG, MediaType.IMAGE_PNG -> {
                val prosesserBilde = bildeprosessering.prosesserBilde(
                    Bilde(
                        mediaType,
                        blobContent
                    )
                )
                BlobContent(prosesserBilde.contentType, prosesserBilde.bytes)
            }

            MediaType.APPLICATION_PDF -> {
                blobContent.sjekkGyldigPdf()
                BlobContent(mediaType, blobContent)
            }

            else -> throw IkkeStøttetMediatypeException(mediaType)
        }

        bucketKlient.lagreBlob(
            blobNavn = id,
            contentType = filTilOpplasting.contentType,
            metadata = mapOf("fnr" to fnr),
            bytes = filTilOpplasting.bytes
        )

        log.info("Lagret vedlegg med blobNavn: $id og mediaType: ${filTilOpplasting.contentType}.")
        return id
    }

    fun hentVedleggg(blobNavn: String): Vedlegg? {
        return bucketKlient.hentBlob(blobNavn)?.let {
            return Vedlegg(
                filnavn = "vedlegg-$blobNavn.${it.filType()}",
                fnr = it.metadata!!["fnr"]!!,
                contentType = it.metadata["content-type"]!!,
                bytes = it.blob.getContent()
            )
        }
    }

    fun slettVedlegg(blobNavn: String) {
        val slettetBlob = bucketKlient.slettBlob(blobNavn)
        if (!slettetBlob) {
            log.warn("Slettet ikke vedlegg med blobNavn: $blobNavn da den ikke finnes.")
        }
        log.info("Slettet vedlegg med blobNavn: $blobNavn.")
    }

    private fun BlobContent.filType(): String {
        return metadata!!["content-type"]!!.split("/")[1]
    }
}

class Vedlegg(
    val filnavn: String,
    val fnr: String,
    val bytes: ByteArray,
    val contentType: String,
    val contentSize: Long = bytes.size.toLong()
)

class VirusFunnetException : AbstractApiError(
    "Virus funnet i vedlegg",
    HttpStatus.BAD_REQUEST,
    "VIRUS_FUNNET",
    LogLevel.ERROR
)

class IkkeStøttetMediatypeException(mediaType: MediaType) : AbstractApiError(
    "ikke støttet mediatype: $mediaType",
    HttpStatus.BAD_REQUEST,
    "FEIL_MEDIATYPE",
    LogLevel.ERROR
)
