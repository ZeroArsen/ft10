# FT10 — PvP Score Counter

A client-side **PvP score tracker for FFA and duels**. FT10 remembers who you are fighting and keeps a
**first-to score** against every opponent, shown right above your action bar: `4 - 7`, your kills in green,
theirs in red. Fight someone else and the score switches to them; go back to the first player and you
continue where you left off.

![Score HUD above a server's action bar](https://raw.githubusercontent.com/ZeroArsen/ft10/main/docs/images/hud.png)

## Features
- **A score per opponent.** Start trading hits with someone and their score shows up. Scores are kept per
  player and per server, and saved between sessions.
- **Duel-server style feedback.** Every kill or death shows a title, a chat line with an **[Undo]** button
  and a sound.
- **First to N.** `/ft goal 10` plays an FT10: reaching the goal shows **VICTORY** or **DEFEAT**, starts a
  new set and counts sets won.
- **Never fights the server's action bar.** The score is its own HUD element, so combat timers and other
  action bar messages stay readable.
- **Works on vanilla and plugin servers.** Reads vanilla death messages, damage and death events, and common
  kill-message formats; add your server's format as a regex if it is unusual.
- **Customizable.** Settings through [Mod Menu](https://modrinth.com/mod/modmenu) or `/ft config`: colors,
  size, title, chat, sound, detection. A drag-and-drop HUD editor (`/ft hud`) and optional hotkeys.
- **English and Turkish.**

![VICTORY title when the first-to goal is reached](https://raw.githubusercontent.com/ZeroArsen/ft10/main/docs/images/victory.png)

## Commands
`/ft10` always works; `/ft` is a shorter alias you can turn off if a server has its own `/ft`.

| Command | What it does |
|---|---|
| `/ft` | Current opponent and score |
| `/ft on` · `off` · `toggle` | Turn tracking on or off |
| `/ft reset` · `reset <player>` · `reset all` | Reset scores |
| `/ft set <me> <them>` | Fix the score by hand |
| `/ft undo` | Take back the last point |
| `/ft goal <n>` · `goal off` | Play first to N |
| `/ft lock <player>` · `unlock` | Keep one opponent on screen |
| `/ft list` | Every score on this server |
| `/ft hud` | Move and resize the score |
| `/ft config` | Open the settings |

## Fair play
FT10 only shows information your client already receives. It sends nothing to the server, never chats for
you and automates nothing — it is a scoreboard, not a hack.

## For server owners
A plugin that already knows who killed whom can report it to FT10 over a tiny plugin channel. Players
with the mod then get exact scores (fake deaths and combat logs included); players without it notice nothing.
See the [integration guide](https://github.com/ZeroArsen/ft10/blob/main/docs/SERVER_INTEGRATION.md).

## Requirements
[Fabric API](https://modrinth.com/mod/fabric-api) and [YACL](https://modrinth.com/mod/yacl).
[Mod Menu](https://modrinth.com/mod/modmenu) is optional. Client only — do not install on a server.

## Links
[Source code](https://github.com/ZeroArsen/ft10) ·
[Report a bug](https://github.com/ZeroArsen/ft10/issues) ·
License: GPL-3.0-only · Made by 0Arsen
