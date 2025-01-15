package dev.kikugie.xoicmod.jukebox;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.item.Item;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;

public class JukeboxCommand {
    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandRegistryAccess access) {
        RegistryEntryLookup<Item> lookup = access.getWrapperOrThrow(RegistryKeys.ITEM);
        dispatcher.register(literal("jukebox")
            .then(literal("query")
                .executes(JukeboxCommand::query))
            .then(literal("pause")
                .executes(JukeboxCommand::pause))
            .then(literal("reload")
                .executes(context -> reload(context, lookup)))
            .then(literal("unload")
                .executes(JukeboxCommand::unload))
            .then(literal("load")
                .executes(context -> open(location -> load(context, location, 0, 3)))
                .then(argument("file", StringArgumentType.string())
                    .executes(context -> {
                        String location = StringArgumentType.getString(context, "file");
                        return load(context, location, 0, 3);
                    })
                    .then(argument("track", IntegerArgumentType.integer(0, 5))
                        .then(argument("offset", IntegerArgumentType.integer(0, Short.MAX_VALUE))
                            .executes(context -> {
                                String location = StringArgumentType.getString(context, "file");
                                int track = IntegerArgumentType.getInteger(context, "track");
                                int offset = IntegerArgumentType.getInteger(context, "offset");
                                return load(context, location, track, offset);
                            }))))
                .then(argument("track", IntegerArgumentType.integer(0, 5))
                    .then(argument("offset", IntegerArgumentType.integer(0, Short.MAX_VALUE))
                        .executes(context -> {
                            int track = IntegerArgumentType.getInteger(context, "track");
                            int offset = IntegerArgumentType.getInteger(context, "offset");
                            return open(location -> load(context, location, track, offset));
                        }))))
        );
    }

    private static int pause(CommandContext<FabricClientCommandSource> context) {
        if (JukeboxManager.current == null) error(context, "No song loaded");
        else {
            boolean state = !JukeboxManager.current.paused;
            JukeboxManager.current.paused = state;
            if (state) success(context, "Paused song filling");
            else success(context, "Resumed song filling");
        }
        return 0;
    }

    private static int query(CommandContext<FabricClientCommandSource> context) {
        JukeboxSong song = JukeboxManager.current;
        if (song == null) error(context, "No song loaded");
        else {
            StringBuilder text = new StringBuilder();
            text.append("Current song position: [%d %d]".formatted(song.current.track(), song.current.offset()));
            if (JukeboxManager.current.paused) text.append(" (paused)");
            success(context, text.toString());
        }
        return 0;
    }

    private static int unload(CommandContext<FabricClientCommandSource> context) {
        try {
            JukeboxManager.current = null;
            JukeboxFiles.cleanUpSong();
            success(context, "Cleared song state");
        } catch (Exception e) {
            error(context, e);
        }
        return 0;
    }

    private static int reload(CommandContext<FabricClientCommandSource> context, RegistryEntryLookup<Item> lookup) {
        try {
            JukeboxManager.mappings = JukeboxFiles.readMappings(lookup);
            success(context, "Loaded mappings");

            JukeboxSong saved = JukeboxFiles.readSongState();
            if (saved != null) {
                JukeboxManager.current = saved;
                success(context, "Loaded song state");
            }
        } catch (Exception e) {
            error(context, e);
        }
        return 0;
    }

    private static int load(CommandContext<FabricClientCommandSource> context, String location, int track, int offset) {
        try {
            if (JukeboxManager.mappings == null || JukeboxManager.mappings.length == 0)
                throw new IllegalStateException("Mappings are not loaded");

            Path file = Path.of(location);
            if (Files.notExists(file))
                throw new IllegalArgumentException("File %s does not exist".formatted(location));

            JukeboxSong.State state = new JukeboxSong.State((byte) track, (short) offset);
            JukeboxManager.current = JukeboxFiles.parseNbs(file, state);
            JukeboxFiles.writeSongState(JukeboxManager.current);
            success(context, "Loaded song %s at [%d %d]".formatted(file, track, offset));
        } catch (Exception e) {
            error(context, e);
        }
        return 0;
    }

    private static int open(Consumer<String> callback) {
        CompletableFuture.supplyAsync(JukeboxCommand::open).thenAccept(callback);
        return 0;
    }

    private static String open() {
        PointerBuffer filters;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            filters = stack.mallocPointer(1);
            filters.put(stack.UTF8("*.nbs"));
            filters.flip();
            return TinyFileDialogs.tinyfd_openFileDialog(
                "Select a song file",
                FabricLoader.getInstance().getGameDir().toString(),
                filters,
                "OpenNBS files",
                false
            );
        }
    }

    private static void error(CommandContext<FabricClientCommandSource> context, Exception e) {
        context.getSource().sendError(Text.of("[Jukebox] " + e.getMessage()));
        JukeboxManager.LOGGER.error("Error while executing command", e);
    }

    private static void error(CommandContext<FabricClientCommandSource> context, String message) {
        context.getSource().sendError(Text.of("[Jukebox] " + message));
    }

    private static void success(CommandContext<FabricClientCommandSource> context, String message) {
        context.getSource().sendFeedback(Text.of("[Jukebox] " + message));
    }
}
