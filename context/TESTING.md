# Manual test checklist

## Setup (26.2, two players on one PC)
1. `./gradlew :26.2:runServer` once, then in `run/server/`: set `eula=true` in `eula.txt` (accepting the
   Minecraft EULA) and in `server.properties`: `online-mode=false`, `level-type=minecraft:flat`,
   `spawn-protection=0`, and for scripted tests `enable-rcon=true` + `rcon.password`. Start it again.
2. In `run/options.txt` and `run/opponent/options.txt` set `pauseOnLostFocus:false` (so an unfocused client
   keeps drawing the HUD) and `tutorialStep:none`.
3. `./gradlew :26.2:runClient -Pft10.join=localhost` (Ft10Dev) and
   `./gradlew :26.2:runOpponent -Pft10.join=localhost` (Ft10Rival): both join the server on their own.
4. Server console or RCON: `gamerule immediate_respawn true`, `difficulty peaceful` (flat worlds spawn slimes).
   26.x gamerules are snake_case (`show_death_messages`, `immediate_respawn`).
   `damage Ft10Rival 100 minecraft:player_attack by Ft10Dev` kills with a vanilla death message.
5. Results: `[CHAT]` lines in `run/logs/latest.log` (Ft10Dev) and `run/opponent/logs/latest.log` (Ft10Rival).

## Automated run — 2026-10-17, 26.2 (RCON-driven, both clients)
| Scenario | Result |
|---|---|
| Vanilla death message kill | ✅ Dev `+1 … (1 - 0)`, Rival `+1 Ft10Dev killed you (0 - 1)`, one line each |
| Reverse kill | ✅ `1 - 1` both sides |
| Death messages off, hit + `/kill` | ❌ → ✅ killer side missed it with instant respawn (health never synced as 0); fixed by reading the entity death event (event 3) |
| Hit, then `/kill` 12 s later | ✅ no point on either side |
| `tellraw "☠ Ft10Rival was killed by Ft10Dev"` | ✅ preset regex, both sides |
| Totem pop | ✅ no point |
| Goal 3 reached | ✅ VICTORY title, `FT3 · 3 - 0`, chat `You won FT3 … | Sets 2 - 0`, new set 0 - 0 |
| Killed by a slime | ✅ no point |
| Client restart | ✅ scores saved per server and resumed |
| HUD next to a server action bar ("Combat: 10s") | ✅ drawn just above it, no overlap (screenshot) |

## Still to check by hand
- [ ] Kill with a bow; void/fall after a hit ("doomed to fall by").
- [ ] Third player hits Ft10Dev mid-fight → screen stays; new opponent → 0 - 0; back → old score.
- [ ] `/ft undo` and the chat [Undo] button.
- [ ] `/ft lock`, `/ft unlock`, `/ft list`, `/ft reset`, `/ft reset all` (+ confirm), `/ft set 4 7`, `/ft off|on`.
- [ ] `/ft config` and Mod Menu → settings screen; color/scale/offset changes show on the HUD.
- [ ] HUD editor (`/ft hud`, settings button, hotkey): **drag with the real mouse** (synthetic input can't test it:
      Minecraft ignores drags while the window is unfocused). Opening via `/ft hud`, scroll resize, arrow keys and
      Esc-to-save were checked by script on 2026-10-17; R, Reset and Cancel were not.
- [ ] Hotkeys in Options → Controls → FT10: toggle, HUD, undo, reset, settings, editor.
- [ ] F1 hides the HUD. Turkish client language → Turkish texts.
- [ ] Public FFA servers with plugin kill messages (owner).
