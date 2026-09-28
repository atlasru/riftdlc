package dev.riftdlc.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.riftdlc.core.ModuleRegistry;
import dev.riftdlc.core.RiftModule;
import dev.riftdlc.core.Setting;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.logging.Logger;

public final class ConfigStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Logger LOG = Logger.getLogger("RiftDLC");
    private final Path file;

    public ConfigStore(Path file) { this.file = file; }
    public void load(ModuleRegistry registry) {
        if (!Files.exists(file)) return;
        try {
            JsonObject root = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            if (!root.has("format") || root.get("format").getAsInt() != 1) return;
            JsonObject modules = root.getAsJsonObject("modules");
            if (modules == null) return;
            for (RiftModule module : registry.all()) {
                if (!modules.has(module.id()) || !modules.get(module.id()).isJsonObject()) continue;
                JsonObject entry = modules.getAsJsonObject(module.id());
                JsonObject values = entry.getAsJsonObject("settings");
                if (values != null) for (Setting<?> setting : module.settings())
                    if (values.has(setting.id())) setting.fromJson(values.get(setting.id()));
                if (entry.has("enabled") && entry.get("enabled").isJsonPrimitive()) {
                    String enabled = entry.get("enabled").getAsString();
                    if (enabled.equals("true") || enabled.equals("false"))
                        module.setEnabled(Boolean.parseBoolean(enabled));
                }
            }
        } catch (RuntimeException | IOException e) {
            LOG.warning("Ignoring malformed RiftDLC config: " + e.getMessage());
        }
    }
    public void save(ModuleRegistry registry) throws IOException {
        JsonObject root = new JsonObject();
        root.addProperty("format", 1);
        JsonObject modules = new JsonObject();
        for (RiftModule module : registry.all()) {
            JsonObject entry = new JsonObject();
            entry.addProperty("enabled", module.enabled());
            JsonObject values = new JsonObject();
            for (Setting<?> setting : module.settings()) values.add(setting.id(), setting.toJson());
            entry.add("settings", values);
            modules.add(module.id(), entry);
        }
        root.add("modules", modules);
        Files.createDirectories(file.getParent());
        Path tmp = Files.createTempFile(file.getParent(), "config-", ".tmp");
        try {
            Files.writeString(tmp, GSON.toJson(root), StandardCharsets.UTF_8);
            try { Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (java.nio.file.AtomicMoveNotSupportedException e) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally { Files.deleteIfExists(tmp); }
    }
}
