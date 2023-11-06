package no.nav.helse.flex.no.nav.helse.flex.bildeprosessering

import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import java.awt.Color
import java.awt.Image
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

private const val RESIZE_TIL_BREDDE = 600

@Component
class Bildeprosessering {

    private val gyldigeBildetyper = listOf(MediaType.IMAGE_PNG, MediaType.IMAGE_JPEG)

    fun prosesserBilde(bilde: Bilde): Bilde {
        if (!gyldigeBildetyper.contains(bilde.contentType)) {
            throw IllegalArgumentException(
                "Kan ikke prosessere bilde av typen ${bilde.contentType}. " +
                    "Kun ${MediaType.IMAGE_JPEG_VALUE} og ${MediaType.IMAGE_PNG_VALUE} er støttet."
            )
        }

        return prosesser(bilde)
    }

    private fun prosesser(bilde: Bilde): Bilde {
        val jpegBytes: ByteArray = if (bilde.contentType == MediaType.IMAGE_JPEG) {
            bilde.bytes
        } else {
            konverterTilJpeg(bilde.bytes)
        }

        return Bilde(
            MediaType.IMAGE_JPEG,
            skalerBilde(jpegBytes)
        )
    }

    private fun skalerBilde(bytes: ByteArray): ByteArray {
        val skalertBilde: Image = bytes.tilBufferedImage().getScaledInstance(
            RESIZE_TIL_BREDDE,
            // Negativt tall for høyde gjør at aspect ratio kalkuleres fra bredden.
            -1,
            Image.SCALE_SMOOTH
        )

        val resultatBilde = BufferedImage(
            skalertBilde.getWidth(null),
            skalertBilde.getHeight(null),
            BufferedImage.TYPE_INT_RGB
        )
        resultatBilde.graphics.drawImage(skalertBilde, 0, 0, null)

        return resultatBilde.tilByteArray()
    }

    private fun konverterTilJpeg(bytes: ByteArray): ByteArray {
        val originalBilde = ImageIO.read(ByteArrayInputStream(bytes))

        val resultatBilde = BufferedImage(
            originalBilde.width,
            originalBilde.height,
            BufferedImage.TYPE_INT_RGB
        )
        // Color.WHITE erstatter transparens i PNG-bilder siden alpha channel ikke støttes av JPEG.
        resultatBilde.createGraphics().drawImage(originalBilde, 0, 0, Color.WHITE, null)

        return resultatBilde.tilByteArray()
    }

    private fun BufferedImage.tilByteArray(): ByteArray {
        val outputStream = ByteArrayOutputStream()
        ImageIO.write(this, "jpeg", outputStream)
        return outputStream.toByteArray()
    }

    private fun ByteArray.tilBufferedImage(): BufferedImage = ImageIO.read(ByteArrayInputStream(this))
}

class Bilde(
    val contentType: MediaType,
    val bytes: ByteArray
)
