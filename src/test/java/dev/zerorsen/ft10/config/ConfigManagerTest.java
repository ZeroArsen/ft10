package dev.zerorsen.ft10.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfigManagerTest {
    @TempDir
    Path dir;

    @Test
    void oldConfigGetsHeadsOnAndKeepsItsValues() throws IOException {
        // Written by 0.3.0, before the heads option existed.
        Path file = dir.resolve("ft10.json");
        Files.writeString(file, "{\"hudScale\": 1.5, \"hudShowName\": false}", StandardCharsets.UTF_8);

        ConfigManager manager = new ConfigManager(file);
        manager.load();

        assertTrue(manager.get().hudShowHeads, "new option defaults to on");
        assertEquals(1.5f, manager.get().hudScale);
        assertEquals(false, manager.get().hudShowName);
    }
}
