package dev.xardas.nekoroqol.mixin.client;

import net.minecraft.client.gui.screens.options.controls.KeyBindsList;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Lets the filtered Key Binds screen use its own list where vanilla keeps its list. */
@Mixin(KeyBindsScreen.class)
public interface KeyBindsScreenAccessor {
    @Accessor("keyBindsList")
    void nekoroQol$setKeyBindsList(KeyBindsList list);
}
