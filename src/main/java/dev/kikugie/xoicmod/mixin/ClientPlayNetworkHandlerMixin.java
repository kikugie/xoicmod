package dev.kikugie.xoicmod.mixin;

import dev.kikugie.xoicmod.ProcessableScreen;
import dev.kikugie.xoicmod.XoicMod;
import dev.kikugie.xoicmod.jukebox.JukeboxManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.InventoryS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ClientPlayNetworkHandler.class, priority = 1100)
public class ClientPlayNetworkHandlerMixin {
    @Inject(method = "onInventory", at = @At("TAIL"))
    private void onOnOpenScreen(InventoryS2CPacket packet, CallbackInfo ci) {
        Screen screen = MinecraftClient.getInstance().currentScreen;
        if (screen == null || !((ProcessableScreen) screen).xoicmod$shouldProcess()) return;
        ((ProcessableScreen) screen).xoicmod$setShouldProcess(false);
        JukeboxManager.handle((ShulkerBoxScreen) screen);
    }

    @Inject(method = "onGameJoin", at = @At("RETURN"))
    private void onJoin(CallbackInfo ci) {
        XoicMod.onJoin();
    }
}
