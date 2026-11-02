package dev.xardas.nekoroqol.client;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.RecipeBookType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.Vec3;

import dev.xardas.nekoroqol.EnchantToggle;
import dev.xardas.nekoroqol.QolConfig;
import dev.xardas.nekoroqol.ToggleEnchantPayload;
import dev.xardas.nekoroqol.mixin.client.ImageButtonAccessor;

/** Everything that only exists in the player's game: keys, the enchantment menu, tooltips, movement, screens. */
public final class QolClient {
    private static final String CATEGORY = "key.categories.nekoro_qol";
    // no default keys, so nothing happens by accident; players pick a key in Controls when they want a feature
    static final KeyMapping TOGGLE_MENU = new KeyMapping("key.nekoro_qol.toggle_enchantments",
            InputConstants.UNKNOWN.getValue(), CATEGORY);
    /** One key per enchantment that is worth switching quickly; the item is found in the hands or the armor. */
    static final Map<KeyMapping, ResourceKey<Enchantment>> QUICK_TOGGLES = new LinkedHashMap<>();

    static {
        for (ResourceKey<Enchantment> key : List.of(Enchantments.SILK_TOUCH, Enchantments.FROST_WALKER,
                Enchantments.KNOCKBACK, Enchantments.PUNCH, Enchantments.FIRE_ASPECT, Enchantments.FLAME,
                Enchantments.SWEEPING_EDGE, Enchantments.RIPTIDE, Enchantments.SOUL_SPEED, Enchantments.THORNS)) {
            QUICK_TOGGLES.put(new KeyMapping("key.nekoro_qol.toggle_" + key.location().getPath(),
                    InputConstants.UNKNOWN.getValue(), CATEGORY), key);
        }
    }
    static final KeyMapping AUTO_WALK = new KeyMapping("key.nekoro_qol.auto_walk", InputConstants.UNKNOWN.getValue(), CATEGORY);
    static final KeyMapping LOCK_ROTATION = new KeyMapping("key.nekoro_qol.lock_rotation", InputConstants.UNKNOWN.getValue(), CATEGORY);
    static final KeyMapping SWAP_HOTBAR = new KeyMapping("key.nekoro_qol.swap_hotbar", InputConstants.UNKNOWN.getValue(), CATEGORY);
    static final KeyMapping FULL_DEBUG = new KeyMapping("key.nekoro_qol.full_debug", InputConstants.UNKNOWN.getValue(), CATEGORY);

    /** While true, the mouse no longer turns the player left or right (looking up and down still works). */
    public static boolean rotationLocked;

    private static boolean autoWalking;
    private static boolean forwardWasDown;

    private QolClient() { }

    /** Every key of the mod, for the loader to register. None has a default key. */
    public static List<KeyMapping> keyMappings() {
        List<KeyMapping> keys = new ArrayList<>();
        keys.add(TOGGLE_MENU);
        keys.addAll(QUICK_TOGGLES.keySet());
        keys.add(AUTO_WALK);
        keys.add(LOCK_ROTATION);
        keys.add(SWAP_HOTBAR);
        keys.add(FULL_DEBUG);
        return keys;
    }

    /** Joining a world or server. */
    public static void onLogin() {
        if (QolConfig.get().hideTutorials) Minecraft.getInstance().getTutorial().setStep(TutorialSteps.NONE);
    }

    /** End of every client tick. */
    public static void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;
        QolConfig cfg = QolConfig.get();

        while (TOGGLE_MENU.consumeClick()) {
            if (cfg.toggleEnchantments && mc.screen == null && serverHas(ToggleEnchantPayload.TYPE)) mc.setScreen(new ToggleEnchantScreen());
        }
        QUICK_TOGGLES.forEach((key, enchantment) -> {
            while (key.consumeClick()) {
                if (cfg.toggleEnchantments && serverHas(ToggleEnchantPayload.TYPE)) quickToggle(player, enchantment);
            }
        });
        while (SWAP_HOTBAR.consumeClick()) {
            if (mc.screen == null && mc.gameMode != null) swapHotbar(mc, player);
        }
        while (FULL_DEBUG.consumeClick()) {
            if (cfg.shortDebugScreen) DebugScreen.toggleFull(mc);
        }
        DebugScreen.onTick(mc);
        while (AUTO_WALK.consumeClick()) {
            autoWalking = !autoWalking;
            forwardWasDown = physicallyDown(mc.options.keyUp);
            if (!autoWalking) mc.options.keyUp.setDown(forwardWasDown);
        }
        while (LOCK_ROTATION.consumeClick()) {
            rotationLocked = !rotationLocked;
            // snap to the nearest of the 8 directions, so walking forward follows an axis or an exact diagonal
            if (rotationLocked) player.setYRot(Math.round(player.getYRot() / 45F) * 45F);
            player.displayClientMessage(Component.translatable(rotationLocked ? "message.nekoro_qol.rotation_locked"
                    : "message.nekoro_qol.rotation_unlocked"), true);
        }
        if (autoWalking) autoWalk(mc);

        if (cfg.noCreativeDrift && player.getAbilities().flying && !player.isFallFlying()) stopDrift(mc, player);
    }

    /** Walks forward until the key is pressed again, or the player presses forward or back himself. */
    private static void autoWalk(Minecraft mc) {
        boolean forwardDown = physicallyDown(mc.options.keyUp);
        boolean pressedForward = forwardDown && !forwardWasDown;
        forwardWasDown = forwardDown;
        if (pressedForward || physicallyDown(mc.options.keyDown)) {
            autoWalking = false;
            mc.options.keyUp.setDown(forwardDown);
            return;
        }
        if (mc.screen == null) mc.options.keyUp.setDown(true);
    }

    private static void stopDrift(Minecraft mc, LocalPlayer player) {
        var o = mc.options;
        boolean horizontal = o.keyUp.isDown() || o.keyDown.isDown() || o.keyLeft.isDown() || o.keyRight.isDown();
        boolean vertical = o.keyJump.isDown() || o.keyShift.isDown();
        if (horizontal && vertical) return;
        Vec3 motion = player.getDeltaMovement();
        player.setDeltaMovement(horizontal ? motion.x : 0, vertical ? motion.y : 0, horizontal ? motion.z : 0);
    }

    /** Features that need the mod on the server too. */
    static boolean serverHas(CustomPacketPayload.Type<?> type) {
        return ClientPlatform.INSTANCE.serverHas(type);
    }

    /** The hotbar swaps places with the row right above it, with normal swap clicks. Pressing again swaps back. */
    private static void swapHotbar(Minecraft mc, LocalPlayer player) {
        int menu = player.inventoryMenu.containerId;
        for (int i = 0; i < 9; i++) mc.gameMode.handleInventoryMouseClick(menu, 27 + i, i, ClickType.SWAP, player);
    }

    private static boolean physicallyDown(KeyMapping key) {
        InputConstants.Key bound = ClientPlatform.INSTANCE.boundKey(key);
        if (bound.getType() != InputConstants.Type.KEYSYM || bound.getValue() == InputConstants.UNKNOWN.getValue()) return key.isDown();
        return InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), bound.getValue());
    }

    /** The server switches it on the first item that has it: main hand, offhand, boots, leggings, chestplate, helmet. */
    private static void quickToggle(LocalPlayer player, ResourceKey<Enchantment> enchantment) {
        for (ItemStack stack : EnchantToggle.searchOrder(player)) {
            for (Holder<Enchantment> holder : ToggleEnchantScreen.toggleable(stack).keySet()) {
                if (holder.is(enchantment)) {
                    ClientPlatform.INSTANCE.sendToServer(new ToggleEnchantPayload(enchantment.location()));
                    return;
                }
            }
        }
        player.displayClientMessage(Component.translatable("message.nekoro_qol.no_enchantment",
                Component.translatable("enchantment." + enchantment.location().getNamespace() + "." + enchantment.location().getPath())), true);
    }

    /** With the recipe book hidden, screens always open with it closed, so their layout stays centered. */
    public static void onScreenInitPre(Screen screen) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (!QolConfig.get().hideRecipeBook || player == null || !(screen instanceof RecipeUpdateListener)) return;
        for (RecipeBookType type : RecipeBookType.values()) player.getRecipeBook().setOpen(type, false);
    }

    /** With the recipe book hidden: its green button in a screen that was just set up, for the loader to remove. */
    public static List<GuiEventListener> recipeBookButtons(Screen screen, List<? extends GuiEventListener> listeners) {
        List<GuiEventListener> out = new ArrayList<>();
        if (!QolConfig.get().hideRecipeBook || !(screen instanceof RecipeUpdateListener)) return out;
        for (GuiEventListener listener : listeners) {
            if (listener instanceof ImageButton button
                    && ((ImageButtonAccessor) button).nekoroQol$getSprites() == RecipeBookComponent.RECIPE_BUTTON_SPRITES) {
                out.add(button);
            }
        }
        return out;
    }
}
