package dev.zerorsen.ft10.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import dev.zerorsen.ft10.Ft10Client;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Loads and saves {@link Ft10Config}. */
public final class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path file;
    private Ft10Config config = new Ft10Config();

    public ConfigManager(Path file) {
        this.file = file;
    }

    public Ft10Config get() {
        return config;
    }

    public void load() {
        if (!Files.exists(file)) {
            save();
            return;
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            Ft10Config loaded = GSON.fromJson(reader, Ft10Config.class);
            config = loaded != null ? loaded : new Ft10Config();
        } catch (IOException | JsonParseException e) {
            Ft10Client.LOGGER.error("Could not read {}, using defaults", file, e);
            config = new Ft10Config();
        }
        config.sanitize();
    }

    public void save() {
        config.sanitize();
        try {
            writeAtomically(file, GSON.toJson(config));
        } catch (IOException e) {
            Ft10Client.LOGGER.error("Could not save {}", file, e);
        }
    }

    /** Writes to a temp file first so a crash mid-write never leaves a half-written file behind. */
    static void writeAtomically(Path target, String content) throws IOException {
        Files.createDirectories(target.getParent());
        Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
        try (Writer writer = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
            writer.write(content);
        }
        try {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException atomicNotSupported) {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
