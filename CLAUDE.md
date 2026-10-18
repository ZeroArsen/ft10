# CLAUDE.md — FT10 bootstrap

**FT10 — PvP Score Counter**: a client-only Fabric mod (mod id `ft10`, commands `/ft10` + `/ft`) that keeps a
first-to score against each opponent on FFA/duel servers. Main dev version **26.2**; target range
**1.21 → 26.3** through Stonecutter. Published on Modrinth (slug `ft10`) and GitHub, **GPL-3.0-only**.

The owner writes in Turkish; answer in Turkish, keep code and identifiers in English.

## Session ritual
**On start:** read this file → `context/PROGRESS.md` (where we are, next task) → `context/ARCHITECTURE.md` as
needed.
**On task complete:** update `context/PROGRESS.md`; append to `context/DECISIONS.md` when something was
decided; add manual steps to `context/TESTING.md`; keep `CHANGELOG.md` current.

## Prime rules
1. **`core/` has no Minecraft imports.** All scoring rules live there and are unit tested (`./gradlew test`).
2. **Version differences live in `compat/`.** Stonecutter `//?` conditions go there, not across the code base.
3. **Nothing is ever sent to the server.** Chat feedback is local (`HudCompat.chat`); no automation.
4. **User-facing text goes through lang keys** (`assets/ft10/lang/en_us.json` + `tr_tr.json`) in the owner's
   house style (`C:\dev\mc-plugins\RevoltPvP\context\MESSAGE_STYLE.md`): white values in `#919191` text,
   `#04E525` success, `#E50404` error, dark-gray framing, ALL-CAPS bold titles, `ꜰᴛ |` tag.
5. **Verify Minecraft APIs against the real jar** (javap on the Loom cache) before using them; 26.x renamed a
   lot (`Gui.hud`, `GuiGraphicsExtractor`, `EntityTypes`, `ClientCommands`, `Gui.setScreen`).

## Build facts (verified 2026-10-17)
- Stonecutter 0.9.8 + loom-back-compat 0.4.2, Loom 1.18-SNAPSHOT, Gradle 9.8.0, Fabric Loader 0.19.5.
- Per-node versions (Fabric API, YACL, Mod Menu, `mod.mc_compat`) live in `stonecutter.properties.toml`.
- JDK 25 builds everything; 1.21.x nodes target Java 21, 26.x nodes Java 25.
- Nodes (one jar per protocol family, D22): `1.21.1`, `1.21.3`, `1.21.4`, `1.21.5`, `1.21.8`, `1.21.10`, `1.21.11`,
  `26.1.2`, `26.2` (`vcsVersion`, the side of every `//? if` that is uncommented in `src/`), `26.3`.
- Run configs: `:26.2:runClient` (Ft10Dev), `:26.2:runOpponent` (Ft10Rival, own run dir), `:26.2:runServer`
  (`run/server`); the same tasks on any other node (`:1.21.1:`) run in `run/<version>/`. `-Pft10.join=localhost` joins the dev
  server, `-Pft10.debug` starts with `/ft debug` on.

## Releasing (same flow as rGoldenHeads)
1. Bump `mod.version` (and `mod.version_type`) in `stonecutter.properties.toml`; add a `## <version>` section on top
   of `CHANGELOG.md` (only the newest section goes to Modrinth). `MODRINTH.md` is the Modrinth page body.
2. Commit, then `git tag v<version>` and push the tag. `.github/workflows/release.yml` builds every node,
   makes a GitHub Release and runs `gradlew modrinth` (one Modrinth version per node) with the `MODRINTH_TOKEN`
   repo secret. Check a release locally with `gradlew modrinth -Pft10.modrinthDryRun --no-configuration-cache`.

## Context files
- `context/PROGRESS.md` — phase checklist and next task. **Read first.**
- `context/ARCHITECTURE.md` — packages, signal flow, kill detection, version boundaries.
- `context/DECISIONS.md` — append-only decisions (D1, D2, …).
- `context/TESTING.md` — manual in-game test checklist.
