package dev.zerorsen.ft10.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import dev.zerorsen.ft10.Ft10Client;
import dev.zerorsen.ft10.core.Rivalry;
import dev.zerorsen.ft10.core.ScoreBook;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Keeps one {@link ScoreBook} per server and persists them to {@code ft10/scores.json} in the game directory.
 * Saving is debounced: changes are written a couple of seconds after the last point, and on disconnect.
 */
public final class ScoreStorage {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int FORMAT_VERSION = 1;
    private static final long SAVE_DELAY_MS = 2_000;

    /** JSON shape of the file. */
    private static final class FileData {
        int version = FORMAT_VERSION;
        Map<String, Map<String, Entry>> servers = new LinkedHashMap<>();
    }

    private static final class Entry {
        String name;
        int me;
        int them;
        int setsMe;
        int setsThem;
        long lastFought;
    }

    private final Path file;
    private final Map<String, ScoreBook> books = new LinkedHashMap<>();
    private long dirtySince = -1;

    public ScoreStorage(Path file) {
        this.file = file;
    }

    /** The score book for a server, created empty if we never played there. */
    public ScoreBook book(String serverKey) {
        return books.computeIfAbsent(serverKey, key -> new ScoreBook());
    }

    public void load() {
        books.clear();
        if (!Files.exists(file)) return;
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            FileData data = GSON.fromJson(reader, FileData.class);
            if (data == null || data.servers == null) return;
            data.servers.forEach((server, rivals) -> {
                ScoreBook book = book(server);
                rivals.forEach((key, e) -> {
                    Rivalry r = new Rivalry(key, e.name != null ? e.name : key);
                    r.setScore(e.me, e.them);
                    r.setSets(e.setsMe, e.setsThem);
                    r.touch(e.lastFought);
                    book.put(r);
                });
            });
        } catch (IOException | JsonParseException e) {
            Ft10Client.LOGGER.error("Could not read {}, starting with empty scores", file, e);
        }
    }

    /** Saves if a book changed at least {@link #SAVE_DELAY_MS} ago. Call every tick. */
    public void tick(long now, boolean persist) {
        boolean changed = books.values().stream().anyMatch(ScoreBook::isDirty);
        if (!changed) {
            dirtySince = -1;
            return;
        }
        if (dirtySince < 0) dirtySince = now;
        if (now - dirtySince >= SAVE_DELAY_MS) flush(persist);
    }

    /** Saves right away if anything changed. */
    public void flush(boolean persist) {
        dirtySince = -1;
        if (books.values().stream().noneMatch(ScoreBook::isDirty)) return;
        books.values().forEach(ScoreBook::markClean);
        if (!persist) return;

        FileData data = new FileData();
        books.forEach((server, book) -> {
            if (book.isEmpty()) return;
            Map<String, Entry> rivals = new LinkedHashMap<>();
            for (Rivalry r : book.all()) {
                Entry e = new Entry();
                e.name = r.name();
                e.me = r.me();
                e.them = r.them();
                e.setsMe = r.setsMe();
                e.setsThem = r.setsThem();
                e.lastFought = r.lastFought();
                rivals.put(r.key(), e);
            }
            data.servers.put(server, rivals);
        });
        try {
            ConfigManager.writeAtomically(file, GSON.toJson(data));
        } catch (IOException e) {
            Ft10Client.LOGGER.error("Could not save {}", file, e);
        }
    }
}
