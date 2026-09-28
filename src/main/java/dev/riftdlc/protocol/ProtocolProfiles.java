package dev.riftdlc.protocol;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public final class ProtocolProfiles {
    private final Path file;
    private final Map<String, String> choices = new TreeMap<>();
    public ProtocolProfiles(Path file) { this.file = file; }
    public static String normalize(String address) {
        String result = address.strip().toLowerCase(Locale.ROOT);
        if (result.endsWith(":25565")) result = result.substring(0, result.length() - 6);
        if (result.isBlank()) throw new IllegalArgumentException("Empty server address");
        return result;
    }
    public String get(String address) { return choices.getOrDefault(normalize(address), "Auto"); }
    public boolean has(String address) { return choices.containsKey(normalize(address)); }
    public void set(String address, String protocol) {
        String key = normalize(address);
        if (protocol == null || protocol.isBlank() || protocol.equals("Default")) choices.remove(key);
        else choices.put(key, protocol);
    }
    public void load() {
        if (!Files.exists(file)) return;
        try {
            JsonObject root = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            if (root.get("format").getAsInt() != 1) return;
            JsonObject entries = root.getAsJsonObject("servers");
            if (entries == null) return;
            Map<String, String> loaded = new TreeMap<>();
            for (var entry : entries.entrySet()) {
                if (entry.getValue().isJsonPrimitive()) loaded.put(normalize(entry.getKey()), entry.getValue().getAsString());
            }
            choices.clear();
            choices.putAll(loaded);
        } catch (RuntimeException | IOException ignored) {
            // Keep current defaults on partial/corrupt data.
        }
    }
    public void save() throws IOException {
        JsonObject root = new JsonObject();
        root.addProperty("format", 1);
        JsonObject entries = new JsonObject();
        choices.forEach(entries::addProperty);
        root.add("servers", entries);
        Files.createDirectories(file.getParent());
        Path tmp = Files.createTempFile(file.getParent(), "protocol-", ".tmp");
        try {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Files.writeString(tmp, gson.toJson(root), StandardCharsets.UTF_8);
            try { Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (java.nio.file.AtomicMoveNotSupportedException e) { Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(tmp); }
    }
}
