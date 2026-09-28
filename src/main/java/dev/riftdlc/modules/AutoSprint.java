package dev.riftdlc.modules;

import dev.riftdlc.core.RiftModule;
import net.minecraft.client.Minecraft;

public final class AutoSprint extends RiftModule {
    public AutoSprint() {
        super("auto_sprint", "Auto Sprint", "Sprint while moving forward", Category.MOVEMENT);
    }
    @Override public void tick() {
        var player = Minecraft.getInstance().player;
        if (player != null && player.input != null && player.input.keyPresses.forward()
                && !player.isSprinting() && !player.isCrouching() && player.getFoodData().getFoodLevel() > 6)
            player.setSprinting(true);
    }
}
