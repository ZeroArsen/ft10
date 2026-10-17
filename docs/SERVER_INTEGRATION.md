# FT10 server integration

FT10 works out kills on its own from death messages, hits and health. That is a guess: servers with fake
deaths, custom kill messages, nicknames or combat-log kills can confuse it. A server plugin that already knows
who killed whom can **tell** FT10 instead. Players with the mod then get exact scores, players without it
notice nothing (plugin messages on unknown channels are dropped).

## Protocol v1
Two plugin channels, server → client. All numbers are big-endian; strings are a VarInt byte length followed by
UTF-8 bytes (the usual Minecraft string encoding, *not* `DataOutputStream.writeUTF`).

### `ft10:hello`
| Field | Type | Value |
|---|---|---|
| version | byte | `1` |

Send it once the client listens on `ft10:hello` (the client announces both channels when it joins; servers like
Paper silently drop messages on a channel the client hasn't registered yet, and `ft10:kill` may be registered
first). From then on
FT10 stops guessing on this server and only counts `ft10:kill` messages (players can turn this off under
*Settings → Detection → Trust server kill reports*).

### `ft10:kill`
Send it to the **victim** and the **killer** whenever your plugin attributes a death.

| Field | Type | Notes |
|---|---|---|
| version | byte | `1` |
| victim | UUID | two longs: most, then least significant bits |
| victimName | string | max 64 bytes |
| hasKiller | boolean | one byte, `0` or `1` |
| killer | UUID | only if `hasKiller` |
| killerName | string | only if `hasKiller` |
| cause | byte | `0` = killed, `1` = combat log |

Newer versions only ever **append** fields, and FT10 skips bytes it doesn't know, so a v2 plugin keeps working
with v1 mods. A payload FT10 can't read is ignored; it never disconnects the player.

**Test vector:** Alex (`00000000-0000-0003-0000-000000000004`) killed Steve
(`00000000-0000-0001-0000-000000000002`):
```
01 0000000000000001 0000000000000002 05 5374657665 01 0000000000000003 0000000000000004 04 416c6578 00
```

## Paper example
```java
public final class Ft10Bridge implements Listener {
    private static final String HELLO = "ft10:hello";
    private static final String KILL = "ft10:kill";
    private final Plugin plugin;

    public Ft10Bridge(Plugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, HELLO);
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, KILL);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    // Paper drops messages on channels the client hasn't registered yet, so say hello when ft10:hello itself is
    // registered. The client may register before or after the join event, so check on join too.
    @EventHandler
    public void onRegister(PlayerRegisterChannelEvent event) {
        if (HELLO.equals(event.getChannel())) event.getPlayer().sendPluginMessage(plugin, HELLO, new byte[] {1});
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.getListeningPluginChannels().contains(HELLO)) player.sendPluginMessage(plugin, HELLO, new byte[] {1});
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getPlayer();
        Player killer = victim.getKiller(); // or your own attribution
        byte[] data = encode(victim.getUniqueId(), victim.getName(),
            killer == null ? null : killer.getUniqueId(), killer == null ? null : killer.getName(), 0);
        send(victim, data);
        if (killer != null && killer != victim) send(killer, data);
    }

    private void send(Player player, byte[] data) {
        if (listening(player)) player.sendPluginMessage(plugin, KILL, data);
    }

    private static boolean listening(Player player) {
        return player.getListeningPluginChannels().contains(KILL);
    }

    static byte[] encode(UUID victim, String victimName, UUID killer, String killerName, int cause) {
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeByte(1);
            out.writeLong(victim.getMostSignificantBits());
            out.writeLong(victim.getLeastSignificantBits());
            writeString(out, victimName);
            out.writeBoolean(killer != null);
            if (killer != null) {
                out.writeLong(killer.getMostSignificantBits());
                out.writeLong(killer.getLeastSignificantBits());
                writeString(out, killerName);
            }
            out.writeByte(cause);
            out.flush();
            return bytes.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void writeString(DataOutputStream out, String value) throws IOException {
        byte[] utf8 = value.getBytes(StandardCharsets.UTF_8);
        int length = utf8.length;
        while ((length & ~0x7F) != 0) {
            out.writeByte((length & 0x7F) | 0x80);
            length >>>= 7;
        }
        out.writeByte(length);
        out.write(utf8);
    }
}
```
Create it once in `onEnable` (`new Ft10Bridge(this)`). If your plugin has its own kill attribution (assists, combat
logging, fake deaths), call `encode` + `send` from there instead of `PlayerDeathEvent`, with cause `1` for
combat logs.
