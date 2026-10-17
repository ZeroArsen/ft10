# Decisions (append-only)

**D1 — Name and identity.** FT10 — PvP Score Counter; Modrinth slug, mod id and data folder `ft10`; commands
`/ft10` (always) and `/ft` (optional alias). "ft10" had zero Modrinth search hits on 2026-10-17, the closest
competitors were MatchHUD and Kill Counter. Package `dev.zerorsen.ft10`.

**D2 — Opponent switching.** The screen follows the first hit exchange with someone (owner's choice), with a
5 s lock while still fighting the shown opponent so a stray third-party hit doesn't steal it. Kills always
count for the right rivalry even when the screen doesn't switch (`/ft lock`).

**D3 — Persistence.** Scores are saved per server to `<game dir>/ft10/scores.json` (owner's choice), separate
from settings in `config/ft10.json` so a config reset never wipes scores.

**D4 — Display.** Our own HUD element just above the vanilla action bar instead of the real action bar (owner's
choice): FFA servers use the action bar constantly (combat timers) and would overwrite or flicker with it.

**D5 — Chat feedback is local only.** Never send chat or commands to the server: automated chat breaks most
server rules and the mod must stay a pure information display.

**D6 — Kill detection by combining signals.** Death messages (vanilla translatable with show_entity UUIDs, plus
regex for plugin formats), damage-event attribution and health-based death detection, merged by
`KillDetector` with a 1 s wait for a message and a 2.5 s dedupe window.

**D7 — Build stack.** Stonecutter 0.9.8 + loom-back-compat (the official Stonecutter Fabric template) with
Mojang mappings on every version, so one source tree compiles for obfuscated 1.21.x and unobfuscated 26.x.

**D8 — YACL required, Mod Menu optional.** YACL exists for every target version (1.21 → 26.4) and the Modrinth
app installs required dependencies automatically; Mod Menu only adds the settings button.

**D9 — Dependencies come from Modrinth Maven** (`maven.modrinth:yacl`, `maven.modrinth:modmenu`): one
repository, version numbers identical to the Modrinth pages.

**D10 — Author name `0Arsen`.** The owner's in-game and Modrinth name; GitHub is `ZeroArsen` only because
`0Arsen` was taken there. Git commits keep `0Arsen` with the GitHub noreply address.

**D11 — Instant-respawn deaths come from the entity death event.** The 2026-10-17 two-player test showed that
with instant respawn other clients never see the victim's health reach 0. `ServerPlayer.die` broadcasts entity
event 3, which arrives in time; the health scan stays as a fallback.

**D12 — Server integration protocol v1** (`docs/SERVER_INTEGRATION.md`). Plugin channels `ft10:hello` and
`ft10:kill`; after hello, FT10 trusts only the server's kill reports on that connection. Fields are only ever
appended; decoders skip unknown bytes and never throw, so a buggy or newer plugin can't kick players. Hello
must be sent when the client registers `ft10:hello` itself: Paper silently drops messages on channels the client
hasn't registered yet, and Fabric registers `ft10:kill` first (found in the Paper 26.2 test).

**D13 — Version guards are explicit `//? if` blocks in `compat/`, not Stonecutter string replacements.** A
replacement is applied backwards when switching to the other version, so `EntityTypes` → `EntityType` would also
turn the real `EntityType<?>` into `EntityTypes<?>` on the way back. Only `ResourceLocation` ↔ `Identifier` stays a
replacement. The boundary is `>=26.1` (as in the owner's Levy mod); FtCommand's builders moved into
`CommandCompat` so no file outside `compat/` has a guard (the test's `EntityTypes` holder is the one exception).

**D14 — 1.21.x dev runs add YACL's nested libraries by hand.** Remapping Loom strips the `jars` list of remapped
mods and Modrinth's POM lists no dependencies, so the 1.21.11 dev client crashed on
`org.quiltmc.parsers.json.gson.GsonWriter`. `localRuntime` quilt-parsers json/gson 0.2.1 and imageio-webp 3.12.0
(Quilt maven, restricted to `org.quiltmc.parsers`) for `<26.1` only. The published jar is unchanged: players'
YACL carries the libraries itself.

**D15 — Each non-main version runs in `run/<version>/`.** 26.2 keeps `run/` (and `run/opponent`, `run/server`) so
existing test notes stay valid; 1.21.11 uses `run/1.21.11/…`, so an older Minecraft never opens a world or options
file written by a newer one.
