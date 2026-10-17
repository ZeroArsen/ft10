# Manual test checklist

## Setup (26.2, two players on one PC)
1. `./gradlew :26.2:runServer` once, then in `run/server/`: set `eula=true` in `eula.txt` (accepting the
   Minecraft EULA), `online-mode=false` in `server.properties`. Start it again.
2. `./gradlew :26.2:runClient` (Ft10Dev) and `./gradlew :26.2:runOpponent` (Ft10Rival); both join `localhost`.
3. In the server console: `op Ft10Dev`, `op Ft10Rival`, `gamemode survival @a`.
   Handy shortcuts: `damage Ft10Rival 100 minecraft:player_attack by Ft10Dev` kills with a vanilla death
   message; `gamerule showDeathMessages false` turns death messages off to test hit-based detection.

## Checklist
- [ ] Ft10Dev hits Ft10Rival → HUD shows `vs Ft10Rival` / `0 - 0` above the action bar.
- [ ] Ft10Dev kills Ft10Rival → title `1 - 0`, subtitle "You killed Ft10Rival", chat `ꜰᴛ | +1 … (1 - 0) [Undo]`,
      XP sound. Ft10Rival sees `0 - 1` and "Ft10Dev killed you".
- [ ] Kill with a bow → counts.
- [ ] Knock into the void / fall damage after a hit ("doomed to fall by") → counts.
- [ ] Totem pop → does **not** count.
- [ ] `gamerule showDeathMessages false`, hit then `/kill Ft10Rival` within 10 s → counts (health detection);
      after 10 s → doesn't.
- [ ] `tellraw @a "☠ Ft10Rival was killed by Ft10Dev"` with death messages off → counts (preset regex).
- [ ] Third player (or `/damage` by Ft10Rival on a third account) hits Ft10Dev mid-fight → screen stays.
- [ ] Fight a new player → screen switches to 0 - 0; return to the first → old score continues.
- [ ] `/ft undo` and the chat [Undo] button revert the last point.
- [ ] `/ft goal 3` → third kill shows VICTORY, `FT3 · 3 - x`, new set at 0 - 0, HUD shows sets.
- [ ] `/ft lock Ft10Rival`, `/ft unlock`, `/ft list`, `/ft reset`, `/ft reset all` (+ confirm), `/ft set 4 7`.
- [ ] `/ft off` → nothing tracked or shown; `/ft on` → back.
- [ ] `/ft config` and Mod Menu → settings screen opens; changing color/scale/offset updates the HUD.
- [ ] F1 hides the HUD.
- [ ] Restart the client → scores still there (`ft10/scores.json`); a different server has its own scores.
- [ ] Turkish client language → Turkish texts.
- [ ] Paper + RevoltFFA: custom kill messages and instant respawn still count exactly once.
