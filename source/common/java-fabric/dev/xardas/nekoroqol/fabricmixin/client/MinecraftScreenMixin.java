package dev.xardas.nekoroqol.fabricmixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import dev.xardas.nekoroqol.client.ControlsScreen;

/** Opens the searchable Key Binds screen in place of the vanilla one (NeoForge has an event for this). */
@Mixin(Minecraft.class)
public abstract class MinecraftScreenMixin {
    @Shadow public Screen screen;

    @ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true)
    private Screen nekoroQol$replaceScreen(Screen next) {
        return ControlsScreen.replace(screen, next);
    }
}
