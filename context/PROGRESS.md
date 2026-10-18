# Progress

## Phases
- [x] **0. Skeleton** — Stonecutter template setup (26.2 node only), GPL-3.0-only, icon from the owner's logo.
- [x] **1. Core** — `PlayerRef`, `Rivalry`, `ScoreBook`, `CombatTracker`, `KillDetector`, `DeathMessageParser`,
  `Session`; 23 unit tests.
- [x] **2. Minecraft wiring (26.2)** — damage/combat-kill mixin, game-message death parsing, health-based death
  scan, HUD element, title/chat/sound notifier, `/ft10` + `/ft` commands. `DeathMessagesTest` (6) covers vanilla
  translatable parsing with real Minecraft classes.
- [x] **3. Settings** — `Ft10Config` (config/ft10.json), YACL screen, Mod Menu entrypoint, en_us + tr_tr.
- [x] **4. Real test** — RCON-driven two-player run on a Fabric 26.2 server (found and fixed the instant-respawn
  miss). See `TESTING.md`.
- [x] **4b. Round 2 features** — HUD editor, hotkeys, author 0Arsen.
- [x] **4c. Server integration removed** (D16): FT10 needs nothing on the server.
- [x] **4d. Kill detection round 2** (D17–D19): wrapped vanilla messages, second-person and broad presets,
  templates, action bar/title parsing, sidebar kill/death counters, optional combat logs, `/ft debug`. 57 unit
  tests per version; RCON run on 26.2 and 1.21.11 (`TESTING.md`).
- [x] **4e. Duel servers** (D20, D21): `{winner}` messages, round titles, spectator switch as a death; HUD
  without "vs"; reset-all buttons. Built from a study of the owner's RevoltPvP; 64 unit tests per version.
- [x] **5. Multi-version** (D22) — ten jars cover Minecraft 1.21 → 26.3: 1.21.1, 1.21.3, 1.21.4, 1.21.5, 1.21.8,
  1.21.10, 1.21.11, 26.1.2, 26.2, 26.3. Every node builds and passes the 64 unit tests, compiles against the lowest
  patch of its range, and passed the RCON two-player run (`TESTING.md`, 2026-10-18). Released in 0.3.0.
- [x] **6. Release** — GitHub repo (public, CI green); Modrinth project `ft10` (in review). v0.1.0 (26.2)
  released 2026-10-17; v0.2.0 (26.2 + 1.21.11) adds the 1.21.11 jar and the new detection; v0.2.1 the duel signals;
  v0.3.0 every version from 1.21 to 26.3.

## Next task
The owner tests on RevoltPvP and public servers with `/ft debug` and reports "Not recognized" messages; add the
formats as presets. The HUD editor and the settings buttons still need a click test on an old version (1.21.1).
A new Minecraft release gets its own node: add it to `settings.gradle.kts` and `stonecutter.properties.toml`
(look the Fabric API / YACL / Mod Menu versions up on Modrinth), build, fix what breaks in `compat/`.

## Dependency versions per node (Modrinth, 2026-10-18)
| Node | Fabric API | YACL | Mod Menu |
|---|---|---|---|
| 1.21.1 | 0.116.17+1.21.1 | 3.8.2+1.21.1-fabric | 11.0.5 |
| 1.21.3 | 0.114.1+1.21.3 | 3.8.2+1.21.3-fabric | 12.0.1 |
| 1.21.4 | 0.119.4+1.21.4 | 3.8.2+1.21.4-fabric | 13.0.4 |
| 1.21.5 | 0.128.2+1.21.5 | 3.8.2+1.21.5-fabric | 14.0.2 |
| 1.21.8 | 0.136.1+1.21.8 | 3.8.2+1.21.6-fabric | 15.0.2 |
| 1.21.10 | 0.138.4+1.21.10 | 3.8.2+1.21.10-fabric | 16.0.1 |
| 1.21.11 | 0.141.6+1.21.11 | 3.8.2+1.21.11-fabric | 17.0.1 |
| 26.1.2 | 0.155.3+26.1.2 | 3.9.7+26.1-fabric | 18.0.2 |
| 26.2 | 0.161.0+26.2 | 3.9.7+26.2-fabric | 20.0.3 |
| 26.3 | 0.163.0+26.3 | 3.9.7+26.3-fabric | 21.0.0 |
