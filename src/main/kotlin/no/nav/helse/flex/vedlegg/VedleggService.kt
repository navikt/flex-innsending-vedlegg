package no.nav.helse.flex.vedlegg

import no.nav.helse.flex.logger
import no.nav.helse.flex.no.nav.helse.flex.bildeprosessering.Bilde
import no.nav.helse.flex.no.nav.helse.flex.bildeprosessering.Bildeprosessering
import no.nav.helse.flex.no.nav.helse.flex.bucket.BucketKlient
import no.nav.helse.flex.no.nav.helse.flex.bucket.BucketKlient.BlobContent
import org.springframework.http.MediaType
import org.springframework.stereotype.Service

@Service
class VedleggService(
    private val bucketKlient: BucketKlient,
    private val bildeprosessering: Bildeprosessering
) {

    private val log = logger()

    fun lagreVedlegg(fnr: String, blobNavn: String, mediaType: MediaType, blobContent: ByteArray) {
        val prosessertBilde = bildeprosessering.prosesserBilde(Bilde(mediaType, blobContent))

        bucketKlient.lagreBlob(
            blobNavn = blobNavn,
            contentType = prosessertBilde!!.contentType,
            metadata = mapOf("fnr" to fnr),
            bytes = prosessertBilde.bytes
        )

        log.info("Lagret vedlegg med blobNavn: $blobNavn og mediaType: $mediaType.")
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
