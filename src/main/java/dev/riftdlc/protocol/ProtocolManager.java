package dev.riftdlc.protocol;

import com.viaversion.viafabricplus.ViaFabricPlus;
import com.viaversion.viafabricplus.api.protocoltranslator.ProtocolTranslation;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;

public final class ProtocolManager {
    public boolean available() { return FabricLoader.getInstance().isModLoaded("viafabricplus"); }
    public List<String> supported() {
        if (!available()) return List.of("Native");
        List<String> result = new ArrayList<>();
        result.add("Auto");
        result.add("Native");
        for (ProtocolVersion version : ProtocolVersion.getReversedProtocols())
            if (version.getName().matches("(1\\..*|26\\..*)") && !result.contains(version.getName()))
                result.add(version.getName());
        return List.copyOf(result);
    }
    public String selected() {
        if (!available()) return "Native";
        ProtocolVersion version = ViaFabricPlus.api().targetVersion();
        if (version.equals(ProtocolTranslation.AUTO_DETECT_VERSION)) return "Auto";
        return version.getName();
    }
    public boolean select(String name) {
        if (Minecraft.getInstance().getConnection() != null) return false;
        if (!available()) return name.equals("Native");
        ProtocolVersion version = resolve(name);
        if (version == null) return false;
        ViaFabricPlus.api().setTargetVersion(version);
        return true;
    }
    public boolean setServerVersion(ServerData server, String name) {
        if (!available()) return false;
        ProtocolVersion version = resolve(name);
        if (version == null) return false;
        ViaFabricPlus.api().protocolTranslation().setServerVersion(server, version);
        return true;
    }
    private ProtocolVersion resolve(String name) {
        if (name.equals("Auto")) return ProtocolTranslation.AUTO_DETECT_VERSION;
        if (name.equals("Native")) return ProtocolVersion.v26_3;
        return ProtocolVersion.getReversedProtocols().stream().filter(p -> p.getName().equals(name)).findFirst().orElse(null);
    }
    public boolean offhandAvailable() {
        if (!available()) return true;
        return ViaFabricPlus.api().targetVersion().newerThanOrEqualTo(ProtocolVersion.v1_9);
    }
}
