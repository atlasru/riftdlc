package dev.riftdlc.core;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import java.util.Objects;
import java.util.function.Predicate;

public final class Setting<T> {
    private final String id;
    private final String description;
    private final T defaultValue;
    private final Predicate<T> validator;
    private final Class<T> type;
    private T value;

    private Setting(String id, String description, T initial, Class<T> type, Predicate<T> validator) {
        this.id = Objects.requireNonNull(id);
        this.description = Objects.requireNonNull(description);
        this.defaultValue = Objects.requireNonNull(initial);
        this.value = initial;
        this.type = type;
        this.validator = validator;
    }

    public static Setting<Boolean> bool(String id, String description, boolean initial) {
        return new Setting<>(id, description, initial, Boolean.class, _ -> true);
    }
    public static Setting<Integer> integer(String id, String description, int initial, int min, int max) {
        return new Setting<>(id, description, initial, Integer.class, v -> v >= min && v <= max);
    }
    public static Setting<Double> decimal(String id, String description, double initial, double min, double max) {
        return new Setting<>(id, description, initial, Double.class, v -> Double.isFinite(v) && v >= min && v <= max);
    }
    public static Setting<String> string(String id, String description, String initial) {
        return new Setting<>(id, description, initial, String.class, v -> v.length() <= 256);
    }
    public static <E extends Enum<E>> Setting<E> enumeration(String id, String description, E initial, Class<E> type) {
        return new Setting<>(id, description, initial, type, _ -> true);
    }
    public String id() { return id; }
    public String description() { return description; }
    public T get() { return value; }
    public T defaultValue() { return defaultValue; }
    public boolean set(T next) {
        if (next == null || !validator.test(next)) return false;
        value = next;
        return true;
    }
    public void reset() { value = defaultValue; }
    public JsonElement toJson() { return new JsonPrimitive(value instanceof Enum<?> e ? e.name() : value.toString()); }
    @SuppressWarnings({"unchecked", "rawtypes"})
    public boolean fromJson(JsonElement element) {
        try {
            if (element == null || !element.isJsonPrimitive()) return false;
            Object parsed;
            if (type == Boolean.class) {
                String text = element.getAsString();
                if (!text.equals("true") && !text.equals("false")) return false;
                parsed = Boolean.parseBoolean(text);
            } else if (type == Integer.class) parsed = element.getAsInt();
            else if (type == Double.class) parsed = element.getAsDouble();
            else if (type == String.class) parsed = element.getAsString();
            else if (type.isEnum()) parsed = Enum.valueOf((Class<Enum>) type, element.getAsString());
            else return false;
            return set((T) parsed);
        } catch (RuntimeException ignored) { return false; }
    }
}
