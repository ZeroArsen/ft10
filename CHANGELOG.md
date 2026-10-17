# Changelog

## 0.1.0 — unreleased
First version, Minecraft 26.2.

- Per-opponent first-to scores, shown above the action bar.
- Kills are also seen through the entity death event, so instant-respawn servers count correctly.
- Kill detection from vanilla death messages, kill-message plugins (built-in and custom regex) and recent hits.
- Title, local chat line (with Undo) and sound for every point; VICTORY / DEFEAT when a first-to goal is reached.
- `/ft` commands: status, on/off/toggle, reset, set, undo, goal, lock/unlock, list, config.
- Settings screen (YACL) with Mod Menu integration.
- HUD editor (`/ft hud`, settings button or hotkey): drag to move, scroll to resize, arrow keys to nudge.
- Optional hotkeys (unbound by default): toggle FT10, toggle HUD, undo, reset opponent, settings, HUD editor.
- Scores saved per server.
- English and Turkish translations.
