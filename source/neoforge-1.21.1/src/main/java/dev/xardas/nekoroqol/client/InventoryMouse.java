package dev.xardas.nekoroqol.client;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.lwjgl.glfw.GLFW;

import dev.xardas.nekoroqol.QolConfig;
import dev.xardas.nekoroqol.inventory.SlotGroups;

/**
 * Mouse shortcuts in inventories. Everything is done with normal clicks, the same ones a player could make
 * by hand, so it works on any server and in any screen built from normal slots.
 */
final class InventoryMouse {
    private static Slot lastDragged;

    private InventoryMouse() { }

    private static AbstractContainerScreen<?> screen(Screen screen) {
        if (!(screen instanceof AbstractContainerScreen<?> s) || screen instanceof CreativeModeInventoryScreen) return null;
        return Minecraft.getInstance().player == null || Minecraft.getInstance().gameMode == null ? null : s;
    }

    private static void click(AbstractContainerScreen<?> screen, Slot slot, int button, ClickType type) {
        Minecraft mc = Minecraft.getInstance();
        mc.gameMode.handleInventoryMouseClick(screen.getMenu().containerId, slot.index, button, type, mc.player);
    }

    private static Slot slotAt(AbstractContainerScreen<?> screen, double mouseX, double mouseY) {
        double x = mouseX - screen.getGuiLeft();
        double y = mouseY - screen.getGuiTop();
        for (Slot slot : screen.getMenu().slots) {
            if (slot.isActive() && x >= slot.x - 1 && x < slot.x + 17 && y >= slot.y - 1 && y < slot.y + 17) return slot;
        }
        return null;
    }

    private static boolean typing(Screen screen) {
        return screen.getFocused() instanceof EditBox box && box.isFocused();
    }

    static void onScroll(ScreenEvent.MouseScrolled.Pre event) {
        AbstractContainerScreen<?> screen = screen(event.getScreen());
        if (screen == null || !QolConfig.get().scrollMovesItems || event.getScrollDeltaY() == 0) return;
        AbstractContainerMenu menu = screen.getMenu();
        Slot hovered = slotAt(screen, event.getMouseX(), event.getMouseY());
        if (hovered == null || !hovered.hasItem() || !menu.getCarried().isEmpty() || SlotGroups.isResult(hovered)) return;
        ItemStack stack = hovered.getItem();
        List<Slot> others = otherSide(menu, hovered);
        if (event.getScrollDeltaY() < 0) {
            Slot target = target(others, stack);
            if (target == null || !hovered.mayPickup(Minecraft.getInstance().player)) return;
            moveOne(screen, hovered, target);
        } else {
            Slot source = null;
            for (Slot slot : others) {
                if (slot.hasItem() && ItemStack.isSameItemSameComponents(slot.getItem(), stack) && !SlotGroups.isResult(slot)) {
                    source = slot;
                    break;
                }
            }
            if (source == null || stack.getCount() >= hovered.getMaxStackSize(stack)) return;
            moveOne(screen, source, hovered);
        }
        event.setCanceled(true);
    }

    /** Pick up the stack, drop one with a right click, put the rest back. */
    private static void moveOne(AbstractContainerScreen<?> screen, Slot from, Slot to) {
        click(screen, from, 0, ClickType.PICKUP);
        click(screen, to, 1, ClickType.PICKUP);
        click(screen, from, 0, ClickType.PICKUP);
    }

    /**
     * Where items from this slot go: a container goes to the player and the player to the container. Without
     * a container (the player's own inventory) the hotbar and the rows above it swap roles.
     */
    private static List<Slot> otherSide(AbstractContainerMenu menu, Slot slot) {
        List<Slot> out = new ArrayList<>();
        if (!SlotGroups.isPlayer(slot)) {
            for (Slot s : menu.slots) if (SlotGroups.part(s) == SlotGroups.MAIN) out.add(s);
            for (Slot s : menu.slots) if (SlotGroups.part(s) == SlotGroups.HOTBAR) out.add(s);
            return out;
        }
        for (Slot s : menu.slots) {
            if (!SlotGroups.isPlayer(s) && !SlotGroups.isResult(s) && !(s.container instanceof CraftingContainer)) out.add(s);
        }
        if (!out.isEmpty()) return out;
        int want = SlotGroups.part(slot) == SlotGroups.HOTBAR ? SlotGroups.MAIN : SlotGroups.HOTBAR;
        for (Slot s : menu.slots) if (SlotGroups.part(s) == want) out.add(s);
        return out;
    }

    /** A stack of the same item with room first, then an empty slot that takes it. */
    private static Slot target(List<Slot> slots, ItemStack stack) {
        for (Slot slot : slots) {
            ItemStack there = slot.getItem();
            if (!there.isEmpty() && ItemStack.isSameItemSameComponents(there, stack)
                    && there.getCount() < slot.getMaxStackSize(there) && slot.mayPlace(stack)) return slot;
        }
        for (Slot slot : slots) if (!slot.hasItem() && slot.mayPlace(stack)) return slot;
        return null;
    }

    static void onPress(ScreenEvent.MouseButtonPressed.Pre event) {
        AbstractContainerScreen<?> screen = screen(event.getScreen());
        lastDragged = null;
        if (screen == null || event.getButton() != 0 || typing(screen)) return;
        AbstractContainerMenu menu = screen.getMenu();
        Slot slot = slotAt(screen, event.getMouseX(), event.getMouseY());
        lastDragged = slot;
        if (slot == null || !menu.getCarried().isEmpty() || SlotGroups.isResult(slot) || Screen.hasShiftDown()) return;
        QolConfig cfg = QolConfig.get();

        if (cfg.ctrlClickMovesSame && Screen.hasControlDown() && slot.hasItem()) {
            ItemStack kind = slot.getItem().copy();
            for (Slot s : SlotGroups.sameGroup(menu, slot)) {
                if (s.hasItem() && s.getItem().is(kind.getItem())) click(screen, s, 0, ClickType.QUICK_MOVE);
            }
            event.setCanceled(true);
        } else if (cfg.spaceClickMovesAll && spaceDown()) {
            for (Slot s : SlotGroups.sameGroup(menu, slot)) {
                if (s.hasItem()) click(screen, s, 0, ClickType.QUICK_MOVE);
            }
            event.setCanceled(true);
        }
    }

    private static boolean spaceDown() {
        return InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), GLFW.GLFW_KEY_SPACE);
    }

    /** Shift + left button held: every stack the mouse passes over is shift-clicked once. */
    static void onDrag(ScreenEvent.MouseDragged.Pre event) {
        AbstractContainerScreen<?> screen = screen(event.getScreen());
        if (screen == null || !QolConfig.get().shiftDragMoves || event.getMouseButton() != 0 || !Screen.hasShiftDown()) return;
        if (!screen.getMenu().getCarried().isEmpty()) return;
        Slot slot = slotAt(screen, event.getMouseX(), event.getMouseY());
        if (slot == null || slot == lastDragged) return;
        lastDragged = slot;
        if (slot.hasItem() && !SlotGroups.isResult(slot)) click(screen, slot, 0, ClickType.QUICK_MOVE);
    }

    static void onRelease(ScreenEvent.MouseButtonReleased.Pre event) {
        lastDragged = null;
    }
}
