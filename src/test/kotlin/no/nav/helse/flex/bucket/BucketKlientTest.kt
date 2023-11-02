package no.nav.helse.flex.bucket

import com.google.cloud.storage.contrib.nio.testing.LocalStorageHelper
import no.nav.helse.flex.no.nav.helse.flex.bucket.BucketKlient
import org.amshove.kluent.`should be`
import org.amshove.kluent.`should be equal to`
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestMethodOrder
import org.springframework.http.MediaType

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BucketKlientTest {

    private val storage = LocalStorageHelper.getOptions().service
    private val bucketName = "local-bucker"
    private val bucketKlient = BucketKlient(bucketName, storage)

    @Test
    @Order(1)
    fun `List filer i tom bucket`() {
        val innhold = storage.list(bucketName).values.toList()

        innhold.isEmpty() `should be` true
    }

    @Test
    @Order(2)
    fun `Lagrer fil i bucket`() {
        val bytes = "blob-content-1".toByteArray()
        bucketKlient.lagreBlob("blob-1", MediaType.TEXT_PLAIN, mapOf("fnr" to "fnr-1"), bytes)

        listInnhold().isEmpty() `should be` false
    }

    @Test
    @Order(3)
    fun `Henter fil fra bucket`() {
        val blobContent = bucketKlient.hentBlob("blob-1")

        String(blobContent!!.blob.getContent()) `should be equal to` "blob-content-1"
        blobContent.metadata!!["fnr"] `should be equal to` "fnr-1"
        blobContent.metadata!!["content-type"] `should be equal to` MediaType.TEXT_PLAIN.toString()
    }

    @Test
    @Order(4)
    fun `Sletter fil fra bucket`() {
        bucketKlient.slettBlob("blob-1")

        listInnhold().isEmpty() `should be` true
    }

    @Test
    @Order(5)
    fun `Henter blob som ikke finnes`() {
        bucketKlient.hentBlob("blob-1") `should be` null
    }

    private fun listInnhold() = storage.list(bucketName).values.toList()
}
