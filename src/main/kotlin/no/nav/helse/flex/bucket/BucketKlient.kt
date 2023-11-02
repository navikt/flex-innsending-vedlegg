package no.nav.helse.flex.no.nav.helse.flex.bucket

import com.google.cloud.storage.Blob
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.Storage
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component

@Component
class BucketKlient(
    @Value("\${BUCKET_NAME}")
    private val bucketName: String,
    private val storage: Storage
) {

    fun lagreBlob(blobNavn: String, contentType: MediaType, metadata: Map<String, String>, bytes: ByteArray): Blob {
        val contentTypeVerdi = contentType.toString()
        val blobInfo = BlobInfo.newBuilder(bucketName, blobNavn)
            .setContentType(contentTypeVerdi)
            .setMetadata(metadata + mapOf("content-type" to contentTypeVerdi))
            .build()

        return storage.create(blobInfo, bytes)
    }

    fun hentBlob(blobNavn: String): BlobContent? {
        return storage.get(bucketName, blobNavn)?.let {
            return BlobContent(metadata = it.metadata, blob = it)
        }
    }

    fun slettBlob(blobNavn: String): Boolean {
        return storage.delete(bucketName, blobNavn)
    }

    data class BlobContent(
        val metadata: MutableMap<String, String?>?,
        val blob: Blob
    )
}
