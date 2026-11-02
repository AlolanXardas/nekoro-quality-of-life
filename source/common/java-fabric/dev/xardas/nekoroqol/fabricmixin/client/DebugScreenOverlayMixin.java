package dev.xardas.nekoroqol.fabricmixin.client;

import java.util.List;

import net.minecraft.client.gui.components.DebugScreenOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import dev.xardas.nekoroqol.client.DebugScreen;

/** The short F3 screen: changes both text columns right before they are drawn (NeoForge has an event for this). */
@Mixin(DebugScreenOverlay.class)
public abstract class DebugScreenOverlayMixin {
    private static final String RENDER_LINES =
            "Lnet/minecraft/client/gui/components/DebugScreenOverlay;renderLines(Lnet/minecraft/client/gui/GuiGraphics;Ljava/util/List;Z)V";

    @ModifyArg(method = "drawGameInformation", at = @At(value = "INVOKE", target = RENDER_LINES), index = 1)
    private List<String> nekoroQol$gameLines(List<String> lines) {
        DebugScreen.edit(lines, true);
        return lines;
    }

    @ModifyArg(method = "drawSystemInformation", at = @At(value = "INVOKE", target = RENDER_LINES), index = 1)
    private List<String> nekoroQol$systemLines(List<String> lines) {
        DebugScreen.edit(lines, false);
        return lines;
    }
}
