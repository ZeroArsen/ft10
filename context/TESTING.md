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

## Automated run — 2026-10-17, 0.2.0 kill signals (26.2 and 1.21.11, RCON-driven, `-Pft10.debug`)
Same on both versions unless noted.

| Scenario | Result |
|---|---|
| `tellraw @a ["☠ ", {translate death.attack.player, with [Rival, Dev]}]` (wrapped vanilla) | ✅ one point each side, source `vanilla` |
| `tellraw Ft10Dev "You killed Ft10Rival!"` | ✅ Dev +1 (`you-killed`) |
| `title Ft10Dev actionbar "You killed Ft10Rival"` ×5 over 6 s | ✅ one point |
| Sidebar `Kills` 5 → 6 after Dev hit Rival (no death at all) | ✅ Dev +1 (weak guess Rival); Rival (only got hit): "you hit nobody recently", no point |
| Sidebar `Kills` +1, `Deaths` +1 and "Ft10Rival was slain by Ft10Dev" together (26.2) | ✅ exactly one point each side |
| Debug: `tellraw Ft10Dev "REKT Ft10Rival (+5 coins)"` mid-fight (26.2) | ✅ "Not recognized (chat): … [Copy]" |
| `title subtitle "Ft10Rival"` + `title title "ELIMINATED"` (26.2) | ✅ Dev +1 (`kill-title`, title + subtitle read together) |
| Real kill (`damage … 100 … by Ft10Dev`) | ✅ one point each side (death screen + chat message deduped) |

Run `-Pft10.debug` with `-Pft10.join=localhost` to get the debug lines in `[CHAT]` and `[debug]` log lines.

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
- [ ] Public FFA servers with plugin kill messages (owner): run `/ft debug`, report "Not recognized" lines.
- [ ] Real fight with sword swings: sidebar kill counter on a fake-death server; `countCombatLogs` on.
