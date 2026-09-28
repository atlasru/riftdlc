package dev.riftdlc.modules;

import dev.riftdlc.core.RiftModule;
import dev.riftdlc.core.Setting;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/** Stores only packet class names; no packet payload, chat text or account data. */
public final class PacketLogger extends RiftModule {
    private static final Logger LOG = Logger.getLogger("RiftDLC");
    private final Setting<Boolean> inbound = add(Setting.bool("inbound", "Record incoming packets", true));
    private final Setting<Boolean> outbound = add(Setting.bool("outbound", "Record outgoing packets", true));
    private final Setting<Boolean> fileLogging = add(Setting.bool("file", "Write a bounded file in the RiftDLC config folder", false));
    private final Setting<Integer> limit = add(Setting.integer("limit", "Maximum memory entries", 500, 100, 2000));
    private final Setting<String> nameFilter = add(Setting.string("filter", "Comma-separated exact packet class names; empty means all", ""));
    private final ArrayDeque<String> recent = new ArrayDeque<>();
    private final ArrayBlockingQueue<String> diskQueue = new ArrayBlockingQueue<>(2048);
    private final Path file;
    private volatile boolean writerRunning;
    private Thread writer;

    public PacketLogger(Path file) {
        super("packet_logger", "Packet Logger", "Bounded packet name log for debugging", Category.MISC);
        this.file = file;
    }
    public void record(boolean incoming, String packetName) {
        if (!enabled() || !(incoming ? inbound.get() : outbound.get())) return;
        String filter = nameFilter.get();
        if (!filter.isBlank()) {
            boolean match = false;
            for (String name : filter.split(",")) if (name.strip().equals(packetName)) { match = true; break; }
            if (!match) return;
        }
        String entry = Instant.now() + " " + (incoming ? "IN  " : "OUT ") + packetName;
        synchronized (recent) {
            while (recent.size() >= limit.get()) recent.removeFirst();
            recent.addLast(entry);
        }
        if (fileLogging.get()) diskQueue.offer(entry); // Drop when full; never block Netty.
    }
    public List<String> snapshot() {
        synchronized (recent) { return List.copyOf(recent); }
    }
    @Override protected void onEnable() {
        writerRunning = true;
        writer = new Thread(this::writeLoop, "RiftDLC packet writer");
        writer.setDaemon(true);
        writer.start();
    }
    @Override protected void onDisable() {
        writerRunning = false;
        if (writer != null) writer.interrupt();
    }
    public void closeOnShutdown() {
        setEnabled(false);
        if (writer != null) {
            try { writer.join(1500); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
    }
    private void writeLoop() {
        while (writerRunning || !diskQueue.isEmpty()) {
            try {
                String first = diskQueue.poll(500, TimeUnit.MILLISECONDS);
                if (first == null) continue;
                List<String> batch = new ArrayList<>();
                batch.add(first);
                diskQueue.drainTo(batch, 127);
                Files.createDirectories(file.getParent());
                if (Files.exists(file) && Files.size(file) > 5_000_000L)
                    Files.move(file, file.resolveSibling("packets.previous.log"), StandardCopyOption.REPLACE_EXISTING);
                Files.writeString(file, String.join("\n", batch) + "\n", StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (InterruptedException ignored) {
                // onDisable wakes the writer; buffered entries are drained before exit.
            } catch (IOException e) {
                LOG.warning("Packet log file write failed: " + e.getMessage());
                diskQueue.clear();
                writerRunning = false;
            }
        }
    }
}
