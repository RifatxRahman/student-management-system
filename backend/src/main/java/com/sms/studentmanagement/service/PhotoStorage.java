package com.sms.studentmanagement.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PhotoStorage {

    private static final Logger log = LoggerFactory.getLogger(PhotoStorage.class);
    private static final Pattern SAFE_NAME = Pattern.compile(
            "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.jpg$");

    private final Path baseDir;

    public PhotoStorage(@Value("${app.storage.photo-dir:./data/photos}") String directory) {
        this.baseDir = Paths.get(directory).toAbsolutePath().normalize();
        try {
            Files.createDirectories(baseDir);
        } catch (IOException e) {
            throw new UncheckedIOException("ছবির ফোল্ডার বানানো যায়নি: " + baseDir, e);
        }
    }

    /** নতুন এলোমেলো নামে সেভ করে নামটা ফেরায়। */
    public String save(byte[] jpegBytes) {
        String name = UUID.randomUUID() + ".jpg";
        try {
            Files.write(baseDir.resolve(name), jpegBytes, StandardOpenOption.CREATE_NEW);
        } catch (IOException e) {
            throw new UncheckedIOException("ছবি সেভ করা যায়নি", e);
        }
        return name;
    }

    /** নাম অচেনা/অসুরক্ষিত হলে বা ফাইল না থাকলে খালি (empty)। */
    public Optional<byte[]> read(String name) {
        Path path = safePath(name);
        if (path == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(Files.readAllBytes(path));
        } catch (NoSuchFileException e) {
            return Optional.empty();
        } catch (IOException e) {
            throw new UncheckedIOException("ছবি পড়া যায়নি", e);
        }
    }

    /** ব্যর্থ হলে শুধু লগে লেখে (মূল কাজ ভাঙে না)। */
    public void delete(String name) {
        Path path = safePath(name);
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("পুরনো ছবি মোছা যায়নি: {}", name);
        }
    }

    private Path safePath(String name) {
        if (name == null || !SAFE_NAME.matcher(name).matches()) {
            return null;
        }
        Path path = baseDir.resolve(name).normalize();
        return path.startsWith(baseDir) ? path : null;
    }
}