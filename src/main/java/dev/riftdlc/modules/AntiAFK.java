package dev.riftdlc.modules;

import dev.riftdlc.core.RiftModule;
import dev.riftdlc.core.Setting;
import net.minecraft.client.Minecraft;

public final class AntiAFK extends RiftModule {
    private final Setting<Integer> interval = add(Setting.integer("interval", "Seconds between small turns", 30, 10, 300));
    private int ticks;
    public AntiAFK() { super("anti_afk", "Anti AFK", "Makes a small turn while idle", Category.MISC); }
    @Override public void tick() {
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.gui.screen() != null) return;
        if (++ticks >= interval.get() * 20) {
            ticks = 0;
            mc.player.setYRot(mc.player.getYRot() + 1.0f);
        }
    }
    @Override protected void onDisable() { ticks = 0; }
}
