package dev.xardas.nekoroqol.mixin.client;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Where an inventory screen's background starts (NeoForge has getters for it, Fabric does not). */
@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {
    @Accessor("leftPos")
    int nekoroQol$leftPos();

    @Accessor("topPos")
    int nekoroQol$topPos();
}
