package dev.xardas.nekoroqol.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.TooltipFlag;

import dev.xardas.nekoroqol.EnchantToggle;
import dev.xardas.nekoroqol.QolConfig;

/**
 * Tidier item tooltips: enchantments grouped under a heading and coloured, durability on every item that can
 * break, and the real colour of dyed items. Lines from other mods stay where they are.
 */
public final class CleanTooltips {
    private CleanTooltips() { }

    public static void onTooltip(ItemStack stack, List<Component> lines, TooltipFlag flags) {
        QolConfig cfg = QolConfig.get();
        if (cfg.tooltipDyeColor) dyeColor(stack, lines);
        enchantments(stack, lines, cfg);
        if (cfg.tooltipDurability && stack.isDamageableItem() && !flags.isAdvanced()) durability(stack, lines);
    }

    private static void enchantments(ItemStack stack, List<Component> lines, QolConfig cfg) {
        List<Component> disabled = new ArrayList<>();
        ItemEnchantments off = stack.get(EnchantToggle.DISABLED.get());
        if (off != null) {
            for (Object2IntMap.Entry<Holder<Enchantment>> entry : off.entrySet()) {
                Component name = Enchantment.getFullname(entry.getKey(), entry.getIntValue()).copy()
                        .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.STRIKETHROUGH);
                disabled.add(Component.translatable("tooltip.nekoroqualityoflife.disabled", name).withStyle(ChatFormatting.DARK_GRAY));
            }
        }
        if (!cfg.tooltipEnchantments) {
            // switched-off enchantments right under the item name, as before
            lines.addAll(Math.min(1, lines.size()), disabled);
            return;
        }

        int first = -1;
        int last = -1;
        for (ItemEnchantments enchantments : new ItemEnchantments[] {
                stack.get(DataComponents.STORED_ENCHANTMENTS), stack.get(DataComponents.ENCHANTMENTS) }) {
            if (enchantments == null) continue;
            for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
                int at = lines.indexOf(Enchantment.getFullname(entry.getKey(), entry.getIntValue()));
                if (at < 0) continue;
                lines.set(at, Component.literal(" ").append(coloured(entry.getKey(), entry.getIntValue(), cfg)));
                first = first < 0 ? at : Math.min(first, at);
                last = Math.max(last, at);
            }
        }
        if (first < 0 && disabled.isEmpty()) return;
        if (first < 0) {
            // only switched-off enchantments: the section goes right under the item name
            first = Math.min(1, lines.size());
            last = first - 1;
        }
        for (int i = 0; i < disabled.size(); i++) {
            lines.add(last + 1 + i, Component.literal(" ").append(disabled.get(i)));
        }
        lines.add(first, Component.translatable("tooltip.nekoroqualityoflife.enchantments").withStyle(ChatFormatting.GRAY));
    }

    private static Component coloured(Holder<Enchantment> holder, int level, QolConfig cfg) {
        String colour = holder.is(EnchantmentTags.CURSE) ? cfg.curseColor
                : level >= holder.value().getMaxLevel() ? cfg.maxLevelColor : cfg.enchantmentColor;
        MutableComponent name = holder.value().description().copy();
        if (level != 1 || holder.value().getMaxLevel() != 1) {
            name.append(" ").append(Component.translatable("enchantment.level." + level));
        }
        return name.withStyle(Style.EMPTY.withColor(colour(colour, ChatFormatting.GRAY)));
    }

    static TextColor colour(String name, ChatFormatting fallback) {
        TextColor parsed = TextColor.parseColor(name.toLowerCase(Locale.ROOT)).result().orElse(null);
        return parsed != null ? parsed : TextColor.fromLegacyFormat(fallback);
    }

    private static void durability(ItemStack stack, List<Component> lines) {
        int max = stack.getMaxDamage();
        int left = max - stack.getDamageValue();
        float share = max > 0 ? (float) left / max : 1;
        int rgb = Mth.hsvToRgb(share / 3F, 0.8F, 1F);
        Component value = Component.literal(left + " / " + max).withStyle(Style.EMPTY.withColor(rgb));
        lines.add(Component.translatable("tooltip.nekoroqualityoflife.durability", value).withStyle(ChatFormatting.GRAY));
    }

    private static void dyeColor(ItemStack stack, List<Component> lines) {
        DyedItemColor dyed = stack.get(DataComponents.DYED_COLOR);
        if (dyed == null) return;
        String hex = String.format(Locale.ROOT, "#%06X", dyed.rgb());
        Component plain = Component.translatable("item.dyed").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);
        Component advanced = Component.translatable("item.color", hex).withStyle(ChatFormatting.GRAY);
        int at = lines.indexOf(plain);
        if (at < 0) at = lines.indexOf(advanced);
        if (at < 0) return;
        Component swatch = Component.literal("■ ").withStyle(Style.EMPTY.withColor(dyed.rgb()));
        lines.set(at, Component.translatable("tooltip.nekoroqualityoflife.color", swatch.copy().append(hex)).withStyle(ChatFormatting.GRAY));
    }
}
