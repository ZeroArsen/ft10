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
- [ ] **5. Multi-version** — **1.21.11 done** (2026-10-17: both jars build, 36 tests pass on each, RCON
  two-player run in `TESTING.md`). Left: 26.3 and 26.1.2, then 1.21.10 down to 1.21.1. Versions per node are
  already looked up (see below).
- [ ] **6. Release** — GitHub repo ✅ (public, CI green), Modrinth publishing set up (Minotaur + release.yml);
  waiting for the owner to create the Modrinth project `ft10` and add the `MODRINTH_TOKEN` secret, then tag v0.1.0.

## Next task
The owner reviews and commits RevoltFFA's `Ft10Bridge`, then tries the 1.21.11 jar on the real RevoltFFA server.
Next port: 26.1.2 (it settles whether each `>=26.1` guard really belongs at 26.1, see ARCHITECTURE.md) and 26.3,
then 1.21.10 down to 1.21.1 (the 1.21.9, 1.21.6 and 1.21.5 boundaries in ARCHITECTURE.md still need guards).

## Dependency versions per node (Modrinth, 2026-10-17)
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
