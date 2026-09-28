package dev.riftdlc;

import dev.riftdlc.automation.AutomationScheduler;
import dev.riftdlc.config.ConfigStore;
import dev.riftdlc.core.ModuleRegistry;
import dev.riftdlc.core.FriendManager;
import dev.riftdlc.core.PacketEvents;
import dev.riftdlc.input.KeybindManager;
import dev.riftdlc.command.RiftCommands;
import dev.riftdlc.dupe.DupeRegistry;
import dev.riftdlc.modules.AntiAFK;
import dev.riftdlc.modules.AutoSprint;
import dev.riftdlc.modules.ActionModules;
import dev.riftdlc.modules.HudModule;
import dev.riftdlc.modules.PacketLogger;
import dev.riftdlc.modules.PlayerRadar;
import dev.riftdlc.modules.ServerInfo;
import dev.riftdlc.protocol.ProtocolManager;
import dev.riftdlc.protocol.ProtocolProfiles;
import dev.riftdlc.ui.RiftScreen;
import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Logger;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public final class RiftDLC implements ClientModInitializer {
    private static RiftDLC instance;
    public static RiftDLC instance() { return instance; }
    public static final String VERSION = "0.2.0-alpha";
    private static final Logger LOG = Logger.getLogger("RiftDLC");
    private final ModuleRegistry modules = new ModuleRegistry();
    private final PacketEvents packetEvents = new PacketEvents();
    private final ProtocolManager protocols = new ProtocolManager();
    private final DupeRegistry dupes = new DupeRegistry();
    private final AutomationScheduler automation = new AutomationScheduler();
    private PacketLogger packetLogger;
    private ConfigStore config;
    private ProtocolProfiles profiles;
    private FriendManager friends;
    private final KeybindManager keybinds = new KeybindManager(this);
    private long tick;
    private boolean openGuiNextTick;

    public ModuleRegistry modules() { return modules; }
    public PacketEvents packetEvents() { return packetEvents; }
    public ProtocolManager protocols() { return protocols; }
    public DupeRegistry dupes() { return dupes; }
    public ProtocolProfiles profiles() { return profiles; }
    public PacketLogger packetLogger() { return packetLogger; }
    public FriendManager friends() { return friends; }
    public KeybindManager keybinds() { return keybinds; }
    public void requestGui() { openGuiNextTick = true; }
    public void reload() { config.load(modules, keybinds); profiles.load(); friends.load(); }
    public void save() {
        try { config.save(modules, keybinds); profiles.save(); friends.save(); }
        catch (IOException e) { LOG.warning("Could not save RiftDLC configuration: " + e.getMessage()); }
    }

    @Override public void onInitializeClient() {
        LOG.info("[RiftDLC] Initializing RiftDLC " + VERSION);
        instance = this;
        Path root = FabricLoader.getInstance().getConfigDir().resolve("riftdlc");
        config = new ConfigStore(root.resolve("config.json"));
        profiles = new ProtocolProfiles(root.resolve("servers/protocols.json"));
        friends = new FriendManager(root.resolve("friends.json"));
        packetLogger = new PacketLogger(root.resolve("packets.log"));
        packetEvents.register((direction, packet) -> {
            packetLogger.record(direction == PacketEvents.Direction.INCOMING, packet.getClass().getSimpleName());
            return true;
        });
        modules.register(new AutoSprint());
        for (ActionModules.Action action : ActionModules.Action.values())
            if (action != ActionModules.Action.NO_FALL) modules.register(new ActionModules(action));
        modules.register(new AntiAFK());
        modules.register(packetLogger);
        HudModule hud = new HudModule(this);
        modules.register(hud);
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("riftdlc", "hud"), (graphics, delta) -> hud.render(graphics));
        PlayerRadar radar = new PlayerRadar();
        ServerInfo serverInfo = new ServerInfo();
        modules.register(radar);
        modules.register(serverInfo);
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("riftdlc", "radar"), (graphics, delta) -> radar.render(graphics));
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("riftdlc", "server_info"), (graphics, delta) -> serverInfo.render(graphics));
        reload();
        RiftCommands commands = new RiftCommands(this);
        ClientSendMessageEvents.ALLOW_CHAT.register(message -> !commands.handle(message));
        LOG.info("[RiftDLC] Registered " + modules.all().size() + " modules");
        LOG.info("[RiftDLC] GUI keybinds: INSERT, RIGHT SHIFT");
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (openGuiNextTick) {
                openGuiNextTick = false;
                client.gui.setScreen(new RiftScreen(this, null));
            }
            modules.tick();
            automation.tick(++tick);
        });
        LOG.info("[RiftDLC] Initialization complete");
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            save();
            packetLogger.closeOnShutdown();
        }, "RiftDLC config shutdown"));
    }
}
