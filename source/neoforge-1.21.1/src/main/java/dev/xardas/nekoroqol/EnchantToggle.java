package dev.xardas.nekoroqol;

import java.util.List;
import java.util.function.Supplier;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Switching single enchantments off and on. A switched-off enchantment is moved from the item's normal
 * enchantments into a second list (the "disabled_enchantments" component), so the game simply does not
 * see it: no effect, no glint if it was the only one, and the anvil lets you add an enchantment that
 * would clash with it (that is how Fortune and Silk Touch end up on one tool). Switching it on moves it back.
 */
public final class EnchantToggle {
    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, NekoroQol.MOD_ID);

    public static final Supplier<DataComponentType<ItemEnchantments>> DISABLED = COMPONENTS.registerComponentType(
            "disabled_enchantments", builder -> builder.persistent(ItemEnchantments.CODEC)
                    .networkSynchronized(ItemEnchantments.STREAM_CODEC));

    private EnchantToggle() { }

    /** Items whose enchantments can be switched: anything enchanted except books (they only store enchantments). */
    public static boolean canToggle(ItemStack stack) {
        return !stack.isEmpty() && !stack.is(Items.ENCHANTED_BOOK) && !stack.is(Items.BOOK);
    }

    /** Where a toggle looks for the enchantment, in this order: the menu works on the main hand, quick keys on all. */
    public static List<ItemStack> searchOrder(Player player) {
        return List.of(player.getMainHandItem(), player.getOffhandItem(), player.getItemBySlot(EquipmentSlot.FEET),
                player.getItemBySlot(EquipmentSlot.LEGS), player.getItemBySlot(EquipmentSlot.CHEST),
                player.getItemBySlot(EquipmentSlot.HEAD));
    }

    static void toggle(ServerPlayer player, ResourceLocation id) {
        if (!QolConfig.get().toggleEnchantments) return;
        Holder<Enchantment> enchantment = player.registryAccess().registryOrThrow(Registries.ENCHANTMENT)
                .getHolder(ResourceKey.create(Registries.ENCHANTMENT, id)).orElse(null);
        // curses stay, otherwise Curse of Binding or Vanishing could simply be switched off
        if (enchantment == null || enchantment.is(EnchantmentTags.CURSE)) return;

        for (ItemStack stack : searchOrder(player)) {
            if (!canToggle(stack)) continue;
            ItemEnchantments active = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
            ItemEnchantments disabled = stack.getOrDefault(DISABLED.get(), ItemEnchantments.EMPTY);
            if (active.getLevel(enchantment) > 0 || disabled.getLevel(enchantment) > 0) {
                toggle(player, stack, enchantment, active, disabled);
                return;
            }
        }
    }

    private static void toggle(ServerPlayer player, ItemStack stack, Holder<Enchantment> enchantment,
                               ItemEnchantments active, ItemEnchantments disabled) {
        boolean switchingOff = active.getLevel(enchantment) > 0;
        int level = switchingOff ? active.getLevel(enchantment) : disabled.getLevel(enchantment);
        if (level <= 0) return;
        // the same enchantment can be added again with an anvil while it is switched off; keep the higher level
        level = Math.max(level, switchingOff ? disabled.getLevel(enchantment) : active.getLevel(enchantment));

        ItemEnchantments.Mutable from = new ItemEnchantments.Mutable(switchingOff ? active : disabled);
        ItemEnchantments.Mutable to = new ItemEnchantments.Mutable(switchingOff ? disabled : active);
        from.set(enchantment, 0);
        to.set(enchantment, level);
        ItemEnchantments newActive = (switchingOff ? from : to).toImmutable();
        ItemEnchantments newDisabled = (switchingOff ? to : from).toImmutable();
        stack.set(DataComponents.ENCHANTMENTS, newActive);
        if (newDisabled.isEmpty()) stack.remove(DISABLED.get());
        else stack.set(DISABLED.get(), newDisabled);

        Component name = Enchantment.getFullname(enchantment, level);
        player.displayClientMessage(Component.translatable(switchingOff ? "message.nekoro_qol.disabled"
                : "message.nekoro_qol.enabled", name), true);
    }
}
