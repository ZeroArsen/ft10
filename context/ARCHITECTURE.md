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
| `mixin/` | `ClientPacketListenerMixin`: damage-event, entity-death-event and player-combat-kill packets. |
| `net/` | `Ft10Protocol`: `ft10:hello` / `ft10:kill` payloads for server integration (`docs/SERVER_INTEGRATION.md`). |
| `Keybinds` | Six optional hotkeys; `feedback/HudEditorScreen` is the drag-and-drop HUD editor. |

## Signal flow
```
AttackEntityCallback (we swing)            ─┐
DamageEvent packet (A hurt B, mixin)        ├─> Ft10 ──> Session.onHit / onSwing      (who is fighting whom)
PlayerCombatKill packet (we died, mixin)    │          Session.onDeathMessage / onDeathSeen
EntityEvent 3 packet (a player died, mixin) │
ft10:kill payload (server says who)         │   (after ft10:hello only this one scores; the guesses above are off)
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
Built so far: **1.21.11** and **26.2**. Conditions are written `//? if >=26.1 { … //?} else { /*…*///?}`; the source
in `src/` is always 26.2's side (`vcsVersion`). The 26.x rows below were checked against the 1.21.11 and 26.2
jars only: whether each change landed in 26.1 or 26.2 is unverified until a 26.1.2 node exists. Don't put `//`
comments inside a commented-out branch: Stonecutter strips `//` when it turns the branch on.

| Since | Change | Where |
|---|---|---|
| 1.21.5 | Hover/ClickEvent became records (`HoverEvent.ShowEntity`, `ClickEvent.RunCommand`) | `TextCompat` |
| 1.21.6 | `HudElementRegistry` replaces `HudRenderCallback`; text colors must be ARGB | `HudCompat` |
| 1.21.9 | authlib 7: `GameProfile` is a record (`id()`, `name()`) | `ProfileCompat` |
| 1.21.9 | Mouse/key events became records; `KeyMapping.Category` record | `CompatScreen`, `KeyCompat` |
| 26.x | `PayloadTypeRegistry.playS2C()` → `clientboundPlay()`; fabric-key-mapping-api | `NetCompat`, `KeyCompat` |
| 1.21.11 | `ResourceLocation` → `Identifier` (Stonecutter string replacement) | `stonecutter.gradle.kts` |
| 26.1 | Unobfuscated; Java 25; Loom without remapping (handled by loom-back-compat) | build |
| 26.x | `Gui.hud` holds title/action bar/chat (`Gui` itself before); F1 is `Hud.isHidden()` (`options.hideGui` before); chat `addClientSystemMessage` (`addMessage` before) | `HudCompat` |
| 26.x | `GuiGraphicsExtractor` (`GuiGraphics` before): `text`/`outline` were `drawString`/`renderOutline`; screens override `extractRenderState`/`extractBackground` (`render`/`renderBackground` before) | `HudCompat`, `CompatScreen` |
| 26.x | `EntityTypes.PLAYER` (`EntityType.PLAYER` before) | `TextCompat`, `DeathMessagesTest` |
| 26.x | Fabric `ClientCommands` (`ClientCommandManager` before) | `CommandCompat` |
| 26.x | `Gui.setScreen` / `Gui.screen()` (`Minecraft.setScreen` / `Minecraft.screen` before) | `ClientCompat` |
| ≤1.21.11 | Remapping Loom drops YACL's nested libraries in dev: `localRuntime` quilt-parsers + imageio-webp | `build.gradle.kts` |
