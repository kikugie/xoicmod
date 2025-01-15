package dev.kikugie.xoicmod.jukebox;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@SuppressWarnings("DataFlowIssue")
public class JukeboxManager {
    static final Logger LOGGER = LoggerFactory.getLogger(JukeboxManager.class);
    static Item[] mappings = new Item[0];
    static JukeboxSong current = null;

    public static void handle(ShulkerBoxScreen screen) {
        if (current != null && !current.paused) handleShulkerBox(screen, current);
    }

    private static void handleShulkerBox(ShulkerBoxScreen screen, JukeboxSong song) {
        try {
            handleImpl(screen, song);
        } catch (Exception e) {
            song.restore();
            error(e.getMessage());
            LOGGER.error("Failed to handle screen", e);
        }
    }

    private static void handleImpl(ShulkerBoxScreen screen, JukeboxSong song) throws Exception {
        JukeboxSong.State state = song.current;
        JukeboxSong.Selection next = song.next();
        verifyInventory(next.items());

        log(state, next);
        Map<Integer, List<Integer>> queue = composeMoveActions(next.items());
        moveItems(screen.getScreenHandler(), queue);

        if (next.advanced()) song.paused = true;
        JukeboxFiles.updateSongState(song);

        if (!next.advanced()) return;
        if (song.hasNext()) error("Advanced to track %d, pausing".formatted(song.current.track()));
        else {
            error("Finished reading song data, resetting");
            JukeboxFiles.cleanUpSong();
            current = null;
        }
    }

    private static void moveItems(ScreenHandler handler, Map<Integer, List<Integer>> queue) {
        int sync = handler.syncId;
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        ClientPlayerInteractionManager interaction = MinecraftClient.getInstance().interactionManager;

        // Reassign the queue to have slot ids instead of indexes
        Map<Integer, Integer> mappings = new Int2IntOpenHashMap();
        Map<Integer, List<Integer>> slots = new Int2ObjectOpenHashMap<>();
        for (Slot slot : handler.slots)
            if (slot.inventory instanceof PlayerInventory) {
                List<Integer> destinations = queue.get(slot.getIndex());
                if (destinations != null) slots.put(slot.id, destinations);
            } else mappings.put(slot.getIndex(), slot.id);

        // Move items to slots
        for (Map.Entry<Integer, List<Integer>> entry : slots.entrySet()) {
            int source = entry.getKey();
            interaction.clickSlot(sync, source, 0, SlotActionType.PICKUP, player);
            for (int dest : entry.getValue())
                interaction.clickSlot(sync, mappings.get(dest), 1, SlotActionType.PICKUP, player);
            interaction.clickSlot(sync, source, 0, SlotActionType.PICKUP, player);
        }
    }

    private static Map<Integer, List<Integer>> composeMoveActions(Item[] notes) {
        // Make a list to get `indexOf` and to make reassigning items safe
        List<Item> copy = Lists.newArrayList(notes);
        // Represents inventory slots assigned to the positions in the shulker box the item has to go to
        Map<Integer, List<Integer>> queue = new Int2ObjectArrayMap<>();

        // Filter non-empty and required items to reduce sorting
        Inventory inventory = MinecraftClient.getInstance().player.getInventory();
        List<SlotStack> candidates = Lists.newArrayList();
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            Item item = stack.getItem();
            if (stack.isEmpty() || !copy.contains(item)) continue;

            candidates.add(new SlotStack(i, stack));
        }
        candidates.sort(null);

        // Get slots for matching items, preferring larger stacks for fewer clicks
        for (SlotStack stack : candidates.reversed()) {
            Item item = stack.getItem();
            int count = stack.getCount();
            int index = copy.indexOf(item);
            List<Integer> destinations = Lists.newArrayList();
            while (index != -1 && count > 0) {
                destinations.add(index);
                copy.set(index, null);
                count--;
                index = copy.indexOf(item);
            }

            if (!destinations.isEmpty())
                queue.put(stack.slot, destinations);
        }
        return queue;
    }

    private static void verifyInventory(Item[] notes) throws Exception {
        Inventory inventory = MinecraftClient.getInstance().player.getInventory();
        // Count how much of each item is required
        Map<Item, Integer> required = new Object2IntArrayMap<>(notes.length);
        for (Item it : notes) required.put(it, required.getOrDefault(it, 0) + 1);

        // Scan the player inventory, decreasing counts for matching items
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            Item item = stack.getItem();
            if (stack.isEmpty() || !required.containsKey(item)) continue;

            int modified = required.get(item) - stack.getCount();
            if (modified <= 0) required.remove(item);
            else required.put(item, modified);
        }

        // Compose an error message containing all remaining items
        if (!required.isEmpty()) {
            String[] missing = new String[required.size()];
            int i = 0;
            for (Map.Entry<Item, Integer> entry : required.entrySet())
                missing[i++] = "- %s: %d".formatted(entry.getKey().getName().getString(), entry.getValue());
            String message = "Missing items:\n%s".formatted(String.join(",\n", missing));
            throw new IllegalStateException(message);
        }
    }

    private static void log(JukeboxSong.State state, JukeboxSong.Selection selection) {
        StringBuilder builder = new StringBuilder();
        builder.append("Inserting at [%d %d]:".formatted(state.track(), state.offset()));

        int length = 0;
        StringBuilder temp = new StringBuilder();
        List<String> items = new ArrayList<>(selection.items().length);
        for (Item item : selection.items()) {
            String name = item.getName().getString();
            for (char c : name.toCharArray())
                if (Character.isUpperCase(c))
                    temp.append(c);
            length = Math.max(length, temp.length());
            items.add(temp.toString());
            temp.setLength(0);
        }

        String padding = " ".repeat(length);
        String[] entries = new String[9];
        for (int i = 0; i < items.size(); i++) {
            int mod = i % 9;
            if (mod == 0) Arrays.fill(entries, padding);
            entries[mod] = items.get(i) + " ".repeat(length - items.get(i).length());
            if (mod == 8 || i == items.size() - 1)
                builder.append("\n  [%s]".formatted(String.join(", ", entries)));
        }
        LOGGER.info(builder.toString());
    }

    @SuppressWarnings("DataFlowIssue")
    private static void error(String message) {
        MinecraftClient.getInstance().player.sendMessage(Text.of("§4[Jukebox]: " + message));
    }

    private record SlotStack(int slot, ItemStack stack) implements Comparable<SlotStack> {
        public Item getItem() {
            return stack.getItem();
        }

        public int getCount() {
            return stack.getCount();
        }

        @Override
        public int compareTo(@NotNull JukeboxManager.SlotStack o) {
            return Integer.compare(getCount(), o.getCount());
        }
    }
}
