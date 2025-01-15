package dev.kikugie.xoicmod.mixin;

import dev.kikugie.xoicmod.ProcessableScreen;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Screen.class)
public class ScreenMixin implements ProcessableScreen {

	@Unique
	private boolean process;

	@Override
	public void xoicmod$setShouldProcess(boolean value) {
		this.process = value;
	}

	@Override
	public boolean xoicmod$shouldProcess() {
		return this.process;
	}
}
