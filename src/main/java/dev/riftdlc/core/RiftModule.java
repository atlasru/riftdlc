package dev.riftdlc.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class RiftModule {
    public enum Category { COMBAT, MOVEMENT, PLAYER, WORLD, RENDER, MISC }
    private final String id, name, description;
    private final Category category;
    private final List<Setting<?>> settings = new ArrayList<>();
    private volatile boolean enabled;

    protected RiftModule(String id, String name, String description, Category category) {
        this.id = Objects.requireNonNull(id);
        this.name = Objects.requireNonNull(name);
        this.description = Objects.requireNonNull(description);
        this.category = Objects.requireNonNull(category);
    }
    public final String id() { return id; }
    public final String name() { return name; }
    public final String description() { return description; }
    public final Category category() { return category; }
    public final List<Setting<?>> settings() { return List.copyOf(settings); }
    protected final <T> Setting<T> add(Setting<T> setting) {
        if (settings.stream().anyMatch(s -> s.id().equals(setting.id()))) throw new IllegalArgumentException("Duplicate setting: " + setting.id());
        settings.add(setting);
        return setting;
    }
    public final boolean enabled() { return enabled; }
    public final void setEnabled(boolean next) {
        if (enabled == next) return;
        if (next) {
            onEnable();
            enabled = true;
        } else {
            onDisable();
            enabled = false;
        }
    }
    public void tick() {}
    protected void onEnable() {}
    protected void onDisable() {}
}
