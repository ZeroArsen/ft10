# Progress

## Phases
- [x] **0. Skeleton** — Stonecutter template setup (26.2 node only), GPL-3.0-only, icon from the owner's logo.
- [x] **1. Core** — `PlayerRef`, `Rivalry`, `ScoreBook`, `CombatTracker`, `KillDetector`, `DeathMessageParser`,
  `Session`; 23 unit tests.
- [x] **2. Minecraft wiring (26.2)** — damage/combat-kill mixin, game-message death parsing, health-based death
  scan, HUD element, title/chat/sound notifier, `/ft10` + `/ft` commands. `DeathMessagesTest` (6) covers vanilla
  translatable parsing with real Minecraft classes.
- [x] **3. Settings** — `Ft10Config` (config/ft10.json), YACL screen, Mod Menu entrypoint, en_us + tr_tr.
- [ ] **4. Real test** — local server + two clients (see `TESTING.md`), then a Paper server with RevoltFFA for
  custom kill messages. **Not done yet**: the mod has only been launched to the title screen (loads cleanly).
- [ ] **5. Multi-version** — 26.3 and 26.1.2 first, then 1.21.11 down to 1.21.1. Versions per node are
  already looked up (see below).
- [ ] **6. Release** — GitHub repo, CI publish, Modrinth project page, v1.0.0.

## Next task
Phase 4: run `TESTING.md` on 26.2. The dedicated dev server needs `run/server/eula.txt` (the owner accepts
the Minecraft EULA) and `online-mode=false`.

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
