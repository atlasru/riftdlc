package dev.riftdlc.ui;

import dev.riftdlc.RiftDLC;
import dev.riftdlc.core.RiftModule;
import dev.riftdlc.core.Setting;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.KeyEvent;
import com.mojang.blaze3d.platform.InputConstants;
import org.jspecify.annotations.NonNull;

public final class RiftScreen extends Screen {
    private final RiftDLC app;
    private final Screen previous;
    private RiftModule.Category category = RiftModule.Category.MOVEMENT;
    private String expanded;
    private boolean dupes;
    private int protocolIndex;
    private int offsetX, offsetY;
    private boolean dragging;
    private int scroll;
    private RiftModule binding;
    private double dragX, dragY;

    public RiftScreen(RiftDLC app, Screen previous) {
        super(Component.literal("RiftDLC"));
        this.app = app;
        this.previous = previous;
    }
    @Override protected void init() {
        int x = Math.max(0, width / 2 - 205 + offsetX), y = Math.max(0, height / 2 - 135 + offsetY);
        int i = 0;
        for (RiftModule.Category value : RiftModule.Category.values()) {
            int bx = x + 12 + (i % 3) * 82, by = y + 34 + (i / 3) * 24;
            addRenderableWidget(Button.builder(Component.literal(value.name()), b -> {
                dupes = false; category = value; scroll = 0; expanded = null; rebuildWidgets();
            }).bounds(bx, by, 78, 20).build());
            i++;
        }
        addRenderableWidget(Button.builder(Component.literal("DUPES"), b -> {
            dupes = true; rebuildWidgets();
        }).bounds(x + 263, y + 34, 132, 20).build());
        addRenderableWidget(Button.builder(Component.literal("PROTOCOL: " + app.protocols().selected()), b -> {
            List<String> options = app.protocols().supported();
            protocolIndex = (options.indexOf(app.protocols().selected()) + 1) % options.size();
            if (app.protocols().select(options.get(protocolIndex))) {
                app.save(); rebuildWidgets();
            }
        }).bounds(x + 263, y + 58, 132, 20).build()).active = Minecraft.getInstance().getConnection() == null && app.protocols().available();

        var server = Minecraft.getInstance().getCurrentServer();
        if (server != null && app.protocols().available()) {
            String current = app.profiles().has(server.ip) ? app.profiles().get(server.ip) : "Default";
            addRenderableWidget(Button.builder(Component.literal("NEXT JOIN: " + current), b -> {
                List<String> options = new java.util.ArrayList<>();
                options.add("Default");
                options.addAll(app.protocols().supported());
                String next = options.get((options.indexOf(current) + 1) % options.size());
                app.profiles().set(server.ip, next);
                app.save(); rebuildWidgets();
            }).bounds(x + 12, y + 216, 383, 20).build());
        }

        if (dupes) {
            for (int n = 0; n < 3; n++) {
                String label = new String[] {"Analyze", "Execute", "Abort"}[n];
                addRenderableWidget(Button.builder(Component.literal(label), b -> {})
                    .bounds(x + 12 + n * 129, y + 176, 121, 20).build()).active = false;
            }
            return;
        }
        int row = 0;
        for (RiftModule module : app.modules().all()) {
            if (module.category() != category) continue;
            int py = y + 92 + (row - scroll) * 24;
            if (py >= y + 92 && py + 20 <= y + 211) {
                addRenderableWidget(Button.builder(Component.literal((module.enabled() ? "ON  " : "OFF ") + module.name()), b -> {
                    module.setEnabled(!module.enabled()); app.save(); rebuildWidgets();
                }).bounds(x + 12, py, 165, 20).build());
                addRenderableWidget(Button.builder(Component.literal(expanded != null && expanded.equals(module.id()) ? "−" : "+"), b -> {
                    expanded = module.id().equals(expanded) ? null : module.id(); rebuildWidgets();
                }).bounds(x + 181, py, 24, 20).build());
                addRenderableWidget(Button.builder(Component.literal(binding == module ? "PRESS KEY" : "BIND: " + (module.bind() < 0 ? "NONE" : module.bind())), b -> {
                    binding = module; rebuildWidgets();
                }).bounds(x + 210, py, 185, 20).build());
            }
            row++;
            if (module.id().equals(expanded)) {
                for (Setting<?> setting : module.settings()) {
                    int sy = y + 92 + (row - scroll) * 24;
                    if (sy >= y + 92 && sy + 20 <= y + 211)
                        addRenderableWidget(Button.builder(Component.literal(setting.id() + ": " + setting.get()), b -> {
                            cycle(setting); app.save(); rebuildWidgets();
                        }).bounds(x + 28, sy, 367, 20).build());
                    row++;
                }
            }
        }
        int maxScroll = Math.max(0, row - 5);
        if (scroll > maxScroll) { scroll = maxScroll; rebuildWidgets(); }
    }
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void cycle(Setting<?> setting) {
        Object value = setting.get();
        if (value instanceof Boolean v) ((Setting<Boolean>) setting).set(!v);
        else if (value instanceof Integer v && !((Setting<Integer>) setting).set(v + 1)) setting.reset();
        else if (value instanceof Double v && !((Setting<Double>) setting).set(Math.round((v + 0.1) * 10) / 10.0)) setting.reset();
        else if (value instanceof Enum<?> v) {
            Object[] values = v.getDeclaringClass().getEnumConstants();
            ((Setting) setting).set(values[(v.ordinal() + 1) % values.length]);
        }
    }
    @Override public void extractBackground(@NonNull GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractBackground(g, mouseX, mouseY, delta);
        int x = Math.max(0, width / 2 - 205 + offsetX), y = Math.max(0, height / 2 - 135 + offsetY);
        g.fill(x, y, x + 410, y + 270, 0xEE11151C);
        g.fill(x, y, x + 410, y + 3, 0xFF8AA8C4);
        g.text(font, "RIFT DLC   /   v" + RiftDLC.VERSION, x + 12, y + 14, 0xFFE6EDF5);
        if (dupes) {
            g.text(font, "Methods: " + app.dupes().all().size() + " | No verified methods installed", x + 16, y + 110, 0xFFB9C1CB);
            g.text(font, "Requirements / environment / status: N/A", x + 16, y + 128, 0xFF778493);
        } else {
            g.text(font, category.name(), x + 12, y + 80, 0xFF8AA8C4);
            g.text(font, "SCROLL for more modules | + for settings", x + 12, y + 241, 0xFF778493);
        }
        if (!app.protocols().available())
            g.text(font, "Protocol translation unavailable: ViaFabricPlus not installed", x + 12, y + 256, 0xFFBD9B78);
        else if (Minecraft.getInstance().getConnection() != null)
            g.text(font, "Reconnect to change protocol", x + 12, y + 248, 0xFFBD9B78);
    }
    @Override public void onClose() { Minecraft.getInstance().gui.setScreen(previous); }
    @Override public boolean keyPressed(KeyEvent event) {
        if (binding != null) {
            binding.bind(event.key() == InputConstants.KEY_ESCAPE ? -1 : event.key());
            binding = null; app.save(); rebuildWidgets(); return true;
        }
        return super.keyPressed(event);
    }
    @Override public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        scroll = Math.max(0, scroll - (int) Math.signum(vertical)); rebuildWidgets(); return true;
    }
    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int x = Math.max(0, width / 2 - 205 + offsetX), y = Math.max(0, height / 2 - 135 + offsetY);
        if (event.button() == 0 && event.x() >= x && event.x() < x + 410 && event.y() >= y && event.y() < y + 30) {
            dragging = true;
            dragX = event.x(); dragY = event.y();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }
    @Override public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (!dragging) return super.mouseDragged(event, dx, dy);
        offsetX += (int) Math.round(event.x() - dragX);
        offsetY += (int) Math.round(event.y() - dragY);
        offsetX = Math.max(205 - width / 2, Math.min(width / 2 - 205, offsetX));
        offsetY = Math.max(135 - height / 2, Math.min(height / 2 - 135, offsetY));
        dragX = event.x(); dragY = event.y();
        rebuildWidgets();
        return true;
    }
    @Override public boolean mouseReleased(MouseButtonEvent event) {
        if (dragging) { dragging = false; return true; }
        return super.mouseReleased(event);
    }
}
