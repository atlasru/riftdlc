package dev.riftdlc.modules;

import dev.riftdlc.core.RiftModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class ServerInfo extends RiftModule {
    public ServerInfo() { super("server_info", "Server Info", "Display server address or singleplayer", Category.RENDER); }
    public void render(GuiGraphicsExtractor graphics) {
        Minecraft mc = Minecraft.getInstance();
        if (!enabled() || mc.player == null || mc.gui.screen() != null) return;
        var server = mc.getCurrentServer();
        String value = server == null ? "Singleplayer" : server.ip;
        graphics.text(mc.font, "SERVER  " + value, 7, graphics.guiHeight() - 18, 0xFF9FB8D0);
    }
}
