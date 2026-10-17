<p align="center"><img src="src/main/resources/assets/ft10/icon.png" width="128" alt="FT10 logo"></p>

# FT10 — PvP Score Counter

A client-side Fabric mod that keeps a **first-to score** against whoever you're fighting on FFA and duel
servers. It remembers each opponent, shows `4 - 7` above your action bar (your score green, theirs red), and
announces every point with a title, a chat line and a sound, like a duel server does.

- **Per-opponent scores.** Start fighting someone and their score shows up. Fight someone else and the screen
  switches to them. Go back to the first player and you continue where you left off.
- **First to N.** `/ft goal 10` turns it into an FT10: reaching 10 shows **VICTORY** or **DEFEAT** and starts
  a new set, counting sets won.
- **Works on vanilla and plugin servers.** Reads vanilla death messages, recent hits, and common kill-message
  formats; you can add your own server's format as a regex.
- **Saved per server.** Scores survive restarts until you reset them.
- **Fully configurable** through Mod Menu or `/ft config`: position, size, colors, title, chat and sound.
- **Fair play.** FT10 only shows information your client already receives. It sends nothing to the server and
  automates nothing.

## Commands
`/ft10` always works; `/ft` is a shorter alias you can turn off if a server has its own `/ft`.

| Command | What it does |
|---|---|
| `/ft` | Current opponent and score |
| `/ft on` · `off` · `toggle` | Turn tracking on or off |
| `/ft reset` · `reset <player>` · `reset all` | Reset the current opponent, one player, or everyone on this server |
| `/ft set <me> <them>` | Fix the current score by hand |
| `/ft undo` | Take back the last point |
| `/ft goal <n>` · `goal off` | Play first to N |
| `/ft lock <player>` · `unlock` | Keep one opponent on screen |
| `/ft list` | Every score on this server |
| `/ft config` | Open the settings |

## How kills are detected
The client is never told "X killed Y", so FT10 combines several signals:

1. **Death messages.** Vanilla and Paper death messages name the victim and killer (with their UUIDs).
2. **Kill-message plugins.** Built-in patterns cover formats like `X was slain by Y` and `Y killed X`; add
   your own under *Settings → Detection → Custom kill message patterns* using `(?<victim>...)` and
   `(?<killer>...)` groups.
3. **Recent hits.** If a player dies without a usable message, whoever hit them last within the kill credit
   window (10 s by default) gets the kill.

The same death is never counted twice. If a point is ever wrong, click **[Undo]** in chat or run `/ft undo`.

## Supported versions
Fabric. Planned range is Minecraft **1.21 – 26.3** (one jar per version range); right now only **26.2** is
built, the other versions are being ported. Requires
[Fabric API](https://modrinth.com/mod/fabric-api) and [YACL](https://modrinth.com/mod/yacl);
[Mod Menu](https://modrinth.com/mod/modmenu) is optional.

## Türkçe
FT10, FFA ve duel sunucularında savaştığın rakibe karşı skoru tutan, sadece client tarafında çalışan bir Fabric
modudur. Her rakibin skoru ayrı tutulur ve action bar'ın üstünde `4 - 7` şeklinde gösterilir. Her puanda title,
chat mesajı ve ses gelir. `/ft goal 10` ile FT10 oynanır. Komutların hepsi `/ft help` altında; ayarlar Mod Menu
veya `/ft config` ile açılır. Mod sunucuya hiçbir şey göndermez, sadece client'ın zaten gördüğü bilgiyi kullanır.

## Building
```
./gradlew build            # builds the active version (see stonecutter.gradle.kts)
./gradlew test             # unit tests
./gradlew :26.2:runClient  # dev client; :26.2:runOpponent starts a second player, :26.2:runServer a server
```
JDK 25 is downloaded automatically through Gradle toolchains. The project uses
[Stonecutter](https://stonecutter.kikugie.dev/) for multi-version builds; see `context/ARCHITECTURE.md`.

## License
[GPL-3.0-only](LICENSE).
