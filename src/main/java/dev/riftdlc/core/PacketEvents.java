package dev.riftdlc.core;

import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.network.protocol.Packet;

/** Network callbacks execute on Netty or the caller thread; listeners must be thread safe. */
public final class PacketEvents {
    public enum Direction { INCOMING, OUTGOING }
    public interface Listener {
        /** Return false to cancel. New listeners should observe only unless cancellation is essential. */
        boolean onPacket(Direction direction, Packet<?> packet);
    }
    private final CopyOnWriteArrayList<Listener> listeners = new CopyOnWriteArrayList<>();
    public void register(Listener listener) { listeners.addIfAbsent(listener); }
    public void unregister(Listener listener) { listeners.remove(listener); }
    public boolean dispatch(Direction direction, Packet<?> packet) {
        for (Listener listener : listeners) if (!listener.onPacket(direction, packet)) return false;
        return true;
    }
}
