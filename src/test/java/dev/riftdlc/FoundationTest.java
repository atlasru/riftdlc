package dev.riftdlc;

import dev.riftdlc.automation.AutomationScheduler;
import dev.riftdlc.config.ConfigStore;
import dev.riftdlc.core.ModuleRegistry;
import dev.riftdlc.core.FriendManager;
import dev.riftdlc.input.KeybindManager;
import dev.riftdlc.command.RiftCommands;
import dev.riftdlc.modules.ActionModules;
import dev.riftdlc.core.RiftModule;
import dev.riftdlc.core.Setting;
import dev.riftdlc.dupe.DupeRegistry;
import dev.riftdlc.protocol.ProtocolProfiles;
import dev.riftdlc.modules.PacketLogger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class FoundationTest {
    @TempDir Path directory;
    static class Example extends RiftModule {
        int starts, stops;
        final Setting<Integer> range = add(Setting.integer("range", "blocks", 3, 1, 10));
        Example() { super("example", "Example", "Test", Category.MISC); }
        @Override protected void onEnable() { starts++; }
        @Override protected void onDisable() { stops++; }
    }
    @Test void lifecycleAndUniqueIds() {
        Example module = new Example();
        module.setEnabled(true); module.setEnabled(true); module.setEnabled(false);
        assertEquals(1, module.starts); assertEquals(1, module.stops);
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(module);
        assertThrows(IllegalArgumentException.class, () -> registry.register(new Example()));
    }
    @Test void settingsValidateAndReset() {
        Setting<Integer> range = Setting.integer("range", "blocks", 3, 1, 10);
        assertFalse(range.set(11));
        assertFalse(range.fromJson(com.google.gson.JsonParser.parseString("\"bad\"")));
        assertTrue(range.fromJson(com.google.gson.JsonParser.parseString("8")));
        range.reset();
        assertEquals(3, range.get());
    }
    @Test void configPersistsAndSurvivesCorruption() throws Exception {
        Path file = directory.resolve("riftdlc/config.json");
        ModuleRegistry registry = new ModuleRegistry();
        Example module = new Example();
        registry.register(module);
        module.range.set(7); module.setEnabled(true);
        new ConfigStore(file).save(registry);
        ModuleRegistry restored = new ModuleRegistry();
        Example copy = new Example();
        restored.register(copy);
        new ConfigStore(file).load(restored);
        assertTrue(copy.enabled()); assertEquals(7, copy.range.get());
        Files.writeString(file, "{broken");
        new ConfigStore(file).load(restored);
        assertTrue(copy.enabled()); assertEquals(7, copy.range.get());
    }
    @Test void profilesNormalizeAndPersist() throws Exception {
        Path file = directory.resolve("servers/protocols.json");
        ProtocolProfiles profiles = new ProtocolProfiles(file);
        profiles.set(" Example.COM:25565 ", "1.12.2");
        profiles.save();
        ProtocolProfiles copy = new ProtocolProfiles(file);
        copy.load();
        assertEquals("1.12.2", copy.get("example.com"));
        copy.set("example.com", "Auto");
        assertEquals("Auto", copy.get("example.com"));
        assertTrue(copy.has("example.com"));
        copy.set("example.com", "Default");
        assertFalse(copy.has("example.com"));
    }
    @Test void automationTimeoutAndCancellation() {
        AutomationScheduler scheduler = new AutomationScheduler();
        AtomicBoolean cancelled = new AtomicBoolean();
        assertTrue(scheduler.start(new AutomationScheduler.Task() {
            @Override public boolean tick() { return false; }
            @Override public void cancel() { cancelled.set(true); }
        }, 10, 2));
        scheduler.tick(11); scheduler.tick(12);
        assertEquals(AutomationScheduler.State.FAILED, scheduler.state());
        assertTrue(cancelled.get());
    }
    @Test void dupeRegistryRejectsDuplicateAndStartsEmpty() {
        DupeRegistry registry = new DupeRegistry();
        assertTrue(registry.all().isEmpty());
        DupeRegistry.Method method = new DupeRegistry.Method() {
            public String id() { return "test"; }
            public String name() { return "Test"; }
            public List<String> requirements() { return List.of(); }
            public DupeRegistry.Compatibility analyze(String protocol) { return new DupeRegistry.Compatibility(false, "Test"); }
            public void execute() {}
            public void abort() {}
        };
        registry.register(method);
        assertThrows(IllegalArgumentException.class, () -> registry.register(method));
    }
    @Test void packetLoggerBoundsMemoryAndIsDisabledByDefault() {
        PacketLogger log = new PacketLogger(directory.resolve("packets.log"));
        log.record(true, "BeforeEnable");
        assertTrue(log.snapshot().isEmpty());
        log.setEnabled(true);
        for (int i = 0; i < 600; i++) log.record(i % 2 == 0, "Packet" + i);
        assertEquals(500, log.snapshot().size());
        assertTrue(log.snapshot().getFirst().endsWith("Packet100"));
        log.setEnabled(false);
        assertFalse(Files.exists(directory.resolve("packets.log")));
    }
    @Test void bindsPersistAndMalformedBindIsIgnored() throws Exception {
        Path path = directory.resolve("riftdlc/config.json");
        ModuleRegistry registry = new ModuleRegistry();
        Example example = new Example(); registry.register(example);
        example.bind(65);
        new ConfigStore(path).save(registry);
        Example restored = new Example();
        ModuleRegistry copy = new ModuleRegistry(); copy.register(restored);
        new ConfigStore(path).load(copy);
        assertEquals(65, restored.bind());
        Files.writeString(path, Files.readString(path).replace("\"bind\": 65", "\"bind\": \"bad\""));
        restored.bind(-1);
        new ConfigStore(path).load(copy);
        assertEquals(-1, restored.bind());
    }
    @Test void friendNamesAreCaseInsensitiveAndPersist() throws Exception {
        Path path = directory.resolve("friends.json");
        FriendManager friends = new FriendManager(path);
        assertTrue(friends.add("Alex_42"));
        assertFalse(friends.add("aLeX_42"));
        assertThrows(IllegalArgumentException.class, () -> friends.add("bad name"));
        friends.save();
        FriendManager copy = new FriendManager(path); copy.load();
        assertTrue(copy.contains("ALEX_42"));
        assertTrue(copy.remove("alex_42"));
    }
    @Test void corruptedFriendFileDoesNotEraseCurrentNames() throws Exception {
        Path path = directory.resolve("friends.json");
        FriendManager friends = new FriendManager(path);
        friends.add("Player123");
        Files.writeString(path, "{bad");
        friends.load();
        assertTrue(friends.contains("player123"));
    }
    @Test void onlyPressEdgesToggleKeys() {
        KeybindManager keys = new KeybindManager(null);
        int insert = com.mojang.blaze3d.platform.InputConstants.KEY_INSERT;
        int shift = com.mojang.blaze3d.platform.InputConstants.KEY_RSHIFT;
        assertEquals(KeybindManager.GuiAction.OPEN, keys.guiAction(insert, 1, true, false));
        assertEquals(KeybindManager.GuiAction.OPEN, keys.guiAction(shift, 1, true, false));
        assertEquals(KeybindManager.GuiAction.CLOSE, keys.guiAction(insert, 1, false, true));
        assertEquals(KeybindManager.GuiAction.NONE, keys.guiAction(shift, 2, true, false));
        assertEquals(KeybindManager.GuiAction.NONE, keys.guiAction(insert, 1, false, false));
    }
    @Test void commandPrefixDoesNotCaptureOrdinaryChat() {
        assertTrue(RiftCommands.isCommand(".rift gui"));
        assertTrue(RiftCommands.isCommand(".RIFT"));
        assertFalse(RiftCommands.isCommand(".rifted"));
        assertFalse(RiftCommands.isCommand("hello .rift"));
    }
    @Test void actionsHaveConcreteModuleIdentity() {
        for (var action : ActionModules.Action.values()) {
            var module = new ActionModules(action);
            assertEquals(action.name().toLowerCase(), module.id());
            assertFalse(module.description().isBlank());
            assertFalse(module.enabled());
        }
    }
    @Test void mixinPackageDoesNotContainEntrypoint() throws Exception {
        try (var stream = getClass().getClassLoader().getResourceAsStream("riftdlc.mixins.json")) {
            assertNotNull(stream);
            var root = com.google.gson.JsonParser.parseString(new String(stream.readAllBytes())).getAsJsonObject();
            assertEquals("dev.riftdlc.mixin", root.get("package").getAsString());
            assertTrue(root.getAsJsonArray("client").toString().contains("KeyboardMixin"));
        }
    }
    @Test void guiKeysAndModuleBindsSurviveConfigRoundTrip() throws Exception {
        Path file = directory.resolve("riftdlc/config.json");
        ModuleRegistry modules = new ModuleRegistry();
        Example module = new Example(); modules.register(module); module.bind(71);
        KeybindManager keys = new KeybindManager(null); keys.setGuiKeys(73, 229);
        new ConfigStore(file).save(modules, keys);
        ModuleRegistry restored = new ModuleRegistry();
        Example copy = new Example(); restored.register(copy);
        KeybindManager restoredKeys = new KeybindManager(null);
        restoredKeys.setGuiKeys(0, 0);
        new ConfigStore(file).load(restored, restoredKeys);
        assertEquals(71, copy.bind());
        assertEquals(73, restoredKeys.insert());
        assertEquals(229, restoredKeys.secondary());
    }
}
