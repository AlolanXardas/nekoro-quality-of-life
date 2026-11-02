package dev.xardas.nekoroqol.inventory;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.FurnaceResultSlot;
import net.minecraft.world.inventory.MerchantResultSlot;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Splits the slots of an open screen into groups: the player's hotbar, the player's main inventory (the three
 * rows above the hotbar), armor/offhand, and every other container (a chest, a backpack...). Used on both sides,
 * so the client shows buttons for the same slots the server sorts.
 */
public final class SlotGroups {
    public static final int HOTBAR = 0;
    public static final int MAIN = 1;
    public static final int EQUIPMENT = 2;
    public static final int OTHER = 3;

    /** Storage smaller than this gets no sort buttons (furnaces, hoppers, brewing stands...). */
    public static final int MIN_STORAGE_SLOTS = 9;

    private static final ItemStack PLAIN_ITEM = new ItemStack(Items.COBBLESTONE);

    private SlotGroups() { }

    public static boolean isPlayer(Slot slot) {
        return slot.container instanceof Inventory;
    }

    public static int part(Slot slot) {
        if (!isPlayer(slot)) return OTHER;
        int i = slot.getContainerSlot();
        if (i < 9) return HOTBAR;
        if (i < 36) return MAIN;
        return EQUIPMENT;
    }

    /** Crafting results, furnace output and trade results: clicking them does more than move items. */
    public static boolean isResult(Slot slot) {
        return slot instanceof ResultSlot || slot instanceof FurnaceResultSlot || slot instanceof MerchantResultSlot;
    }

    /** Slots in the same container and the same part of the player inventory as this one, results left out. */
    public static List<Slot> sameGroup(AbstractContainerMenu menu, Slot slot) {
        List<Slot> out = new ArrayList<>();
        int part = part(slot);
        for (Slot other : menu.slots) {
            if (other.container == slot.container && part(other) == part && !isResult(other)) out.add(other);
        }
        return out;
    }

    public static List<Slot> playerMain(AbstractContainerMenu menu) {
        List<Slot> out = new ArrayList<>();
        for (Slot slot : menu.slots) if (part(slot) == MAIN) out.add(slot);
        return out;
    }

    /**
     * Plain storage slots of this container: no results, no crafting grid, and only slots that take any item
     * (a horse's saddle slot or a furnace's fuel slot do not).
     */
    public static List<Slot> storage(AbstractContainerMenu menu, Container container) {
        List<Slot> out = new ArrayList<>();
        if (container instanceof Inventory || container instanceof CraftingContainer) return out;
        for (Slot slot : menu.slots) {
            if (slot.container == container && !isResult(slot) && slot.mayPlace(PLAIN_ITEM)) out.add(slot);
        }
        return out;
    }

    /** Every storage container in the menu with at least {@link #MIN_STORAGE_SLOTS} plain slots. */
    public static List<List<Slot>> storages(AbstractContainerMenu menu) {
        List<List<Slot>> out = new ArrayList<>();
        List<Container> seen = new ArrayList<>();
        for (Slot slot : menu.slots) {
            if (seen.contains(slot.container)) continue;
            seen.add(slot.container);
            List<Slot> group = storage(menu, slot.container);
            if (group.size() >= MIN_STORAGE_SLOTS) out.add(group);
        }
        return out;
    }
}
