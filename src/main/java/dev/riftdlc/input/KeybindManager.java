package dev.riftdlc.input;

import dev.riftdlc.RiftDLC;
import dev.riftdlc.core.RiftModule;
import dev.riftdlc.ui.RiftScreen;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.platform.InputConstants;

/** Handles press edges once, on the Minecraft thread. */
public final class KeybindManager {
    private int insert = InputConstants.KEY_INSERT;
    private int secondary = InputConstants.KEY_RSHIFT;
    private final RiftDLC app;
    public KeybindManager(RiftDLC app) { this.app = app; }
    public int insert() { return insert; }
    public int secondary() { return secondary; }
    public void setGuiKeys(int first, int second) { insert = first; secondary = second; }
    public static boolean isPress(int action) { return action == 1; }
    public enum GuiAction { NONE, OPEN, CLOSE }
    public GuiAction guiAction(int key, int action, boolean gameplay, boolean riftScreen) {
        if (!isPress(action) || key != insert && key != secondary) return GuiAction.NONE;
        if (riftScreen) return GuiAction.CLOSE;
        return gameplay ? GuiAction.OPEN : GuiAction.NONE;
    }
    public void key(int key, int action) {
        if (!isPress(action)) return;
        Minecraft mc = Minecraft.getInstance();
        GuiAction guiAction = guiAction(key, action, mc.gui.screen() == null, mc.gui.screen() instanceof RiftScreen);
        if (guiAction != GuiAction.NONE) {
            if (guiAction == GuiAction.CLOSE) mc.gui.screen().onClose();
            else mc.gui.setScreen(new RiftScreen(app, null));
            return;
        }
        if (key == insert || key == secondary) return;
        if (mc.gui.screen() != null || mc.player == null) return;
        for (RiftModule module : app.modules().all()) if (module.bind() == key) {
            module.setEnabled(!module.enabled());
            app.save();
        }
    }
}
