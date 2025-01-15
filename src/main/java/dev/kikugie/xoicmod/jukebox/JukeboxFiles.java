package dev.kikugie.xoicmod.jukebox;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.Item;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import dev.kikugie.xoicmod.javanbs.NBSReader;
import dev.kikugie.xoicmod.javanbs.NBSNote;
import dev.kikugie.xoicmod.javanbs.NBSSong;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class JukeboxFiles {
    private static final Deflater COMPRESSOR = new Deflater(9);
    private static final Inflater DECOMPRESSOR = new Inflater();
    private static final Logger LOGGER = LoggerFactory.getLogger(JukeboxFiles.class);
    private static final Path CONFIG = FabricLoader.getInstance().getConfigDir().resolve("xoid_jukebox");
    private static final List<String> DEFAULT_MAPPINGS = List.of(
        "white_stained_glass",
        "magenta_stained_glass",
        "brown_wool",
        "brown_stained_glass",
        "red_wool",
        "red_stained_glass",
        "orange_wool",
        "yellow_wool",
        "yellow_stained_glass",
        "lime_wool",
        "lime_stained_glass",
        "light_blue_wool",
        "magenta_wool",
        "magenta_stained_glass_pane",
        "brown_concrete",
        "brown_stained_glass_pane",
        "red_concrete",
        "red_stained_glass_pane",
        "orange_concrete",
        "yellow_concrete",
        "yellow_stained_glass_pane",
        "lime_concrete",
        "lime_stained_glass_pane",
        "light_blue_concrete",
        "magenta_concrete",
        "magenta_dye"
    );

    static {
        try {
            Files.createDirectories(CONFIG);
        } catch (Exception e) {
            LOGGER.error("Failed to create config directory", e);
        }
    }

    public static Item[] readMappings(RegistryEntryLookup<Item> lookup) throws Exception {
        Path mappingsFile = CONFIG.resolve("mappings.json");
        List<String> mappings;
        if (Files.exists(mappingsFile))
            mappings = Files.readAllLines(mappingsFile, Charset.defaultCharset());
        else {
            writeDefaultMappings();
            mappings = DEFAULT_MAPPINGS;
        }
        return decodeMappings(mappings, lookup);
    }

    private static void writeDefaultMappings() throws Exception {
        Path mappingsFile = CONFIG.resolve("mappings.txt");
        Files.write(mappingsFile, JukeboxFiles.DEFAULT_MAPPINGS, Charset.defaultCharset(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private static Item[] decodeMappings(List<String> mappings, RegistryEntryLookup<Item> lookup) throws Exception {
        Item[] items = new Item[mappings.size()];
        for (int i = 0; i < mappings.size(); i++) {
            Identifier id = Identifier.of(mappings.get(i));
            RegistryEntry<Item> entry = lookup.getOrThrow(RegistryKey.of(RegistryKeys.ITEM, id));
            items[i] = entry.value();
        }
        return items;
    }

    public static JukeboxSong parseNbs(Path file, JukeboxSong.State state) throws Exception {
        NBSSong song = NBSReader.readSong(file.toString());

        byte[][] notes = new byte[6][song.getHeader().getLength()];
        for (byte[] bytes : notes) Arrays.fill(bytes, (byte) -1);

        for (NBSNote note : song.getNotes()) {
            if (note.getLayer() > 5 || note.getTick() >= song.getHeader().getLength()) continue;
            notes[note.getLayer()][note.getTick()] = (byte) (note.getKey() - 33);
        }
        return new JukeboxSong(notes, state);
    }

    public static void updateSongState(JukeboxSong song) throws Exception {
        Path songFile = CONFIG.resolve("current.jukebox");
        if (!Files.exists(songFile)) {
            writeSongState(song);
            return;
        }

        // First 4 bytes are assigned to the paused state, track and offset info
        ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put((byte) (song.paused ? 1 : 0));
        buffer.put(song.current.track());
        buffer.putShort(song.current.offset());

        // Modify only the required bytes
        try (RandomAccessFile file = new RandomAccessFile(songFile.toFile(), "rwd")) {
            file.write(buffer.array());
        }
    }

    public static void cleanUpSong() throws Exception {
        Path songFile = CONFIG.resolve("current.jukebox");
        Files.deleteIfExists(songFile);
    }

    public static void writeSongState(JukeboxSong song) throws Exception {
        Path songFile = CONFIG.resolve("current.jukebox");
        // Flatten notes into one array
        byte[] flattened = new byte[song.notes[0].length * song.notes.length];
        for (int i = 0; i < song.notes.length; i++)
            System.arraycopy(song.notes[i], 0, flattened, i * song.notes[0].length, song.notes[i].length);

        // Compress notes
        COMPRESSOR.setInput(flattened);
        COMPRESSOR.finish();
        byte[] compressed = new byte[flattened.length];
        int length = COMPRESSOR.deflate(compressed);
        COMPRESSOR.reset();
        if (length == 0) throw new IllegalStateException("Failed to compress song");
        LOGGER.info("Compressed song %d -> %d bytes".formatted(flattened.length, length));

        // Write song state and compressed notes
        ByteBuffer buffer = ByteBuffer.allocate(length + 4).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put((byte) (song.paused ? 1 : 0));
        buffer.put(song.current.track());
        buffer.putShort(song.current.offset());
        buffer.put(compressed, 0, length);

        Files.write(songFile, buffer.array(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    public static @Nullable JukeboxSong readSongState() throws Exception {
        Path songFile = CONFIG.resolve("current.jukebox");
        if (!Files.exists(songFile)) return null;

        // Read track state and note data
        ByteBuffer buffer = ByteBuffer.wrap(Files.readAllBytes(songFile)).order(ByteOrder.LITTLE_ENDIAN);
        boolean paused = buffer.get() == 1;
        byte track = buffer.get();
        short offset = buffer.getShort();
        byte[] compressed = new byte[buffer.remaining()];
        buffer.get(compressed);

        // Decompress notes
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        DECOMPRESSOR.setInput(compressed);
        int length = 0;
        byte[] temp = new byte[1024];
        while (!DECOMPRESSOR.finished()) {
            int count = DECOMPRESSOR.inflate(temp);
            stream.write(temp, 0, count);
            length += count;
        }
        DECOMPRESSOR.reset();
        if (length % 6 != 0) throw new IllegalStateException("Invalid song length %d".formatted(length));
        LOGGER.info("Decompressed song %d -> %d bytes".formatted(compressed.length, length));

        // Unwrap notes into 6 tracks
        byte[] flattened = stream.toByteArray();
        byte[][] notes = new byte[6][length / 6];
        for (int i = 0; i < notes.length; i++)
            System.arraycopy(flattened, i * notes[0].length, notes[i], 0, notes[i].length);
        return new JukeboxSong(notes, new JukeboxSong.State(track, offset), paused);
    }
}
