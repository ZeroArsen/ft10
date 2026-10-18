# FT10 — PvP Score Counter

Keeps score against whoever you fight on FFA and duel servers. Your score shows above the action bar as
`4 - 7`: yours in green, theirs in red. Each opponent has their own score, so switching fights and coming back
picks up where you left off.

## Features
- A separate score for every opponent, saved per server
- A title, a chat line and a sound for every kill and death
- First-to games: `/ft goal 10` plays an FT10 and counts the sets you win
- Works on vanilla and plugin servers, with nothing to install on the server
- Your head and your opponent's next to the score (can be turned off)
- Move and resize the score with `/ft hud`; colors and everything else in the settings

## Commands
| Command | What it does |
|---|---|
| `/ft` | Show the current score |
| `/ft goal <n>` | Play first to N |
| `/ft undo` | Take back the last point |
| `/ft reset` | Reset the current opponent |
| `/ft hud` | Move and resize the score |
| `/ft help` | All commands |

## Requirements
Minecraft 1.21 – 26.3 on Fabric, with [Fabric API](https://modrinth.com/mod/fabric-api) and [YACL](https://modrinth.com/mod/yacl).
[Mod Menu](https://modrinth.com/mod/modmenu) is optional. Client side only.

FT10 sends nothing to the server and plays nothing for you: it only counts.

[Source code](https://github.com/ZeroArsen/ft10) · [Report a bug](https://github.com/ZeroArsen/ft10/issues)
