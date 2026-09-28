package dev.riftdlc.mixin;

import dev.riftdlc.RiftDLC;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public abstract class KeyboardMixin {
    @Inject(method = "keyPress", at = @At("HEAD"))
    private void riftdlc$key(long window, int action, KeyEvent event, CallbackInfo ci) {
        RiftDLC app = RiftDLC.instance();
        if (app != null && window == Minecraft.getInstance().getWindow().handle())
            Minecraft.getInstance().execute(() -> app.keybinds().key(event.key(), action));
    }
}
