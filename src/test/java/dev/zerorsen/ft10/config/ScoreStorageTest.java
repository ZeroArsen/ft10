package dev.zerorsen.ft10.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.zerorsen.ft10.core.PlayerRef;
import dev.zerorsen.ft10.core.ScoreBook;
import dev.zerorsen.ft10.core.Side;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ScoreStorageTest {
    @TempDir
    Path dir;

    @Test
    void resetEverythingEmptiesEveryServerAndSaves() {
        Path file = dir.resolve("scores.json");
        ScoreStorage storage = new ScoreStorage(file);
        ScoreBook server1 = storage.book("play.example.net");
        server1.award(PlayerRef.of(UUID.randomUUID(), "Alex"), Side.ME, 0, true, 1);
        server1.award(PlayerRef.of(UUID.randomUUID(), "Bob"), Side.THEM, 0, true, 2);
        storage.book("duels.example.net").award(PlayerRef.of(UUID.randomUUID(), "Carl"), Side.ME, 0, true, 3);
        storage.flush(true);

        assertEquals(3, storage.resetEverything(true));
        assertSame(server1, storage.book("play.example.net"), "a running session keeps its book");
        assertTrue(server1.isEmpty());

        ScoreStorage reloaded = new ScoreStorage(file);
        reloaded.load();
        assertTrue(reloaded.book("play.example.net").isEmpty());
        assertTrue(reloaded.book("duels.example.net").isEmpty());
    }
}
