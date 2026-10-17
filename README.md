<p align="center"><img src="src/main/resources/assets/ft10/icon.png" width="128" alt="FT10 logo"></p>

# FT10 — PvP Score Counter

A client-side Fabric mod that keeps a **first-to score** against whoever you're fighting on FFA and duel
servers. It remembers each opponent, shows `4 - 7` above your action bar (your score green, theirs red), and
announces every point with a title, a chat line and a sound, like a duel server does.

- **Per-opponent scores.** Start fighting someone and their score shows up. Fight someone else and the screen
  switches to them. Go back to the first player and you continue where you left off.
- **First to N.** `/ft goal 10` turns it into an FT10: reaching 10 shows **VICTORY** or **DEFEAT** and starts
  a new set, counting sets won.
- **Works on vanilla and plugin servers, with nothing installed on the server.** Reads death messages, kill
  messages in chat, action bar and titles, sidebar kill counters and recent hits; you can add your own server's
  format as a simple template.
- **Saved per server.** Scores survive restarts until you reset them.
- **Fully configurable** through Mod Menu or `/ft config`: position, size, colors, title, chat and sound. A
  drag-and-drop HUD editor (`/ft hud`) and optional hotkeys (toggle, undo, reset, settings) are included.
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
| `/ft hud` | Drag the score where you want it, scroll to resize |
| `/ft config` | Open the settings |
| `/ft debug` · `debug last` | Explain kill detection in chat; show messages FT10 didn't recognize |

## How kills are detected
The client is never told "X killed Y", so FT10 combines several signals. Points only ever count when you are
the killer or the victim.

1. **Death messages.** Vanilla and Paper death messages name the victim and killer (with their UUIDs), also when
   a plugin wraps them in its own prefix.
2. **Kill-message plugins.** Built-in formats cover chat, action bar and title messages like `X was slain by Y`,
   `Y killed X`, `You killed X`, `You were slain by Y` and `KILLED X`. Add your server's format under
   *Settings → Detection → Custom kill message patterns* as a template (`{victim} was killed by {killer}`, `*`
   matches anything) or as a regex with `(?<victim>...)` / `(?<killer>...)` groups.
3. **Sidebar counters.** On servers that respawn players without a real death, a `Kills` / `Deaths` line in the
   sidebar going up by one is matched with the player you just fought. The labels are configurable.
4. **Recent hits.** If a player dies without a usable message, whoever hit them last within the kill credit
   window (10 s by default) gets the kill.
5. **Duel servers.** Round messages that only name the winner (`Steve won the round 1 - 0`), "Round Won" /
   "You Died" titles, and a switch to spectator right after a fight are read as the end of your fight.
6. **Combat logs** (off by default): an opponent who leaves mid-fight counts as your kill.

The same death is never counted twice. If a point is ever wrong, click **[Undo]** in chat or run `/ft undo`.

**A server's kills not counted?** Run `/ft debug`: FT10 then explains every signal in chat (only you see it) and
lists messages about your fight it didn't understand, with a **[Copy]** button. `/ft debug last` shows the last
ten. Paste the message in an [issue](https://github.com/ZeroArsen/ft10/issues) or add it as a custom pattern.

## Supported versions
Fabric. Planned range is Minecraft **1.21 – 26.3** (one jar per version range); right now **26.2** and
**1.21.11** are built, the other versions are being ported. Requires
[Fabric API](https://modrinth.com/mod/fabric-api) and [YACL](https://modrinth.com/mod/yacl);
[Mod Menu](https://modrinth.com/mod/modmenu) is optional.

## Building
```
./gradlew build               # builds and tests every version: versions/<version>/build/libs/
./gradlew test                # unit tests, every version
./gradlew :26.2:runClient     # dev client; :26.2:runOpponent starts a second player, :26.2:runServer a server
./gradlew :1.21.11:runClient  # the same for 1.21.11 (game folders under run/1.21.11/)
```
JDK 25 (and JDK 21 for 1.21.x) is downloaded automatically through Gradle toolchains. The project uses
[Stonecutter](https://stonecutter.kikugie.dev/) for multi-version builds; see `context/ARCHITECTURE.md`.

## Credits
Made by **0Arsen** ([Modrinth](https://modrinth.com/user/0Arsen) · [GitHub](https://github.com/ZeroArsen)).

## License
[GPL-3.0-only](LICENSE).
