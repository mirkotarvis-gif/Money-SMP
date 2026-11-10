# MoneySMP

Paper 1.21.4+ plugin for Money SMP. Java 21.

## Build

Upload this folder's contents to a GitHub repo so `pom.xml` is at the top level, not inside a
subfolder. The workflow in `.github/workflows/build.yml` builds the jar on every push. Open
**Actions**, click the latest run, and download **MoneySMP** from Artifacts.

Building locally needs JDK 21 and Maven: `mvn package` writes `target/MoneySMP-1.1.0.jar`.

## Resource pack

`resourcepack/MoneySMP-ResourcePack.zip` adds the icons shown in tab and chat: crown for leaders,
S–F tier badges, 6 team shields, ADMIN / STAFF badges and a coin. Without it those icons show as boxes.

To push it to everyone, host the zip (e.g. a GitHub release asset or mc-packs.net) and set this in `server.properties`:

```
resource-pack=<direct download link>
resource-pack-sha1=<sha1 in resourcepack/SHA1.txt>
require-resource-pack=true
```

Not using the pack? Replace the `\uE0xx` icons in `config.yml` with text, e.g. `admin-icon: "<red><bold>ADMIN</bold></red>"`.

Team shields: `/team icon <team> <red|blue|green|yellow|purple|orange>`.

## Commands

### Money and teams
| Command | What it does |
|---|---|
| `/assassin set [player]` | Sets you (or a player) as an assassin for Disasters events. `/assassin remove`, `/assassin list` |
| `/money view <player\|team>` | Shows how much money a player or whole team has |
| `/money set\|add\|remove <player> <amount>` | Changes a player's money |
| `/teamleader set <player> <team>` | Makes a player the leader (crown icon). Creates the team if it doesn't exist |
| `/teamreset` | Removes every player from every team |
| `/tier set <player> <tier>` | Sets a player's tier |
| `/balance`, `/pay`, `/baltop`, `/team ...` | Everyday money / team commands |

### Ranks and chat
| Command | What it does |
|---|---|
| `/admin set\|remove <player>` | Admin icon next to their name in tab and chat |
| `/staff set\|remove <player>` | Staff icon next to their name |
| `/broadcast <message>` | Message to everyone (MiniMessage colours work) |

### Start setup
| Command | What it does |
|---|---|
| `/moneystart [seconds]` | Starts Money SMP: title + grace period |
| `/bidding time` | Starts a bidding session (every team gets `start-budget`) |
| `/bidopen <tier>` | Opens bidding on one tier; its players go up one at a time |
| `/bidclose` | Sells the current player to the highest bid and closes the tier |
| `/bidding stop` | Cancels the whole bidding session |
| `/bid <amount>` | Team leaders bid on the current player |

### Events hosting
| Command | What it does |
|---|---|
| `/event start\|stop <name>` | Starts / stops a small event |
| `/event random` | Spins a roulette and starts a random event |
| `/eventmain <event> setmap` | Sets that event's map to where you're standing |
| `/eventmain <event> start` | Saves everyone's stuff, teleports them to the map, starts the event |
| `/eventmain <event> stop` | Stops the event (pays placements) and gives everyone their stuff back |

Events: `capturepoints`, `titans`, `egghunt`, `acehunt`, `disasters`, `gladiators`.
Saved inventories survive a crash. Offline players get theirs back when they rejoin.

### Gladiators kits
| Command | What it does |
|---|---|
| `/eventmain gladiators kit set <name>` | Saves your inventory as a kit |
| `/eventmain gladiators kit edit <name>` | Opens a GUI to edit it (close to save) |
| `/eventmain gladiators kit remove <name>` | Deletes a kit |
| `/eventmain gladiators kit list` | Shows all kits |
| `/eventmain gladiators kit give <name> [player\|all]` | Gives a kit |

### Capture points
| Command | What it does |
|---|---|
| `/eventmain point set <name>` | Creates a capture point where you stand |
| `/eventmain point remove <name>` | Deletes it |
| `/eventmain point list` | Lists every capture point |

### Buildings
Spawned in front of you and turned to face the way you're looking.

| Command | Build |
|---|---|
| `/eventmain build lobby` | 49-block round spawn plaza with money fountain, light pillars and "MONEY SMP" sign (centred on you) |
| `/eventmain build biddinghall` | Hall with stage, auction podium, big screen, 6 captain booths and spectator stands |
| `/eventmain build gladiators` | Round sand arena with walls, stepped stands and 2 gates. Sets the gladiators map automatically |
| `/eventmain build capturepoint [name]` | `zone-size` platform with beacon and hologram. Registers the capture point |
| `/eventmain build podium` | 1st / 2nd / 3rd winners podium |
| `/eventmain build undo` | Removes your last build and puts back what was there |

## Minigames status
Each game has its config loaded, countdown, timer, team scores and placement rewards.
The actual gameplay for capture points, titans abilities, egg hunt, ace hunt, disasters and gladiators fights
is still to be written (see the TODO at the top of each file in `src/main/java/net/moneysmp/games/`).
Until then: `/game <id> score <team> <points>` and `/eventmain <id> stop` to pay out.

## Permission
`moneysmp.admin` (op by default) for every admin command.
