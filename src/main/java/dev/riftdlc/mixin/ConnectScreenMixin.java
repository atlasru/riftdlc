package dev.riftdlc.mixin;

import dev.riftdlc.RiftDLC;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.TransferState;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Apply profile to ServerData before ViaFabricPlus starts the connection. */
@Mixin(ConnectScreen.class)
public abstract class ConnectScreenMixin {
    @Inject(method = "startConnecting", at = @At("HEAD"))
    private static void riftdlc$profile(Screen parent, Minecraft client, ServerAddress address,
                                        ServerData server, boolean quickPlay, TransferState transfer,
                                        CallbackInfo ci) {
        RiftDLC app = RiftDLC.instance();
        if (app != null && server != null && app.profiles().has(server.ip))
            app.protocols().setServerVersion(server, app.profiles().get(server.ip));
    }
}
