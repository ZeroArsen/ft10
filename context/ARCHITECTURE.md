# Architecture

## Packages (`dev.zerorsen.ft10`)
| Package | Role |
|---|---|
| `Ft10Client` | Entrypoint: registers Fabric events, commands, HUD element. |
| `Ft10` | Runtime singleton: config, stored scores, the current `Session`; turns game events into session calls. |
| `core/` | Pure Java scoring rules (no Minecraft imports, unit tested). |
| `mc/` | Minecraft → core translation: `Players` (tab list lookups), `DeathMessages`, `ServerKey`. |
| `feedback/` | `ScoreHud` (drawing), `Notifier` (title/chat/sound), `Texts` (house-style components). |
| `command/` | `FtCommand`: the `/ft10` and `/ft` trees (client commands). |
| `config/` | `Ft10Config` + `ConfigManager` (config/ft10.json), `ScoreStorage` (ft10/scores.json), YACL `ConfigScreen`, `ModMenuIntegration`. |
| `compat/` | Every call that differs between Minecraft versions. Stonecutter conditions belong here. |
| `mixin/` | `ClientPacketListenerMixin`: damage-event and player-combat-kill packets. |

## Signal flow
```
AttackEntityCallback (we swing)            ─┐
DamageEvent packet (A hurt B, mixin)        ├─> Ft10 ──> Session.onHit / onSwing      (who is fighting whom)
PlayerCombatKill packet (we died, mixin)    │          Session.onDeathMessage / onDeathSeen
GAME chat message (death messages)          │
client tick: other players' health hits 0  ─┘          Session.tick → KillDetector settles pending deaths
                                                                 └─> ScoreBook.award → Notifier (title/chat/sound)
ScoreHud reads Session.book().active() every frame.
```

## Kill detection (core)
- `CombatTracker` remembers the last player who hit each victim and when we last traded hits with someone.
- `KillDetector`: a death message (names the killer) settles a death at once. A death we only *see* waits
  `PENDING_MS` (1 s) for a message, then falls back to the hit-based guess. A settled victim is ignored for
  `DEDUPE_MS` (2.5 s), so packet + chat + health never count one death twice.
- Points only when we are the victim or the killer.
- Screen switching (`Session.engage`): the first hit with someone shows their score, unless we traded hits with
  the shown opponent within `switchLockSeconds`. A finished fight (either side died) releases the lock at once.
  `/ft lock` pins the screen; other fights still score.

## Storage
- `config/ft10.json` — `Ft10Config` fields (Gson). `sanitize()` clamps hand-edited values.
- `<game dir>/ft10/scores.json` — `{ version, servers: { <server key>: { <rival key>: {name, me, them, setsMe, setsThem, lastFought} } } }`.
  Server key = address without `:25565`, `singleplayer:<world>` or `realms:<name>`. Rival key = UUID, or
  `name:<lower-case name>` when no UUID was known. Saved 2 s after the last change and on disconnect/exit.

## Version boundaries to handle in `compat/` (Phase 5)
| Since | Change | Where |
|---|---|---|
| 1.21.5 | Hover/ClickEvent became records (`HoverEvent.ShowEntity`, `ClickEvent.RunCommand`) | `TextCompat` |
| 1.21.6 | `HudElementRegistry` replaces `HudRenderCallback`; text colors must be ARGB | `HudCompat` |
| 1.21.9 | authlib 7: `GameProfile` is a record (`id()`, `name()`) | `ProfileCompat` |
| 1.21.11 | `ResourceLocation` → `Identifier` (Stonecutter string replacement) | `stonecutter.gradle.kts` |
| 26.1 | Unobfuscated; Java 25; Loom without remapping (handled by loom-back-compat) | build |
| 26.x | `Gui.hud` holds HUD calls, `GuiGraphicsExtractor`, `EntityTypes.PLAYER`, `ClientCommands`, `Gui.setScreen` | `HudCompat`, `TextCompat`, `ClientCompat`, `FtCommand` imports |
