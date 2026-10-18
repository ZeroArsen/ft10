# Architecture

## Packages (`dev.zerorsen.ft10`)
| Package | Role |
|---|---|
| `Ft10Client` | Entrypoint: registers Fabric events, commands, HUD element. |
| `Ft10` | Runtime singleton: config, stored scores, the current `Session`; turns game events into session calls. |
| `core/` | Pure Java scoring rules (no Minecraft imports, unit tested). |
| `mc/` | Minecraft → core translation: `Players` (tab list lookups), `DeathMessages`, `Sidebar` (sidebar rows), `ServerKey`. |
| `feedback/` | `ScoreHud` (drawing), `Notifier` (title/chat/sound), `Texts` (house-style components), `Debug` (`/ft debug`). |
| `command/` | `FtCommand`: the `/ft10` and `/ft` trees (client commands). |
| `config/` | `Ft10Config` + `ConfigManager` (config/ft10.json), `ScoreStorage` (ft10/scores.json), YACL `ConfigScreen`, `ModMenuIntegration`. |
| `compat/` | Every call that differs between Minecraft versions. Stonecutter conditions belong here. |
| `mixin/` | `ClientPacketListenerMixin`: damage event, entity death event, player combat kill, action bar, title, subtitle and player-info-remove packets. |
| `Keybinds` | Six optional hotkeys; `feedback/HudEditorScreen` is the drag-and-drop HUD editor. |

## Signal flow
```
AttackEntityCallback (we swing)            ─┐
DamageEvent packet (A hurt B, mixin)        ├─> Ft10 ──> Session.onHit / onSwing      (who is fighting whom)
PlayerCombatKill packet (we died, mixin)    │          Session.onDeathMessage / onDeathSeen
EntityEvent 3 packet (a player died, mixin) │
GAME chat message (death/kill messages)     │          (mc/DeathMessages: vanilla death.* anywhere in the
ActionBar / Title / Subtitle packets (mixin)│           component tree, else DeathMessageParser patterns;
                                            │           action bars and titles pass RepeatGuard first)
client tick: other players' health hits 0   │
every 5 ticks: sidebar "Kills"/"Deaths"     │   → StatWatcher → Session.onKillCounted / onDeathCounted
PlayerInfoRemove packet (mixin)            ─┘   → Session.onPlayerLeft (only with countCombatLogs)
                                                       Session.tick → KillDetector settles pending deaths
                                                                 └─> ScoreBook.award → Notifier (title/chat/sound)
ScoreHud reads Session.book().active() every frame. feedback/Debug explains each signal when /ft debug is on.
```

## Kill detection (core)
- `CombatTracker` remembers the last player who hit each victim and when we last traded hits with someone.
- `KillDetector`: a death message (names the killer) settles a death at once. A death we only *see* waits
  `PENDING_MS` (1 s) for a message, then falls back to the hit-based guess. A settled victim is ignored for
  `DEDUPE_MS` (2.5 s), so packet + chat + health never count one death twice.
- A **weak** pending death (from a sidebar kill counter: the victim is guessed as the player we last hit or swung
  at) is dropped when its killer was credited with any other kill within `DEDUPE_MS` (D17).
- `DeathMessageParser`: presets (second-person "You killed X" / "X killed you" first), custom templates
  (`{victim}`/`{killer}`, `*`) or regexes. No victim group = the player themself died; no killer group = the hit
  history decides. **Loose** presets ("X was <words> by Y") count only against a current opponent.
- `mc/DeathMessages` resolves names: show_entity hovers in the message, tab list profile names, our own name, tab
  list / nametag display names, and finally a recent opponent by name (someone who already left).
- Points only when we are the victim or the killer.
- Screen switching (`Session.engage`): the first hit with someone shows their score, unless we traded hits with
  the shown opponent within `switchLockSeconds`. A finished fight (either side died) releases the lock at once.
  `/ft lock` pins the screen; other fights still score.

## Storage
- `config/ft10.json` — `Ft10Config` fields (Gson). `sanitize()` clamps hand-edited values.
- `<game dir>/ft10/scores.json` — `{ version, servers: { <server key>: { <rival key>: {name, me, them, setsMe, setsThem, lastFought} } } }`.
  Server key = address without `:25565`, `singleplayer:<world>` or `realms:<name>`. Rival key = UUID, or
  `name:<lower-case name>` when no UUID was known. Saved 2 s after the last change and on disconnect/exit.

## Version boundaries (`compat/`)
Ten nodes, one jar per protocol family: 1.21.1 (1.21–1.21.1), 1.21.3 (1.21.2–1.21.3), 1.21.4, 1.21.5, 1.21.8
(1.21.6–1.21.8), 1.21.10 (1.21.9–1.21.10), 1.21.11, 26.1.2 (26.1–26.1.2), 26.2, 26.3 (D22). Conditions are written
`//? if >=26.2 { … //?} else { /*…*///?}`; the source in `src/` is always 26.2's side (`vcsVersion`). Every row
below was checked with javap against that version's jar (2026-10-18), and each node also compiles against the
lowest patch of its range. Don't put `//` comments inside a commented-out branch: Stonecutter strips `//` when it
turns the branch on.

| Since | Change | Where |
|---|---|---|
| 1.21.5 | Hover/ClickEvent became records (`HoverEvent.ShowEntity`, `ClickEvent.RunCommand`); before: `getValue(Action.SHOW_ENTITY)`, `EntityTooltipInfo.id` (now `uuid`), `new ClickEvent(Action, String)` | `TextCompat`, `DeathMessagesTest` |
| 1.21.6 | `HudElementRegistry` replaces `HudRenderCallback` (before: drawn on top of the whole HUD); GUI pose is a 2D matrix stack (`pushMatrix`, `translate(x, y)`), `PoseStack` before (`pushPose`, `translate(x, y, 0)`); text colors must be ARGB | `HudCompat` |
| 1.21.9 | authlib 7: `GameProfile` is a record (`id()`, `name()`; `getId()`/`getName()` before) | `ProfileCompat` |
| 1.21.9 | Mouse/key events became records (`MouseButtonEvent`, `KeyEvent`; `(double, double, int)` / `(int, int, int)` before) | `CompatScreen` |
| 1.21.9 | `KeyMapping.Category` record (a translation key string before) | `KeyCompat` |
| – | The outline method was `renderOutline`, `submitOutline` (1.21.9–1.21.10), then `outline` (26.x): drawn with four `fill`s instead | `HudCompat` |
| 26.3 | `InputConstants.Type.KEYSYM` → `KEYBOARD`: avoided with the `KeyMapping(String, int, category)` constructor | `KeyCompat` |
| 1.21.11 | `ResourceLocation` → `Identifier` (Stonecutter string replacement) | `stonecutter.gradle.kts` |
| 26.1 | Unobfuscated; Java 25; Loom without remapping (handled by loom-back-compat) | build |
| 26.1 | fabric-key-mapping-api (`KeyMappingHelper`) | `KeyCompat` |
| 26.1 | `GuiGraphicsExtractor` (`GuiGraphics` before): `text` was `drawString`; screens override `extractRenderState`/`extractBackground` (`render`/`renderBackground` before) | `HudCompat`, `CompatScreen` |
| 26.1 | Chat `addClientSystemMessage` (`addMessage` before) | `HudCompat` |
| 26.1 | Fabric `ClientCommands` (`ClientCommandManager` before) | `CommandCompat` |
| 26.2 | `Gui.hud` holds title/action bar/chat (`Gui` itself before); F1 is `Hud.isHidden()` (`options.hideGui` before) | `HudCompat` |
| 26.2 | `EntityTypes.PLAYER` (`EntityType.PLAYER` before) | `TextCompat`, `DeathMessagesTest` |
| 26.2 | `Gui.setScreen` / `Gui.screen()` (`Minecraft.setScreen` / `Minecraft.screen` before) | `ClientCompat` |
| ≤1.21.11 | Remapping Loom drops YACL's nested libraries in dev: `localRuntime` quilt-parsers + imageio-webp | `build.gradle.kts` |

The eight `ClientPacketListener` methods the mixin targets exist under the same names in every node.
