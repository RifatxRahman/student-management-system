package com.sms.studentmanagement.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sms.studentmanagement.exception.ApiException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class SafeImageTest {

    private byte[] image(String format, int width, int height, int type) throws IOException {
        BufferedImage img = new BufferedImage(width, height, type);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, format, out);
        return out.toByteArray();
    }

    @Test
    void largePngBecomesSmallJpeg() throws IOException {
        byte[] result = SafeImage.toSafeJpeg(image("png", 800, 600, BufferedImage.TYPE_INT_ARGB));

        // JPEG ফাইল সবসময় FF D8 দিয়ে শুরু হয়
        assertEquals((byte) 0xFF, result[0]);
        assertEquals((byte) 0xD8, result[1]);
        BufferedImage back = ImageIO.read(new ByteArrayInputStream(result));
        assertEquals(512, back.getWidth());
        assertEquals(384, back.getHeight());
    }

    @Test
    void smallImageIsNotEnlarged() throws IOException {
        byte[] result = SafeImage.toSafeJpeg(image("jpeg", 200, 100, BufferedImage.TYPE_INT_RGB));

        BufferedImage back = ImageIO.read(new ByteArrayInputStream(result));
        assertEquals(200, back.getWidth());
        assertEquals(100, back.getHeight());
    }

    @Test
    void textRenamedAsImageIsRejected() {
        byte[] fake = "<script>alert(1)</script>".getBytes(StandardCharsets.UTF_8);
        assertThrows(ApiException.class, () -> SafeImage.toSafeJpeg(fake));
    }

    @Test
    void gifIsRejected() throws IOException {
        byte[] gif = image("gif", 50, 50, BufferedImage.TYPE_INT_RGB);
        assertThrows(ApiException.class, () -> SafeImage.toSafeJpeg(gif));
    }

    @Test
    void tooWideImageIsRejected() throws IOException {
        byte[] wide = image("png", 5001, 1, BufferedImage.TYPE_INT_RGB);
        assertThrows(ApiException.class, () -> SafeImage.toSafeJpeg(wide));
    }

    @Test
    void emptyAndOversizedAreRejected() {
        assertThrows(ApiException.class, () -> SafeImage.toSafeJpeg(new byte[0]));
        assertThrows(ApiException.class, () -> SafeImage.toSafeJpeg(new byte[SafeImage.MAX_BYTES + 1]));
    }
}