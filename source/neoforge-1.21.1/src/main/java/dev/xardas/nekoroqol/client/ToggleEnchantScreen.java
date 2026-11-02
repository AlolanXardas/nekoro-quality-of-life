package dev.xardas.nekoroqol.client;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.neoforge.network.PacketDistributor;

import dev.xardas.nekoroqol.EnchantToggle;
import dev.xardas.nekoroqol.ToggleEnchantPayload;

/** One button per enchantment on the item in the main hand: green = on, grey = off. Click to switch. */
public final class ToggleEnchantScreen extends Screen {
    private static final int BUTTON_WIDTH = 200;
    private static final int BUTTON_HEIGHT = 20;
    private static final int GAP = 4;

    record Entry(int level, boolean active) { }

    /** What the buttons were built from, to rebuild them when the server sends the changed item back. */
    private String shown = "";

    public ToggleEnchantScreen() {
        super(Component.translatable("screen.nekoro_qol.toggle_enchantments"));
    }

    /** Active and switched-off enchantments of the item, without curses (those cannot be switched). */
    static Map<Holder<Enchantment>, Entry> toggleable(ItemStack stack) {
        Map<Holder<Enchantment>, Entry> out = new LinkedHashMap<>();
        if (!EnchantToggle.canToggle(stack)) return out;
        ItemEnchantments active = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        ItemEnchantments disabled = stack.getOrDefault(EnchantToggle.DISABLED.get(), ItemEnchantments.EMPTY);
        for (var e : active.entrySet()) {
            if (!e.getKey().is(EnchantmentTags.CURSE)) out.put(e.getKey(), new Entry(e.getIntValue(), true));
        }
        for (var e : disabled.entrySet()) out.put(e.getKey(), new Entry(e.getIntValue(), false));
        return out;
    }

    private ItemStack held() {
        return minecraft != null && minecraft.player != null ? minecraft.player.getMainHandItem() : ItemStack.EMPTY;
    }

    @Override
    protected void init() {
        Map<Holder<Enchantment>, Entry> entries = toggleable(held());
        shown = entries.toString();
        int x = (width - BUTTON_WIDTH) / 2;
        int total = (entries.size() + 1) * (BUTTON_HEIGHT + GAP);
        int y = Math.max(30, (height - total) / 2);
        for (var e : entries.entrySet()) {
            Holder<Enchantment> holder = e.getKey();
            Entry entry = e.getValue();
            Component label = Enchantment.getFullname(holder, entry.level()).copy()
                    .withStyle(entry.active() ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY);
            addRenderableWidget(Button.builder(label, b -> holder.unwrapKey().ifPresent(key ->
                    PacketDistributor.sendToServer(new ToggleEnchantPayload(key.location()))))
                    .bounds(x, y, BUTTON_WIDTH, BUTTON_HEIGHT).build());
            y += BUTTON_HEIGHT + GAP;
        }
        addRenderableWidget(Button.builder(Component.translatable("screen.nekoro_qol.done"), b -> onClose())
                .bounds(x, y + GAP, BUTTON_WIDTH, BUTTON_HEIGHT).build());
    }

    @Override
    public void tick() {
        if (!toggleable(held()).toString().equals(shown)) rebuildWidgets();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 12, 0xFFFFFF);
        ItemStack stack = held();
        if (!stack.isEmpty()) graphics.drawCenteredString(font, stack.getHoverName(), width / 2, 22, 0xAAAAAA);
        if (toggleable(stack).isEmpty()) {
            graphics.drawCenteredString(font, Component.translatable("screen.nekoro_qol.no_enchantments"),
                    width / 2, height / 2 - 20, 0xAAAAAA);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
