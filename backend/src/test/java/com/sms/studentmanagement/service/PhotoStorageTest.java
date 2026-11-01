package com.sms.studentmanagement.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PhotoStorageTest {

    @TempDir
    Path tempDir;

    @Test
    void savedFileCanBeReadBack() {
        PhotoStorage storage = new PhotoStorage(tempDir.toString());
        byte[] data = {1, 2, 3, 4};

        String name = storage.save(data);

        assertTrue(name.matches("^[0-9a-f-]{36}\\.jpg$"));
        assertTrue(Files.exists(tempDir.resolve(name)));
        assertArrayEquals(data, storage.read(name).orElseThrow());
    }

    @Test
    void unsafeNamesAreNeverRead() throws IOException {
        Path secret = tempDir.resolveSibling("secret.txt");
        Files.write(secret, "secret".getBytes(StandardCharsets.UTF_8));
        PhotoStorage storage = new PhotoStorage(tempDir.toString());

        assertFalse(storage.read("../secret.txt").isPresent());
        assertFalse(storage.read("..\\secret.txt").isPresent());
        assertFalse(storage.read("photo.jpg").isPresent());
        assertFalse(storage.read(null).isPresent());
        Files.deleteIfExists(secret);
    }

    @Test
    void deleteIgnoresUnsafeNamesAndRemovesRealOnes() {
        PhotoStorage storage = new PhotoStorage(tempDir.toString());
        String name = storage.save(new byte[] {9});

        storage.delete("../anything.jpg"); // কিছু হবে না, Error-ও না
        storage.delete(name);

        assertFalse(Files.exists(tempDir.resolve(name)));
    }
}