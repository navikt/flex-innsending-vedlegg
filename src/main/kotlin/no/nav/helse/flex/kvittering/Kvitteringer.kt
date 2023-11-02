package no.nav.helse.flex.kvittering

import no.nav.helse.flex.logger
import no.nav.helse.flex.no.nav.helse.flex.bildeprosessering.Bilde
import no.nav.helse.flex.no.nav.helse.flex.bildeprosessering.Bildeprosessering
import no.nav.helse.flex.no.nav.helse.flex.bucket.BucketKlient
import no.nav.helse.flex.no.nav.helse.flex.bucket.BucketKlient.BlobContent
import org.springframework.http.MediaType
import org.springframework.stereotype.Service

@Service
class Kvitteringer(
    private val bucketKlient: BucketKlient,
    private val bildeprosessering: Bildeprosessering
) {

    private val log = logger()

    fun lagreKvittering(fnr: String, blobNavn: String, mediaType: MediaType, blobContent: ByteArray) {
        val prosessertBilde = bildeprosessering.prosesserBilde(Bilde(mediaType, blobContent))

        bucketKlient.lagreBlob(
            blobNavn = blobNavn,
            contentType = prosessertBilde!!.contentType,
            metadata = mapOf("fnr" to fnr),
            bytes = prosessertBilde.bytes
        )

        log.info("Lagret kvittering med blobNavn: $blobNavn og mediaType: $mediaType.")
    }

    fun hentKvittering(blobNavn: String): Kvittering? {
        return bucketKlient.hentBlob(blobNavn)?.let {
            return Kvittering(
                filnavn = "kvittering-$blobNavn.${it.filType()}",
                fnr = it.metadata!!["fnr"]!!,
                contentType = it.metadata["content-type"]!!,
                bytes = it.blob.getContent()
            )
        }
    }

    fun slettKvittering(blobNavn: String) {
        val slettetBlob = bucketKlient.slettBlob(blobNavn)
        if (!slettetBlob) {
            log.warn("Slettet ikke kvittering med blobNavn: $blobNavn da den ikke finnes.")
        }
        log.info("Slettet kvittering med blobNavn: $blobNavn.")
    }

    private fun BlobContent.filType(): String {
        return metadata!!["content-type"]!!.split("/")[1]
    }
}

class Kvittering(
    val filnavn: String,
    val fnr: String,
    val bytes: ByteArray,
    val contentType: String,
    val contentSize: Long = bytes.size.toLong()
)
