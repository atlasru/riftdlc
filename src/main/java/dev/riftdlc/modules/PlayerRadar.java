package dev.riftdlc.modules;

import dev.riftdlc.RiftDLC;
import dev.riftdlc.core.RiftModule;
import dev.riftdlc.core.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Bounded nearby-player list rendered in the HUD. */
public final class PlayerRadar extends RiftModule {
    private final Setting<Integer> distance = add(Setting.integer("distance", "Player search radius", 64, 8, 256));
    public PlayerRadar() { super("player_radar", "Player Radar", "Show nearby players and distance", Category.RENDER); }
    public void render(GuiGraphicsExtractor graphics) {
        Minecraft mc = Minecraft.getInstance();
        if (!enabled() || mc.player == null || mc.level == null || mc.gui.screen() != null) return;
        int y = 8, x = Math.max(5, graphics.guiWidth() - 160);
        graphics.text(mc.font, "NEARBY PLAYERS", x, y, 0xFF8AA8C4);
        for (var target : mc.level.players()) {
            if (target == mc.player || mc.player.distanceToSqr(target) > distance.get() * distance.get()) continue;
            y += 12;
            if (y > graphics.guiHeight() - 15) break;
            int color = RiftDLC.instance().friends().contains(target.getName().getString()) ? 0xFF82DAA7 : 0xFFE2E7ED;
            graphics.text(mc.font, target.getName().getString() + "  " + (int) Math.sqrt(mc.player.distanceToSqr(target)) + "m", x, y, color);
        }
    }
}
