package dev.xardas.nekoroqol.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.minecraft.client.gui.screens.Screen;

import dev.xardas.nekoroqol.QolConfig;
import dev.xardas.nekoroqol.mixin.client.AbstractContainerScreenAccessor;
import dev.xardas.nekoroqol.inventory.InventoryActions;
import dev.xardas.nekoroqol.inventory.InventoryPayload;
import dev.xardas.nekoroqol.inventory.SlotGroups;

/**
 * Small buttons above the top right corner of each storage area: sort, and with a container open also
 * "move matching items here" and "move everything here". The hotbar is never touched. They work on the
 * server, so they only show when the server has the mod.
 */
public final class InventoryButtons {
    private static final ResourceLocation BUTTON = ResourceLocation.withDefaultNamespace("widget/button");
    private static final ResourceLocation BUTTON_HOVER = ResourceLocation.withDefaultNamespace("widget/button_highlighted");
    private static final int SIZE = 10;

    private InventoryButtons() { }

    /** The buttons for a screen that was just set up (empty for other screens). */
    public static List<AbstractWidget> create(Screen opened) {
        List<AbstractWidget> out = new ArrayList<>();
        if (!(opened instanceof AbstractContainerScreen<?> screen) || screen instanceof CreativeModeInventoryScreen) return out;
        QolConfig cfg = QolConfig.get();
        if (!cfg.sortButton && !cfg.transferButtons) return out;
        if (cfg.noButtonsIn.contains(screen.getClass().getName())) return out;
        if (!ClientPlatform.INSTANCE.serverHas(InventoryPayload.TYPE)) return out;

        // each area gets: sort, then "matching items here" and "everything here" (items move into that area)
        List<List<Slot>> storages = SlotGroups.storages(screen.getMenu());
        for (List<Slot> storage : storages) {
            int x = right(storage);
            if (cfg.sortButton) out.add(new Button(screen, storage.get(0), Kind.SORT, x -= SIZE, top(storage)));
            if (cfg.transferButtons) {
                out.add(new Button(screen, storage.get(0), Kind.MATCHING_TO_STORAGE, x -= SIZE + 1, top(storage)));
                out.add(new Button(screen, storage.get(0), Kind.ALL_TO_STORAGE, x - SIZE - 1, top(storage)));
            }
        }
        List<Slot> main = SlotGroups.playerMain(screen.getMenu());
        if (main.size() != 27) return out;
        int x = right(main);
        if (cfg.sortButton) out.add(new Button(screen, main.get(0), Kind.SORT, x -= SIZE, top(main)));
        if (cfg.transferButtons && !storages.isEmpty()) {
            // the transfer works with the first container of the screen (almost every screen has only one)
            Slot storage = storages.get(0).get(0);
            out.add(new Button(screen, storage, Kind.MATCHING_TO_PLAYER, x -= SIZE + 1, top(main)));
            out.add(new Button(screen, storage, Kind.ALL_TO_PLAYER, x - SIZE - 1, top(main)));
        }
        return out;
    }

    static int left(AbstractContainerScreen<?> screen) {
        return ((AbstractContainerScreenAccessor) screen).nekoroQol$leftPos();
    }

    static int top(AbstractContainerScreen<?> screen) {
        return ((AbstractContainerScreenAccessor) screen).nekoroQol$topPos();
    }

    /** Right edge of the slots, relative to the screen's left side. */
    private static int right(List<Slot> slots) {
        int x = 0;
        for (Slot slot : slots) x = Math.max(x, slot.x + 17);
        return x;
    }

    /** Just above the first row. */
    private static int top(List<Slot> slots) {
        int y = Integer.MAX_VALUE;
        for (Slot slot : slots) y = Math.min(y, slot.y);
        return y - SIZE - 2;
    }

    enum Kind {
        SORT(InventoryActions.SORT, false, false),
        MATCHING_TO_STORAGE(InventoryActions.MATCHING_TO_STORAGE, true, false),
        ALL_TO_STORAGE(InventoryActions.ALL_TO_STORAGE, true, true),
        MATCHING_TO_PLAYER(InventoryActions.MATCHING_TO_PLAYER, false, false),
        ALL_TO_PLAYER(InventoryActions.ALL_TO_PLAYER, false, true);

        final int action;
        /** Containers sit above the player inventory, so items going into them move up. */
        final boolean up;
        final boolean all;

        Kind(int action, boolean up, boolean all) {
            this.action = action;
            this.up = up;
            this.all = all;
        }
    }

    private static final class Button extends AbstractWidget {
        private final AbstractContainerScreen<?> screen;
        private final Slot anchor;
        private final Kind kind;
        private final int offsetX;
        private final int offsetY;

        Button(AbstractContainerScreen<?> screen, Slot anchor, Kind kind, int offsetX, int offsetY) {
            super(left(screen) + offsetX, top(screen) + offsetY, SIZE, SIZE, Component.empty());
            this.screen = screen;
            this.anchor = anchor;
            this.kind = kind;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            updateTooltip();
        }

        private void updateTooltip() {
            Component text = switch (kind) {
                case SORT -> Component.translatable("button.nekoro_qol.sort",
                        Component.translatable("button.nekoro_qol.sort." + ClientState.sortMode().id()));
                case MATCHING_TO_STORAGE, MATCHING_TO_PLAYER -> Component.translatable("button.nekoro_qol.matching");
                case ALL_TO_STORAGE, ALL_TO_PLAYER -> Component.translatable("button.nekoro_qol.all");
            };
            setTooltip(Tooltip.create(text));
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            // the screen can move (recipe book opened), so follow it
            setPosition(left(screen) + offsetX, top(screen) + offsetY);
            g.blitSprite(isHoveredOrFocused() ? BUTTON_HOVER : BUTTON, getX(), getY(), width, height);
            int x = getX() + 2;
            int y = getY() + 2;
            int c = 0xFFE0E0E0;
            if (kind == Kind.SORT) {
                // three lines getting shorter
                g.fill(x, y, x + 6, y + 1, c);
                g.fill(x, y + 2, x + 4, y + 3, c);
                g.fill(x, y + 4, x + 2, y + 5, c);
            } else if (kind.all) {
                chevron(g, x, y, kind.up, c);
                chevron(g, x, y + 3, kind.up, c);
            } else {
                arrow(g, x, y, kind.up, c);
            }
        }

        /** A 6x6 arrow pointing up or down: matching items. */
        private static void arrow(GuiGraphics g, int x, int y, boolean up, int c) {
            chevron(g, x, up ? y : y + 3, up, c);
            g.fill(x + 2, up ? y + 3 : y, x + 4, up ? y + 6 : y + 3, c);
        }

        /** A 6x3 arrow head; two of them stacked mean everything. */
        private static void chevron(GuiGraphics g, int x, int y, boolean up, int c) {
            for (int row = 0; row < 3; row++) {
                int yy = up ? y + row : y + 2 - row;
                g.fill(x + 2 - row, yy, x + 4 + row, yy + 1, c);
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (!active || !visible || !isMouseOver(mouseX, mouseY) || (button != 0 && button != 1)) return false;
            playDownSound(Minecraft.getInstance().getSoundManager());
            // sort: every click sorts in the next order (right click: the previous one), so one click shows the result
            if (kind == Kind.SORT) {
                ClientState.setSortMode(button == 1 ? ClientState.sortMode().previous() : ClientState.sortMode().next());
                updateTooltip();
            }
            ClientPlatform.INSTANCE.sendToServer(new InventoryPayload(screen.getMenu().containerId, anchor.index,
                    kind.action, ClientState.sortMode().ordinal()));
            return true;
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }
}
