package dev.zerorsen.ft10.net;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.netty.buffer.Unpooled;
import java.util.HexFormat;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

/**
 * The wire format is a contract with server plugins: these bytes are also in docs/SERVER_INTEGRATION.md and in
 * RevoltFFA's Ft10Bridge test. Changing them breaks every server that integrated.
 */
class Ft10ProtocolTest {
    private static final UUID STEVE = new UUID(1, 2);
    private static final UUID ALEX = new UUID(3, 4);

    /** Alex killed Steve: version 1, victim, "Steve", has killer, killer, "Alex", cause 0. */
    static final String KILL_HEX = "01" + "0000000000000001" + "0000000000000002" + "05" + "5374657665"
        + "01" + "0000000000000003" + "0000000000000004" + "04" + "416c6578" + "00";

    /** Steve combat-logged with no one to credit: version 1, victim, "Steve", no killer, cause 1. */
    static final String NO_KILLER_HEX = "01" + "0000000000000001" + "0000000000000002" + "05" + "5374657665" + "00" + "01";

    @Test
    void readsAKill() {
        Ft10Protocol.Kill kill = decode(KILL_HEX);
        assertTrue(kill.valid());
        assertEquals(1, kill.version());
        assertEquals(STEVE, kill.victim());
        assertEquals("Steve", kill.victimName());
        assertEquals(ALEX, kill.killer());
        assertEquals("Alex", kill.killerName());
        assertEquals(Ft10Protocol.Kill.CAUSE_KILLED, kill.cause());
    }

    @Test
    void readsADeathWithoutKiller() {
        Ft10Protocol.Kill kill = decode(NO_KILLER_HEX);
        assertTrue(kill.valid());
        assertNull(kill.killer());
        assertEquals(Ft10Protocol.Kill.CAUSE_COMBAT_LOG, kill.cause());
    }

    @Test
    void writesTheSameBytes() {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        Ft10Protocol.Kill.CODEC.encode(buf, Ft10Protocol.Kill.of(STEVE, "Steve", ALEX, "Alex", 0));
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        assertArrayEquals(HexFormat.of().parseHex(KILL_HEX), bytes);
    }

    @Test
    void newerVersionsWithExtraFieldsStillRead() {
        FriendlyByteBuf buf = buffer(KILL_HEX.replaceFirst("^01", "02") + "deadbeef");
        Ft10Protocol.Kill kill = Ft10Protocol.Kill.CODEC.decode(buf);
        assertTrue(kill.valid());
        assertEquals(ALEX, kill.killer());
        assertEquals(0, buf.readableBytes(), "extra bytes must be consumed or the client gets kicked");
    }

    @Test
    void brokenPayloadIsInvalidNotAnException() {
        FriendlyByteBuf buf = buffer(KILL_HEX.substring(0, 20));
        Ft10Protocol.Kill kill = Ft10Protocol.Kill.CODEC.decode(buf);
        assertFalse(kill.valid());
        assertEquals(0, buf.readableBytes());
    }

    @Test
    void hello() {
        assertEquals(1, Ft10Protocol.Hello.CODEC.decode(buffer("01")).version());
        assertEquals(0, Ft10Protocol.Hello.CODEC.decode(buffer("")).version());
    }

    private static Ft10Protocol.Kill decode(String hex) {
        return Ft10Protocol.Kill.CODEC.decode(buffer(hex));
    }

    private static FriendlyByteBuf buffer(String hex) {
        return new FriendlyByteBuf(Unpooled.wrappedBuffer(HexFormat.of().parseHex(hex)));
    }
}
