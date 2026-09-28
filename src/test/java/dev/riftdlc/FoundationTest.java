package dev.riftdlc;

import dev.riftdlc.automation.AutomationScheduler;
import dev.riftdlc.config.ConfigStore;
import dev.riftdlc.core.ModuleRegistry;
import dev.riftdlc.core.RiftModule;
import dev.riftdlc.core.Setting;
import dev.riftdlc.dupe.DupeRegistry;
import dev.riftdlc.protocol.ProtocolProfiles;
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
}
