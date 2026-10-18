# Changelog

## 0.4.0 — 2026-10-18 (beta)
Player heads and quicker resizing.

- Your head is shown left of your (green) score and your opponent's right of theirs (red), with their real skins.
  Turn it off under Settings → Display → "Show player heads" or with the new button in the HUD editor.
- The HUD editor (`/ft hud`) has − and + buttons with the current size between them; the + and − keys and the
  mouse wheel resize too. Cancel also undoes the size and heads changes.

## 0.3.0 — 2026-10-18 (beta)
Now for every Minecraft version from **1.21 to 26.3**.

- New jars: 1.21–1.21.1, 1.21.2–1.21.3, 1.21.4, 1.21.5, 1.21.6–1.21.8, 1.21.9–1.21.10, 26.1–26.1.2 and 26.3, next
  to 1.21.11 and 26.2. Download the one for your version; features are the same everywhere.
- On 1.21.5 and older the score is drawn on top of the whole HUD (newer versions draw it right after the action bar).

## 0.2.1 — 2026-10-18 (beta)
Kills on duel servers that never really kill the loser.

- Duel round messages that only name the winner ("Steve won the round 1 - 0") now count: a point for you when you
  won, for your opponent when they did. Watching someone else's duel adds nothing.
- "Round Won" and "You Died" titles count too.
- A player switching to spectator mode right after a fight with you counts as a death, for servers that fake
  deaths that way.
- Custom patterns accept `{winner}` for such messages: `{winner} won the round *`.
- The HUD shows just the opponent's name, without "vs".
- Settings → General has "Reset scores on this server" and "Reset all scores" (with a confirmation).

## 0.2.0 — 2026-10-17 (beta)
Now for Minecraft **1.21.11** as well as 26.2, with much better kill detection.

- Kill messages are read from chat, the action bar and titles, including "You killed X", "X killed you",
  "You were slain by X", "KILLED X" and "X was <anything> by Y" (the last only against the player you're fighting).
- Vanilla death messages are found even when a server wraps them in its own prefix.
- Names are matched through hover info, nicknames and display names, and kills still count when the opponent has
  already left.
- Sidebar kill and death counters ("Kills: 12") are used on servers that respawn players without a real death.
- Custom kill formats can be written as simple templates: `{victim} was killed by {killer}`.
- New option: count an opponent who leaves mid-fight as your kill (off by default).
- `/ft debug` explains every kill signal in chat and lists messages FT10 didn't recognize, with a Copy button;
  `/ft debug last` shows the last ten.
- Removed the server integration (`ft10:hello` / `ft10:kill`): FT10 needs nothing on the server.

## 0.1.0 — 2026-10-17 (beta)
First version, Minecraft 26.2.

- Per-opponent first-to scores, shown above the action bar.
- Kills are also seen through the entity death event, so instant-respawn servers count correctly.
- Kill detection from vanilla death messages, kill-message plugins (built-in and custom regex) and recent hits.
- Title, local chat line (with Undo) and sound for every point; VICTORY / DEFEAT when a first-to goal is reached.
- `/ft` commands: status, on/off/toggle, reset, set, undo, goal, lock/unlock, list, config.
- Settings screen (YACL) with Mod Menu integration.
- HUD editor (`/ft hud`, settings button or hotkey): drag to move, scroll to resize, arrow keys to nudge.
- Optional hotkeys (unbound by default): toggle FT10, toggle HUD, undo, reset opponent, settings, HUD editor.
- Server integration (`ft10:hello` / `ft10:kill`, docs/SERVER_INTEGRATION.md): servers that report kills get exact
  scores, including fake deaths and combat logs; FT10 stops guessing on those servers.
- Scores saved per server.
- English and Turkish translations.
