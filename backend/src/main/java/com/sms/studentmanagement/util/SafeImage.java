package com.sms.studentmanagement.util;

import com.sms.studentmanagement.exception.ApiException;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import org.springframework.http.HttpStatus;

public final class SafeImage {

    public static final int MAX_BYTES = 2 * 1024 * 1024;
    private static final int MAX_SIDE = 5000;
    private static final long MAX_PIXELS = 12_000_000L;
    private static final int OUTPUT_SIDE = 512;
    private static final Set<String> ALLOWED_FORMATS = Set.of("jpeg", "png");

    private SafeImage() {
    }

    /** আসল ছবি হলে ৫১২px-এর মধ্যে ছোট করা নতুন JPEG-এর বাইট ফেরায়। না হলে 400। */
    public static byte[] toSafeJpeg(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw bad("ছবি খালি");
        }
        if (bytes.length > MAX_BYTES) {
            throw bad("ছবি সর্বোচ্চ ২ MB হতে পারে");
        }
        BufferedImage source = decode(bytes);
        return encodeJpeg(resizeToRgb(source));
    }

    private static BufferedImage decode(byte[] bytes) {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (input == null) {
                throw notAnImage();
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw notAnImage();
            }
            ImageReader reader = readers.next();
            try {
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                if (!ALLOWED_FORMATS.contains(format)) {
                    throw notAnImage();
                }
                reader.setInput(input, true, true);
                // পুরো ছবি মেমোরিতে খোলার আগে মাপ দেখে নিই (খুব বড় মাপের "বোমা" ঠেকাতে)
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width < 1 || height < 1 || width > MAX_SIDE || height > MAX_SIDE
                        || (long) width * height > MAX_PIXELS) {
                    throw bad("ছবির মাপ খুব বড় (সর্বোচ্চ " + MAX_SIDE + " x " + MAX_SIDE + " পিক্সেল)");
                }
                return reader.read(0);
            } finally {
                reader.dispose();
            }
        } catch (ApiException e) {
            throw e;
        } catch (IOException | RuntimeException e) {
            throw notAnImage();
        }
    }

    private static BufferedImage resizeToRgb(BufferedImage source) {
        int width = source.getWidth();
        int height = source.getHeight();
        double scale = Math.min(1.0, (double) OUTPUT_SIDE / Math.max(width, height));
        int newWidth = Math.max(1, (int) Math.round(width * scale));
        int newHeight = Math.max(1, (int) Math.round(height * scale));

        BufferedImage output = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = output.createGraphics();
        try {
            g.setColor(Color.WHITE); // স্বচ্ছ (PNG) অংশ সাদা হবে
            g.fillRect(0, 0, newWidth, newHeight);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(source, 0, 0, newWidth, newHeight, null);
        } finally {
            g.dispose();
        }
        return output;
    }

    private static byte[] encodeJpeg(BufferedImage image) {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        try {
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(0.85f);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (ImageOutputStream ios = ImageIO.createImageOutputStream(out)) {
                writer.setOutput(ios);
                // metadata = null: কোনো EXIF বা লুকানো তথ্য নতুন ফাইলে যায় না
                writer.write(null, new IIOImage(image, null, null), param);
            }
            return out.toByteArray();
        } catch (IOException e) {
            throw bad("ছবি প্রক্রিয়া করা যায়নি");
        } finally {
            writer.dispose();
        }
    }

    private static ApiException notAnImage() {
        return bad("এটা সঠিক JPEG বা PNG ছবি নয়");
    }

    private static ApiException bad(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message);
    }
}