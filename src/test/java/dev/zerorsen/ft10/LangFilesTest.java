package dev.zerorsen.ft10;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

/** Every translation file parses and has exactly the same keys as English. */
class LangFilesTest {
    @Test
    void turkishMatchesEnglish() throws Exception {
        Map<String, String> english = load("en_us");
        Map<String, String> turkish = load("tr_tr");
        assertEquals(new TreeSet<>(english.keySet()), new TreeSet<>(turkish.keySet()));
    }

    private static Map<String, String> load(String locale) throws Exception {
        String path = "/assets/ft10/lang/" + locale + ".json";
        try (InputStream in = LangFilesTest.class.getResourceAsStream(path)) {
            assertNotNull(in, path);
            return new Gson().fromJson(new InputStreamReader(in, StandardCharsets.UTF_8),
                new TypeToken<Map<String, String>>() {}.getType());
        }
    }
}
