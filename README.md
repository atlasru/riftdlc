# RiftDLC 0.2.0-alpha

Experimental Fabric client mod for Minecraft Java 26.3. This is an alpha: modules are simple client-side behaviors and have not been tested against individual servers. Server-side movement checks can reject movement changes.

## Install

Use Java 25, Minecraft 26.3, Fabric Loader 0.19.5 or later, and Fabric API 0.161.0+26.3. Put the RiftDLC JAR in `.minecraft/mods`. Do not put the sources JAR there.

ViaFabricPlus 5.1.1 is optional and installed separately. Without it, protocol translation is unavailable and the selector is disabled. With it, select a target before connecting or store a per-server target for the next join. The game code remains 26.3; version selection is network protocol translation, not a live game-version switch.

## Access

Press **Insert** or **Right Shift** during gameplay. The `.rift gui` chat command opens the GUI as a fallback. Press Esc to close it. GUI keys default to Insert and Right Shift and are stored as key codes in `config/riftdlc/config.json`. Module binds can be set from the GUI or `.rift bind <id> <KEY|none>`; for example `.rift bind auto_sprint G`. Use the **+** button for settings, mouse wheel to scroll, and **BIND** to capture a key. Chat typing does not activate module keys. Default module binds are unassigned.

Commands: `.rift gui`, `.rift modules`, `.rift toggle <id>`, `.rift bind <id> <key|none>`, `.rift friend add <name>`, `.rift friend remove <name>`, `.rift friend list`, `.rift protocol [target]`, `.rift config save`, `.rift config reload`.

## Modules

| Category | Module IDs | Behavior |
| --- | --- | --- |
| Movement | `auto_sprint`, `auto_jump`, `speed`, `safe_walk`, `auto_swim`, `fast_climb`, `auto_sneak` | Sprint forward; jump while moving; modest sprint momentum; sneak near an edge; rise in water; climb faster; sneak while walking. |
| Combat | `trigger_bot`, `kill_aura` | Attack the living entity under the crosshair; attack nearest eligible player or mob after cooldown. Kill Aura excludes friends and mobs by default; range and targets are configurable. |
| Player | `auto_eat`, `auto_tool`, `auto_respawn` | Select hotbar food when hungry and hold use; select a faster tool while mining; respawn on the death screen. |
| World | `auto_mine`, `auto_place` | Hold attack on targeted blocks; hold use on targeted blocks with a held item. |
| Render | `hud`, `fullbright`, `player_radar`, `server_info` | Show configurable HUD information; increase local gamma; list nearby players; show current server address. |
| Misc | `anti_afk`, `packet_logger` | Turn slightly at intervals; store bounded packet class names in memory and optionally in a file. Packet payloads are never logged. |

All modules except HUD start disabled. GUI settings currently support Boolean and numeric controls; string settings such as packet filters are edited in the config file. The HUD displays **TPS N/A** until a reliable server tick estimator exists. The `speed`, `safe_walk`, and other movement modules are basic, without bypass claims. Packet hooks run on the network thread; extensions must be thread safe.

Friends are saved in `config/riftdlc/friends.json`; protocol profiles are in `config/riftdlc/servers/protocols.json`. JSON configuration is written with a temporary file. The Dupe page contains no verified duplication methods and offers no executable exploit.

## Build and testing

Run `./gradlew build` with Java 25. The client JAR is `build/libs/riftdlc-0.2.0-alpha.jar`. A dev launch can be attempted with `./gradlew runClient`; Minecraft assets and a working display are required. Source compilation and unit tests do not prove that GUI keys or modules work on a player's machine. The draft PR remains open for manual gameplay validation.

## Acknowledgements / Upstream

Meteor Client: https://github.com/MeteorDevelopment/meteor-client (GPL-3.0). Its module organization and feature vocabulary informed this project. No Meteor source code or assets were copied in this revision. If code is adapted in future revisions, individual files and `THIRD_PARTY.md` must identify their upstream paths and retain required notices. ViaFabricPlus and Fabric details are in [THIRD_PARTY.md](THIRD_PARTY.md).

RiftDLC is GPL-3.0-or-later. Minecraft trademarks belong to their owners.
