# Mingeriacc

A 2D Mine clone of a clone, AI generated in Java for my personal amusement.

<img width="2560" height="1392" alt="image_v03" src="https://github.com/user-attachments/assets/ddd5163d-d4b0-413a-96b9-ec4016a983d0" />

## How to play

**Windows**
Use the .exe file from [Releases](../../releases) OR...

1. Install the latest Java runtime version (JRE).
   version 26 should be fine from Oracle, Adoptium or whoever you like to distribute your java runtime:
   ```
   winget install EclipseAdoptium.Temurin.26.JRE
   ```
   (or download an installer from [adoptium.net](https://adoptium.net))
2. Download `Mingeriacc.jar` from the [Releases](../../releases) page.
3. Double-click `Mingeriacc.jar`


## Controls

In the help menu in game.

## Getting started

- Punch a tree and create a nice wooden house or wall yourself in with shitty dirt blocks.
- Make a work bench (10 wood, press E), then a furnace and smelt some ore. An anvil needs 5 iron bars.
- Slimes drop gel for torches. At night the zombies and demon eyes come out.
- Build a house with walls, a door, a torch, a table and a chair: the Guide moves in, and later the Merchant and the Nurse.


## Saves

- Worlds: `%APPDATA%\Mingeriacc\worlds` (Windows) or `~/.mingeriacc/worlds` (macOS/Linux)
- Settings: `%APPDATA%\Mingeriacc\settings.properties` / `~/.mingeriacc/settings.properties`

If the game crashes, details are written to `error.log` in the same folder.
Worlds from older versions load in newer versions.


## Features

- **Basic features**: Mining, building, crafting at a work bench, furnace and anvil, inventory with trash slot,
  coloured smooth lighting (or retro lighting in the settings), day-night cycle, saving (progress, multiple worlds, settings).

- **World generation**: hills and mountains, dirt and stone layers, caves and tunnels, cave entrances, deserts, beaches,
  clay, copper/iron/silver/gold ores, forests, tall grass, flowers and mushrooms, life crystals and pots in caves.
  Three world sizes and a seed.

- **Combat**: swords, a shortsword and bows from wood to molten, flaming arrows, poison and fire, life and healing potions, death and respawning (also at your bed).
  Enemies: slimes (many colours, and the rare Pinky), zombies, demon eyes, cave bats, skeletons, jungle slimes, hornets,
  eaters of souls, lava slimes, fire imps, demons and hellbats. Bunnies hop around the forest.

- **Biomes**: forest, desert, oceans, jungle (mud, mahogany trees, vines, glowing spores), corruption (ebonstone,
  chasms, demonite), and the underworld (ash, lava lakes, hellstone, ruined houses).

- **Water and lava**: flowing liquids, swimming, breath, lava burns, obsidian where water meets lava, buckets.

- **Town and gear**: town folk (Guide, Merchant with a shop, Nurse), doors, tables, chairs, beds, chests with loot
  in caves and cabins; armour sets (copper to molten, mining helmet) and accessories (Hermes Boots, Cloud in a Bottle...).

- **Graphics**: pixel art with smooth sky gradients, parallax forests, dunes, jungles, corruption and the underworld, soft clouds, coloured torchlight.
  Zoom setting (near / normal / far).

- **Audio**: calm background music (guitar, piano, pads, flute, bells) for the title, day, night, caves, jungle, corruption and underworld,
  and natural sound effects. Music and sound effects synthesized in code, no audio files.

## Roadmap

- **0.4+** — Bosses (each in its own version), magic and mana, more events and biomes.
- **0.5** — First multiplayer prototype


## Building from source

This is a plain Java project with **no external dependencies** — only the JDK is needed.

```bash
# Windows
build.bat

# macOS / Linux
./build.sh
```

Both scripts compile everything in `src/mingeriacc` and package it into `Mingeriacc.jar` in the project root. You'll need a JDK (not JRE):
```
winget install EclipseAdoptium.Temurin.26.JDK
```


## Creating an exe file (bundled java runtime for those without java installed)
**Option 1:**
`make_exe.bat`

**Option 2: use the following command (Remember to change version number):**

Remove-Item -Recurse -Force exe-tmp, Mingeriacc-exe -ErrorAction SilentlyContinue; mkdir exe-tmp | Out-Null; Copy-Item Mingeriacc.jar exe-tmp\; jpackage --type app-image --name Mingeriacc --app-version 0.3.0 --input exe-tmp --main-jar Mingeriacc.jar --main-class mingeriacc.Main --java-options "-Dsun.java2d.uiScale=1" --dest Mingeriacc-exe; Remove-Item -Recurse -Force exe-tmp


### Project layout

```
mingeriacc/
├── src/mingeriacc/   Java source (game logic, rendering, world gen, audio, UI)
├── build.bat/.sh     Compiles the source into Mingeriacc.jar
└── make_exe.bat      Makes a Windows exe with its own Java
```



## License

This software and any derivative works may be freely used, modified,
and distributed for non-commercial purposes only.

Commercial use (including but not limited to: selling, bundling with
commercial products, or using for commercial services) requires explicit
written permission from the copyright holder (Joe-Alder22)

All modifications must maintain this license.
