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

## Other versions (1.21.11 so far)
- Same steps with the version in the task name: `:1.21.11:runServer`, `:1.21.11:runClient`, `:1.21.11:runOpponent`.
- Every version except 26.2 runs in `run/<version>/` (client), `run/<version>/opponent/` and
  `run/<version>/server/`, so a 26.2 world is never opened by 1.21.11.
- 1.21.11 gamerules are snake_case too (`immediate_respawn`; `doImmediateRespawn` is rejected).
- Screenshots without touching the owner's own windows: post F2 as a window message to the dev client's window
  (`PostMessage` WM_KEYDOWN/WM_KEYUP, scancode 0x3C); the game saves it to `run/<version>/screenshots/`.
- Chat commands can be typed the same way (T, then WM_CHAR per letter, then Enter). From Git Bash set
  `MSYS_NO_PATHCONV=1`, or `/ft hud` turns into a Windows path and goes to the server as a chat line.

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

## Server integration run — 2026-10-17, Paper 26.2 build 134 + test plugin (docs example code)
| Scenario | Result |
|---|---|
| Join | ✅ `ft10:hello` after the fix above (first try: dropped because sent before the client registered it) |
| Real kill (plugin report + vanilla message) | ✅ exactly one point each side |
| `ft10test kill` without any death (fake-death server) | ✅ counted, reached VICTORY |
| Regex kill message on a reporting server | ✅ ignored |
| Combat log (cause 1) | ✅ counted for the other player |

## Automated run — 2026-10-17, 1.21.11 (Fabric server, RCON-driven, both clients)
| Scenario | Result |
|---|---|
| Build | ✅ both jars; 36 unit tests pass on 1.21.11 and on 26.2 |
| Dev client start | ❌ → ✅ YACL crashed (`ClassNotFoundException: org.quiltmc.parsers.json.gson.GsonWriter`): remapping Loom drops the libraries YACL nests in its jar. Fixed with dev-only `localRuntime` deps (D14); players are unaffected |
| Vanilla death message kill | ✅ Dev `+1 You killed Ft10Rival. (1 - 0)`, Rival `+1 Ft10Dev killed you. (0 - 1)`, one line each |
| Reverse kill | ✅ `1 - 1` both sides |
| Death messages off, hit + `/kill` (instant respawn on) | ✅ `2 - 1` both sides |
| Hit, then `/kill` 12 s later | ✅ no point on either side |
| `tellraw` "? Ft10Rival was killed by Ft10Dev" and "☠ Ft10Dev was killed by Ft10Rival" | ✅ preset regex, both sides |
| Totem pop | ✅ no point |
| Killed by a slime | ✅ no point |
| HUD (screenshot) | ✅ "vs Ft10Rival" + colored score above the hotbar; with a server action bar ("Combat: 10s") drawn just above it, no overlap |
| Client restart (kick, kill both clients, start again) | ✅ scores saved per server and resumed (`3 - 2` → `3 - 3`) |
| Goal 3 reached by the opponent | ✅ DEFEAT title, `FT3 · 3 - 3` subtitle, chat `You lost FT3 against Ft10Rival. (3 - 3) | Sets 0 - 1`, new set `0 - 0`; Rival `You won FT3 …` |
| `/ft hud` (typed by window messages) | ✅ editor drawn (help text, Done/Reset/Cancel, outline, center guide, world behind); arrow keys ×3 + Esc saved `hudOffsetX: 3` |
| `/ft config` | ✅ YACL screen opens (General / Display / Title, Chat & Sound / Detection) |

## Server integration run — 2026-10-17, RevoltFFA + Paper 1.21.11 build 132
The owner's `C:/dev/mc-plugins/RevoltFFA` (uncommitted `Ft10Bridge`, jar built 17:11) on a **copy** of its `run/`
folder in the session scratchpad, with `online-mode=false`, RCON and bStats off in the copy only; the owner's
files were not changed. Players joined the "duel" arena with `execute as <player> run ffa join duel`.

| Scenario | Result |
|---|---|
| Join with FT10 | ✅ both clients: "This server reports kills to FT10, so every point is exact." |
| Kill in the arena (`damage … by Ft10Dev` after spawn protection) | ✅ exactly one point each side; RevoltFFA's "☠ Ft10Rival was slain by Ft10Dev" (matches a preset regex) added nothing |
| Combat log: Dev hits Rival, Rival's client is killed | ✅ "☠ Ft10Rival logged out in combat — Ft10Dev gets the kill", Dev `+1 (2 - 0)`. The offline victim gets nothing, so Rival's own count stays one behind |
| Combat log when the victim was never hit (Rival still spawn-protected) | ✅ no attribution, nothing sent, no point |
| Kill in the lobby | ✅ nothing sent, no point |
| `integration.ft10: false` + `/rffa reload`, same connection | ⚠️ nothing sent, and FT10 counts nothing either: it still trusts the hello it got on join. RevoltFFA's checklist expects a fallback to guessing |
| … after both players rejoin | ✅ no hello, FT10 guesses again: one point each side |
| Player without the mod | not tested (no vanilla client in the script) |

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
