package dev.zerorsen.ft10.net;

import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server integration, protocol version 1 (spec: {@code docs/SERVER_INTEGRATION.md}).
 *
 * <p>A server plugin that knows exactly who killed whom tells the mod through two plugin channels. The decoders
 * never throw: a payload from a buggy or newer plugin is read as far as possible, the rest is skipped, and a
 * broken one is marked invalid instead of kicking the player.
 */
public final class Ft10Protocol {
    public static final int VERSION = 1;
    private static final int MAX_NAME = 64;

    private Ft10Protocol() {}

    /** {@code ft10:hello} — "this server reports kills". Sent once the client listens on {@code ft10:kill}. */
    public record Hello(int version) implements CustomPacketPayload {
        public static final Type<Hello> TYPE = new Type<>(Identifier.fromNamespaceAndPath("ft10", "hello"));
        public static final StreamCodec<FriendlyByteBuf, Hello> CODEC = StreamCodec.of(
            (buf, hello) -> buf.writeByte(hello.version()),
            buf -> {
                int version = buf.isReadable() ? buf.readUnsignedByte() : 0;
                buf.skipBytes(buf.readableBytes());
                return new Hello(version);
            });

        @Override
        public Type<Hello> type() {
            return TYPE;
        }
    }

    /**
     * {@code ft10:kill} — a death the server attributed, sent to the victim and the killer.
     *
     * @param killer null when nobody gets the credit
     * @param cause  {@link #CAUSE_KILLED} or {@link #CAUSE_COMBAT_LOG}
     * @param valid  false when the payload couldn't be read
     */
    public record Kill(int version, UUID victim, String victimName, UUID killer, String killerName, int cause,
                       boolean valid) implements CustomPacketPayload {
        public static final int CAUSE_KILLED = 0;
        public static final int CAUSE_COMBAT_LOG = 1;

        public static final Type<Kill> TYPE = new Type<>(Identifier.fromNamespaceAndPath("ft10", "kill"));
        public static final StreamCodec<FriendlyByteBuf, Kill> CODEC = StreamCodec.of(Kill::write, Kill::read);

        public static Kill of(UUID victim, String victimName, UUID killer, String killerName, int cause) {
            return new Kill(VERSION, victim, victimName, killer, killerName, cause, true);
        }

        private static void write(FriendlyByteBuf buf, Kill kill) {
            buf.writeByte(kill.version());
            buf.writeUUID(kill.victim());
            buf.writeUtf(kill.victimName());
            buf.writeBoolean(kill.killer() != null);
            if (kill.killer() != null) {
                buf.writeUUID(kill.killer());
                buf.writeUtf(kill.killerName());
            }
            buf.writeByte(kill.cause());
        }

        private static Kill read(FriendlyByteBuf buf) {
            try {
                int version = buf.readUnsignedByte();
                UUID victim = buf.readUUID();
                String victimName = buf.readUtf(MAX_NAME);
                UUID killer = null;
                String killerName = null;
                if (buf.readBoolean()) {
                    killer = buf.readUUID();
                    killerName = buf.readUtf(MAX_NAME);
                }
                int cause = buf.readUnsignedByte();
                return new Kill(version, victim, victimName, killer, killerName, cause, true);
            } catch (RuntimeException malformed) {
                return new Kill(0, null, null, null, null, 0, false);
            } finally {
                buf.skipBytes(buf.readableBytes()); // fields added by newer protocol versions
            }
        }

        @Override
        public Type<Kill> type() {
            return TYPE;
        }
    }
}
