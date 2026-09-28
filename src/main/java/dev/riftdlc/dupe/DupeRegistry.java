package dev.riftdlc.dupe;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** No exploit is registered in v0.1.0. Methods must supply explicit checks and verification. */
public final class DupeRegistry {
    public enum State { IDLE, CHECKING, PREPARING, EXECUTING, VERIFYING, SUCCESS, FAILED, CANCELLED }
    public record Compatibility(boolean supported, String reason) {}
    public interface Method {
        String id();
        String name();
        List<String> requirements();
        Compatibility analyze(String protocol);
        void execute();
        void abort();
    }
    private final Map<String, Method> methods = new LinkedHashMap<>();
    public void register(Method method) {
        if (methods.putIfAbsent(method.id(), method) != null) throw new IllegalArgumentException("Duplicate method");
    }
    public List<Method> all() { return List.copyOf(methods.values()); }
}
