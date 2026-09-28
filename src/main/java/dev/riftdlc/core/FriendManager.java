package dev.riftdlc.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

public final class FriendManager {
    private final Path file;
    private final Set<String> names = new TreeSet<>();
    public FriendManager(Path file) { this.file = file; }
    public boolean add(String name) { return names.add(normalize(name)); }
    public boolean remove(String name) { return names.remove(normalize(name)); }
    public boolean contains(String name) { return names.contains(normalize(name)); }
    public Set<String> all() { return Set.copyOf(names); }
    private static String normalize(String name) {
        if (name == null || !name.matches("[A-Za-z0-9_]{3,16}")) throw new IllegalArgumentException("Invalid player name");
        return name.toLowerCase(Locale.ROOT);
    }
    public void load() {
        if (!Files.exists(file)) return;
        try {
            Set<String> loaded = new TreeSet<>();
            for (var entry : JsonParser.parseString(Files.readString(file)).getAsJsonArray()) loaded.add(normalize(entry.getAsString()));
            names.clear(); names.addAll(loaded);
        } catch (RuntimeException | IOException ignored) { /* Keep existing friends on a damaged file. */ }
    }
    public void save() throws IOException {
        Files.createDirectories(file.getParent());
        JsonArray array = new JsonArray();
        names.forEach(array::add);
        Path tmp = Files.createTempFile(file.getParent(), "friends-", ".tmp");
        try { Files.writeString(tmp, array.toString()); Files.move(tmp, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING); }
        finally { Files.deleteIfExists(tmp); }
    }
}
