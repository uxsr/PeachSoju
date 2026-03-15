# 🍑 PeachSoju

A Fabric mod for Minecraft 1.21.x focused on Hypixel Skyblock dungeon automation and quality-of-life features.

## Features

### Dungeon Automation
- **AutoRoutes & Burst Mode** — Waypoint-based etherwarp chaining with multiple node types (ETHER, AOTV, HYPE, LOOK, WALK, STOP, ALIGN)
- **AutoIceFill** — Automated Ice Fill puzzle solver using AOTV teleportation
- **StormBowTimer** — Server-tick-accurate bow release timer for Last Breath
- **SecretAura Integration** — Packet-based secret clicking for chests, levers, and skulls

### Visual Enhancements
- **FMBlocks** — Custom ghost block placement with highlight rendering and etherwarp activation
- **NickHider** — Custom nickname and rank display with cross-client sync
- **Item Customization** — Custom names, enchant glint, leather dye, and item model replacement

### Utilities
- **AutoWeirdos** — Automated Weirdos puzzle solver
- **AutoSS** — Sea lantern block update detection
- **ChatBypass** — Unicode-based chat filter bypass

## Requirements

- Minecraft 1.21.x
- Fabric Loader
- Fabric API
- [Odin](https://github.com/odtheking/Odin) (dependency)

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/installer/)
2. Download the latest release from [Releases](https://github.com/uxsr/PeachSoju/releases)
3. Place the `.jar` file in your `mods` folder
4. Ensure Odin and Fabric API are also installed

## Building from Source

```bash
git clone https://github.com/uxsr/PeachSoju.git
cd PeachSoju
./gradlew build
```

The built jar will be in `build/libs/`.

## Configuration

In-game GUI available via keybind (default configured in mod settings).

## License

BSD-3-Clause — See [LICENSE.txt](LICENSE.txt)

## Disclaimer

This mod is intended for educational purposes. Use at your own risk on servers with rules against automation.