package dev.riftdlc.core;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ModuleRegistry {
    private final Map<String, RiftModule> modules = new LinkedHashMap<>();
    public void register(RiftModule module) {
        if (modules.putIfAbsent(module.id(), module) != null) throw new IllegalArgumentException("Duplicate module: " + module.id());
    }
    public List<RiftModule> all() { return List.copyOf(modules.values()); }
    public RiftModule get(String id) { return modules.get(id); }
    public void tick() {
        for (RiftModule module : modules.values()) if (module.enabled()) module.tick();
    }
}
