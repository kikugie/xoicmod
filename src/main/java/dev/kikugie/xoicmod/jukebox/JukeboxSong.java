package dev.kikugie.xoicmod.jukebox;

import net.minecraft.item.Item;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;

public class JukeboxSong implements Iterator<JukeboxSong.Selection> {
    final Item[] mappings = JukeboxManager.mappings;
    final byte[][] notes;

    State current;
    State previous;
    boolean paused = false;

    JukeboxSong(byte[][] notes, State state, boolean paused) {
        this(notes, state);
        this.paused = paused;
    }

    JukeboxSong(byte[][] notes, State state) {
        this.notes = notes;
        this.current = state;
        this.previous = state;
    }

    public record State(byte track, short offset) {
    }

    public record Selection(Item[] items, boolean advanced) {
    }

    public void backup() {
        previous = current;
    }

    public void restore() {
        current = previous;
    }

    @Override
    public boolean hasNext() {
        return current.track < 6;
    }

    @Override
    public Selection next() {
        if (current.track > 5) throw new IllegalStateException("No more notes in the song");
        backup();

        final byte[] track = notes[current.track];
        final Item[] items = new Item[27];
        Arrays.fill(items, mappings[0]);

        byte j = 0;
        for (int i = current.offset; i < track.length; i += 4) {
            if (j >= notes.length) break;
            byte note = track[i];
            if (note != -1) items[j] = Objects.requireNonNull(mappings[note + 1], "Invalid note id: %d".formatted(note + 1));
            j++;
        }

        return new Selection(items, advance());
    }

    private boolean advance() {
        boolean advanced = false;
        short offset = current.offset;
        byte track = current.track;

        // Jukebox groups notes into 4 shulker boxes -> 27 * 4 = 108,
        // which then should be shuffled in the order [4, 2, 3, 1]
        switch (offset % 4) {
            case 3, 2 -> offset -= 2;
            case 1 -> offset += 1;
            case 0 -> offset += 108 + 3;
        }

        // Advance the track if the next position is out of bounds
        if (offset >= notes[track].length) {
            advanced = true;
            offset = 3;
            track += 1;
        }

        current = new State(track, offset);
        return advanced;
    }
}
