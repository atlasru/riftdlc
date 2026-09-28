package dev.riftdlc.modules;

import dev.riftdlc.RiftDLC;
import dev.riftdlc.core.RiftModule;
import dev.riftdlc.core.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class HudModule extends RiftModule {
    private final Setting<Boolean> watermark = add(Setting.bool("watermark", "RiftDLC name and version", true));
    private final Setting<Boolean> fps = add(Setting.bool("fps", "Frames per second", false));
    private final Setting<Boolean> ping = add(Setting.bool("ping", "Connection latency", false));
    private final Setting<Boolean> coordinates = add(Setting.bool("coordinates", "Player block position", false));
    private final Setting<Boolean> protocol = add(Setting.bool("protocol", "Target protocol", false));
    private final Setting<Boolean> active = add(Setting.bool("modules", "Enabled module list", false));
    private final RiftDLC app;
    public HudModule(RiftDLC app) {
        super("hud", "HUD", "Compact optional information overlay", Category.RENDER);
        this.app = app;
        setEnabled(true);
    }
    public void render(GuiGraphicsExtractor g) {
        if (!enabled()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gui.screen() != null) return;
        int y = 7;
        if (watermark.get()) y = line(g, "RIFT DLC  " + RiftDLC.VERSION, y, 0xFFB9D4E9);
        if (fps.get()) y = line(g, "FPS  " + mc.getFps(), y, 0xFFE1E6ED);
        if (ping.get() && mc.getConnection() != null) {
            var info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
            if (info != null) y = line(g, "PING  " + info.getLatency() + " ms", y, 0xFFE1E6ED);
        }
        if (coordinates.get()) {
            var pos = mc.player.blockPosition();
            y = line(g, "XYZ  " + pos.getX() + " / " + pos.getY() + " / " + pos.getZ(), y, 0xFFE1E6ED);
        }
        if (protocol.get()) y = line(g, "PROTO  " + app.protocols().selected(), y, 0xFFE1E6ED);
        if (active.get()) for (RiftModule module : app.modules().all())
            if (module.enabled() && module != this) y = line(g, module.name(), y, 0xFF9FAFBF);
    }
    private int line(GuiGraphicsExtractor g, String text, int y, int color) {
        g.text(Minecraft.getInstance().font, text, 7, y, color);
        return y + 12;
    }
}
