package dev.riftdlc;

import dev.riftdlc.automation.AutomationScheduler;
import dev.riftdlc.config.ConfigStore;
import dev.riftdlc.core.ModuleRegistry;
import dev.riftdlc.dupe.DupeRegistry;
import dev.riftdlc.modules.AntiAFK;
import dev.riftdlc.modules.AutoSprint;
import dev.riftdlc.modules.HudModule;
import dev.riftdlc.protocol.ProtocolManager;
import dev.riftdlc.protocol.ProtocolProfiles;
import dev.riftdlc.ui.RiftScreen;
import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Logger;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import com.mojang.blaze3d.platform.InputConstants;

public final class RiftDLC implements ClientModInitializer {
    private static RiftDLC instance;
    public static RiftDLC instance() { return instance; }
    public static final String VERSION = "0.1.0";
    private static final Logger LOG = Logger.getLogger("RiftDLC");
    private final ModuleRegistry modules = new ModuleRegistry();
    private final ProtocolManager protocols = new ProtocolManager();
    private final DupeRegistry dupes = new DupeRegistry();
    private final AutomationScheduler automation = new AutomationScheduler();
    private ConfigStore config;
    private ProtocolProfiles profiles;
    private long tick;

    public ModuleRegistry modules() { return modules; }
    public ProtocolManager protocols() { return protocols; }
    public DupeRegistry dupes() { return dupes; }
    public ProtocolProfiles profiles() { return profiles; }
    public void save() {
        try { config.save(modules); profiles.save(); }
        catch (IOException e) { LOG.warning("Could not save RiftDLC configuration: " + e.getMessage()); }
    }

    @Override public void onInitializeClient() {
        instance = this;
        Path root = FabricLoader.getInstance().getConfigDir().resolve("riftdlc");
        config = new ConfigStore(root.resolve("config.json"));
        profiles = new ProtocolProfiles(root.resolve("servers/protocols.json"));
        modules.register(new AutoSprint());
        modules.register(new AntiAFK());
        HudModule hud = new HudModule(this);
        modules.register(hud);
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("riftdlc", "hud"), (graphics, delta) -> hud.render(graphics));
        config.load(modules);
        profiles.load();
        KeyMapping gui = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.riftdlc.open", InputConstants.KEY_INSERT, KeyMapping.Category.register(Identifier.fromNamespaceAndPath("riftdlc", "main"))));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (gui.consumeClick()) client.gui.setScreen(new RiftScreen(this, client.gui.screen()));
            modules.tick();
            automation.tick(++tick);
        });
        Runtime.getRuntime().addShutdownHook(new Thread(this::save, "RiftDLC config shutdown"));
    }
}
