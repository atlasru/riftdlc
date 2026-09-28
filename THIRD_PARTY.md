# Third-party components

- Minecraft 26.3: runtime dependency, not redistributed in the RiftDLC JAR.
- Fabric Loader 0.19.5, Fabric API 0.161.0+26.3, Loom 1.18.2: build and runtime dependencies. Fabric projects: https://github.com/FabricMC
- ViaFabricPlus 5.1.1: optional compile API. Users install the runtime mod separately; none of its source is copied. GPL-3.0-or-later. https://github.com/ViaVersion/ViaFabricPlus
- ViaVersion: API transitively provided through ViaFabricPlus; no source copied.
- Meteor Client: GPL-3.0, https://github.com/MeteorDevelopment/meteor-client. Studied for module concepts and naming. **No Meteor files, assets, or code were copied or substantially adapted in this revision.** Future reuse must record upstream file paths and preserve copyright notices.
- Fabric example mod (CC0): consulted for 26.3 build conventions. The Gradle wrapper is a toolchain artifact.

RiftDLC is licensed GPL-3.0-or-later.
