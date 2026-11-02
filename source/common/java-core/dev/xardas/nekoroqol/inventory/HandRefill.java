package dev.xardas.nekoroqol.inventory;

import java.util.EnumMap;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import dev.xardas.nekoroqol.QolConfig;

/**
 * When the item in a hand is used up (a tool breaks, the last block is placed, the last food eaten, the last
 * Poke Ball thrown), the same item from the inventory takes its place. Only uses count: moving or dropping
 * an item by hand never pulls a new one in.
 */
public final class HandRefill {
    private static final Map<Player, EnumMap<InteractionHand, ItemStack>> USED = new WeakHashMap<>();

    private HandRefill() { }

    /** A hand item was used (right click, block use, eaten, broke); checked again after the player tick. */
    public static void used(Player player, InteractionHand hand, ItemStack stack) {
        if (hand == null || stack.isEmpty() || player.level().isClientSide() || player.hasInfiniteMaterials()) return;
        if (!QolConfig.get().refillHand) return;
        USED.computeIfAbsent(player, p -> new EnumMap<>(InteractionHand.class)).put(hand, stack.copyWithCount(1));
    }

    /** After the player tick. */
    public static void refill(Player player) {
        EnumMap<InteractionHand, ItemStack> used = USED.remove(player);
        if (used == null) return;
        used.forEach((hand, stack) -> {
            if (!player.getItemInHand(hand).isEmpty()) return;
            Inventory inv = player.getInventory();
            int from = find(inv, stack, hand == InteractionHand.MAIN_HAND ? inv.selected : -1);
            if (from < 0) return;
            player.setItemInHand(hand, inv.getItem(from));
            inv.setItem(from, ItemStack.EMPTY);
        });
    }

    /** The same item, main inventory first, then the hotbar. Tools may differ in damage and enchantments. */
    private static int find(Inventory inv, ItemStack wanted, int skip) {
        int found = -1;
        for (int pass = 0; pass < 2 && found < 0; pass++) {
            boolean exact = pass == 0;
            if (!exact && !wanted.isDamageableItem()) break;
            for (int n = 0; n < 36; n++) {
                int i = (n + 9) % 36;
                if (i == skip) continue;
                ItemStack there = inv.getItem(i);
                if (there.isEmpty()) continue;
                if (exact ? ItemStack.isSameItemSameComponents(there, wanted) : there.is(wanted.getItem())) {
                    found = i;
                    break;
                }
            }
        }
        return found;
    }
}
