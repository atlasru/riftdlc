# RiftDLC

RiftDLC is an experimental, local-first Fabric client mod for Minecraft Java 26.3. Version 0.1.0 starts the module, settings, protocol and automation foundations. This is an early development build.

## Install

Install Minecraft 26.3, Fabric Loader 0.19.5 or newer, Fabric API compatible with 26.3, and Java 25. Place the RiftDLC JAR in the mods directory. Install ViaFabricPlus 5.1.1 separately to enable protocol translation. Open RiftDLC with **Insert**.

The draggable GUI has module categories, toggles, an optional protocol selector, and a Dupe page. Protocol selection changes the target of the **next connection**. Disconnect before changing the global choice. While connected, NEXT JOIN stores a choice for that server; reconnect for it to take effect. Default inherits the global choice. Auto is ViaFabricPlus auto detection (release servers 1.7+); older server protocols require explicit selection. This does not replace the running game code.

Shipped modules: **Auto Sprint**, **Anti AFK**, **Packet Logger**, and **HUD**. Movement, AFK and packet logging modules are disabled by default. HUD starts with a small watermark; FPS, ping, coordinates, protocol and enabled modules can be toggled independently. Packet Logger records only packet class names, keeps at most 500 entries by default, and can optionally write rotated files (5 MB limit per file). Its exact-name filter can be edited in config.json. Configuration lives in .minecraft/config/riftdlc/ and is written atomically. The Dupe page contains **no working methods**. No exploit is claimed.

## Build

Run ./gradlew build on Java 25 (Windows: gradlew.bat build). The client JAR is in build/libs/; CI uploads it as RiftDLC-0.1.0.

## Scope and limitations

The first build has a simple GUI. Richer modules and a complete dupe execution flow are not implemented yet. Per-server profile application uses a focused ConnectScreen injection and needs live gameplay verification. No native injection, launcher, telemetry or account collection is included.

GPL-3.0-or-later. See [THIRD_PARTY.md](THIRD_PARTY.md).
