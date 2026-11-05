package dev.xardas.nekoroqol.mixin.client;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;

/** Where an inventory screen's background starts (NeoForge has getters for it, Fabric does not). */
@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {
    @Accessor("leftPos")
    int nekoroQol$leftPos();

    @Accessor("topPos")
    int nekoroQol$topPos();

    /** A click as the screen itself handles it (the creative inventory passes it on to the player's inventory). */
    @Invoker("slotClicked")
    void nekoroQol$slotClicked(Slot slot, int slotId, int mouseButton, ClickType type);
}
