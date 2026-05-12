<pre align="center">
   ___       __  ___       __    __      _                       ___
  / _ \___ _/ /_/ _ \___ _/ /_  / /_____(_)__ ___   _______  ___/ (_)__  ___ _
 / // / _ `/ __/ , _/ _ `/ __/ / __/ __/ / -_|_-<  / __/ _ \/ _  / / _ \/ _ `/
/____/\_,_/\__/_/|_|\_,_/\__/  \__/_/ /_/\__/___/  \__/\___/\_,_/_/_//_/\_, /
                                                                       /___/
</pre>

<p align="center">
  <img alt="Forge" src="https://img.shields.io/badge/Forge-555?style=for-the-badge">
  <img alt="1.7.10" src="https://img.shields.io/badge/1.7.10-555?style=for-the-badge">
</p>

<p align="center">
  <a href="#Features">Features</a> ·
  <a href="#Aggressive-Coremod-Approach">Aggressive Coremod Approach</a> ·
  <a href="#Portal-Data-Preservation">Portal Data Preservation</a> ·
  <a href="#Build-Instructions">Build Instructions</a>
</p>

# Moveable Quest Gates

A Minecraft mod that lets Hardcore Questing Mode quest gate blocks move with vanilla pistons while preserving their portal data.

## Features

- **Moveable Quest Gates**: Allows `hqm:quest_portal` blocks to be moved by vanilla pistons.
- **Sticky Piston Support**: Sticky pistons can push and retract quest gates.
- **Normal Piston Support**: Normal pistons can push quest gates.
- **All Directions**: Movement works in all six piston directions.
- **Vanilla Limits Preserved**: The normal 12-block piston limit and unrelated piston rules still apply.
- **Scoped Behavior**: Other tile entity blocks remain unpushable unless vanilla already allows them.
- **HQM Integration**: Targets Hardcore Questing Mode `4.4.4` quest portals.

## Aggressive Coremod Approach

This mod is intentionally aggressive and is meant for controlled niche modpacks, not broad public-pack compatibility.

- **Vanilla Piston Patch**: Uses UniMixins to patch vanilla piston logic directly.
- **Custom Movement Path**: Replaces the vanilla piston move routine only when an HQM quest portal is part of the piston move.
- **Tile Entity Override**: Adds custom data storage to vanilla moving piston tile entities so HQM portal NBT survives piston animation.
- **Targeted Risk**: The aggressive behavior is scoped to `hqm:quest_portal`; unrelated piston moves should keep vanilla behavior.
- **Compatibility Target**: Built and tested against Minecraft `1.7.10`, Forge `10.13.4.1614`, and HQM `4.4.4`.

## Portal Data Preservation

Quest gates keep their configured state while moving:

- **NBT Capture**: Captures `TileEntityPortal` data before the piston replaces the source block.
- **Moving Piston Storage**: Stores the captured portal data on the temporary moving piston tile entity.
- **Restore Queue**: Restores the portal snapshot after movement finishes.
- **Quest Binding**: Keeps the assigned quest.
- **Portal Settings**: Preserves portal type, custom display item, collision flags, texture flags, and player data.
- **Save Safety**: Persists custom moving-piston data through NBT if the world is saved mid-movement.

## Build Instructions

Required local dependency jars go in `deps/`:

- `deps/HQM-The Journey-4.4.4.jar`
- `deps/+unimixins-all-1.7.10-0.3.0.jar`

Example placeholder files are included in `deps/` to document the expected jar names.

To build the mod:

```bash
JAVA_HOME=/usr/lib/jvm/java-8-openjdk ./gradlew --no-daemon clean build
```

The built JAR will be located in `build/libs/`.
