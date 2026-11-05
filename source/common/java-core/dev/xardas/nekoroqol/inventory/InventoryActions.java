package dev.xardas.nekoroqol.inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import dev.xardas.nekoroqol.QolConfig;

/**
 * The server side of the sort and transfer buttons. Works on the slots of the open screen, so any container
 * works, and the hotbar is never touched.
 */
public final class InventoryActions {
    public static final int SORT = 0;
    public static final int MATCHING_TO_STORAGE = 1;
    public static final int ALL_TO_PLAYER = 2;
    public static final int ALL_TO_STORAGE = 3;
    public static final int MATCHING_TO_PLAYER = 4;

    private InventoryActions() { }

    public static void handle(ServerPlayer player, InventoryPayload payload) {
        QolConfig cfg = QolConfig.get();
        if (!cfg.inventoryButtons) return;
        AbstractContainerMenu menu = player.containerMenu;
        if (menu.containerId != payload.containerId() || !menu.stillValid(player) || !menu.getCarried().isEmpty()) return;
        if (payload.slot() < 0 || payload.slot() >= menu.slots.size()) return;
        Slot anchor = menu.getSlot(payload.slot());

        List<Slot> group = switch (SlotGroups.part(anchor)) {
            case SlotGroups.MAIN -> SlotGroups.playerMain(menu);
            case SlotGroups.OTHER -> wholeStorage(menu, anchor.container);
            default -> List.of();
        };
        group = group.stream().filter(s -> s.isActive() && s.mayPickup(player) && s.allowModification(player)).toList();
        if (group.isEmpty()) return;
        boolean storage = SlotGroups.part(anchor) == SlotGroups.OTHER;

        switch (payload.action()) {
            case SORT -> sort(group, SortOrder.Mode.values()[Math.floorMod(payload.mode(), SortOrder.Mode.values().length)], cfg);
            case MATCHING_TO_STORAGE -> { if (storage) moveMatching(SlotGroups.playerMain(menu), group, player); }
            case ALL_TO_PLAYER -> { if (storage) moveAll(group, SlotGroups.playerMain(menu), player); }
            case ALL_TO_STORAGE -> { if (storage) moveAll(SlotGroups.playerMain(menu), group, player); }
            case MATCHING_TO_PLAYER -> { if (storage) moveMatching(group, SlotGroups.playerMain(menu), player); }
            default -> { }
        }
        menu.broadcastChanges();
    }

    /**
     * The slots of a storage. A window that shows only a part of something bigger (a bag that scrolls) can have a
     * container that is also a Supplier of slots for all of it, so the buttons work on the whole storage.
     */
    @SuppressWarnings("unchecked")
    private static List<Slot> wholeStorage(AbstractContainerMenu menu, Container container) {
        if (container instanceof Supplier<?> supplier && supplier.get() instanceof List<?> list
                && !list.isEmpty() && list.get(0) instanceof Slot) {
            return (List<Slot>) list;
        }
        return SlotGroups.storage(menu, container);
    }

    /** Joins equal stacks, sorts them and puts them back from the first slot on. Changes nothing if they do not fit. */
    private static void sort(List<Slot> slots, SortOrder.Mode mode, QolConfig cfg) {
        List<ItemStack> items = new ArrayList<>();
        for (Slot slot : slots) if (slot.hasItem()) merge(items, slot.getItem().copy());
        items.sort(SortOrder.comparator(mode, cfg.sortCategories));

        ItemStack[] result = new ItemStack[slots.size()];
        int next = 0;
        for (ItemStack stack : items) {
            while (!stack.isEmpty()) {
                if (next >= slots.size()) return;
                Slot slot = slots.get(next);
                if (slot.mayPlace(stack)) {
                    result[next] = stack.split(Math.min(stack.getCount(), slot.getMaxStackSize(stack)));
                }
                next++;
            }
        }
        for (int i = 0; i < slots.size(); i++) slots.get(i).set(result[i] == null ? ItemStack.EMPTY : result[i]);
    }

    private static void merge(List<ItemStack> items, ItemStack stack) {
        for (ItemStack existing : items) {
            if (stack.isEmpty()) return;
            if (!ItemStack.isSameItemSameComponents(existing, stack)) continue;
            int add = Math.min(stack.getCount(), existing.getMaxStackSize() - existing.getCount());
            if (add <= 0) continue;
            existing.grow(add);
            stack.shrink(add);
        }
        if (!stack.isEmpty()) items.add(stack);
    }

    /** Moves only the kinds of items the other side already has. */
    private static void moveMatching(List<Slot> from, List<Slot> into, ServerPlayer player) {
        List<Slot> sources = new ArrayList<>();
        for (Slot slot : from) {
            if (!slot.hasItem() || !slot.mayPickup(player)) continue;
            for (Slot target : into) {
                if (target.hasItem() && target.getItem().is(slot.getItem().getItem())) {
                    sources.add(slot);
                    break;
                }
            }
        }
        moveAll(sources, into, player);
    }

    private static void moveAll(List<Slot> from, List<Slot> into, ServerPlayer player) {
        for (Slot slot : from) {
            if (!slot.hasItem() || !slot.mayPickup(player)) continue;
            ItemStack rest = insert(slot.getItem().copy(), into);
            if (rest.getCount() != slot.getItem().getCount()) slot.set(rest);
        }
    }

    /** Tops up equal stacks first, then fills empty slots. Returns what did not fit. */
    private static ItemStack insert(ItemStack stack, List<Slot> into) {
        for (Slot target : into) {
            if (stack.isEmpty()) return stack;
            ItemStack there = target.getItem();
            if (there.isEmpty() || !ItemStack.isSameItemSameComponents(there, stack) || !target.mayPlace(stack)) continue;
            int add = Math.min(stack.getCount(), target.getMaxStackSize(stack) - there.getCount());
            if (add <= 0) continue;
            ItemStack grown = there.copy();
            grown.grow(add);
            target.set(grown);
            stack.shrink(add);
        }
        for (Slot target : into) {
            if (stack.isEmpty()) return stack;
            if (target.hasItem() || !target.mayPlace(stack)) continue;
            target.set(stack.split(Math.min(stack.getCount(), target.getMaxStackSize(stack))));
        }
        return stack;
    }
}
