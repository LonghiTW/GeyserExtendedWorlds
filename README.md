# GeyserExtendedWorlds

GeyserExtendedWorlds is a Geyser extension that remaps Java Edition coordinates into Bedrock-safe coordinate windows. It is intended for sightseeing in worlds that exceed Bedrock client's practical coordinate and height limits.

This project is a fork of GeyserFloatingPoints by oryxel. Modified by LonghiTW in 2026 to support XYZ coordinate remapping, extended vertical world-height windows, high-world chunk remapping, and Bedrock prediction correction.

Licensed under the GNU General Public License v3.0. If you distribute binaries, make the corresponding source code for that build available under the same license.

## How this works
- X/Z remapping keeps Bedrock players near safer coordinate ranges to avoid far-coordinate precision issues.
- Y remapping projects a slice of an extended Java world into Bedrock's supported vertical chunk window.
- Chunk and block update packets are rewritten so Bedrock sees the remapped position while the Java server still receives real coordinates.

## Installation
- Download `geyserextendedworlds.jar` from the [release](https://github.com/LonghiTW/GeyserExtendedWorlds/releases) or build it yourself with `./gradlew build`.
- Place the jar in your Geyser extensions folder.
	- Standalone Geyser: `extensions/`
	- Plugin Geyser: `plugins/Geyser/extensions/`
- Restart Geyser or restart the server/proxy so the extension can load.
- After the first start, edit `extensions/GeyserExtendedWorlds/config.yml` if you need to adjust the coordinate remap settings.

## Configuration
- `capped-value`: Max X/Z distance before the player is remapped closer to the origin.
- `vertical-remap-threshold`: Distance from fake Y center `128` before switching the visible vertical window.
- `show-position-by-default`: Whether Bedrock players see their real Java coordinates in the actionbar by default.

## Commands
- `/geyserextendedworlds position` - Toggle on/off actionbar that shows your real position. Permission: `geyserextendedworlds.position`

## Drawbacks
- You won't be able to see your real position since your position is always spoofed.
- Some interactions may display differently on Bedrock until the server confirms the real block state.
- Bugs are bound to happen, feel free to report them.

## Attribution

- Original project: GeyserFloatingPoints by oryxel.
- Fork and modifications: GeyserExtendedWorlds by LonghiTW, 2026.
- License: GPLv3, see [LICENSE](https://github.com/LonghiTW/GeyserExtendedWorlds/blob/main/LICENSE).
