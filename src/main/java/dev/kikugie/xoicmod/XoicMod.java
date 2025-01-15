package dev.kikugie.xoicmod;

import dev.kikugie.xoicmod.jukebox.JukeboxCommand;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.impl.command.client.ClientCommandInternals;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.*;


public class XoicMod implements ModInitializer {
    private static final String MOD_ID = "xoicmod";
    private static final Logger LOGGER = getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ClientCommandRegistrationCallback.EVENT.register(JukeboxCommand::register);
    }

    @SuppressWarnings("UnstableApiUsage")
    public static void onJoin() {
        ClientCommandInternals.executeCommand("jukebox reload");
    }
}
