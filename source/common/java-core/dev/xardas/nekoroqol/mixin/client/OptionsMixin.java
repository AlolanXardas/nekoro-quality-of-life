package dev.xardas.nekoroqol.mixin.client;

import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.xardas.nekoroqol.QolConfig;
import dev.xardas.nekoroqol.client.Brightness;

/** Swaps in the Brightness option with the longer range before options.txt is read, so higher values load. */
@Mixin(Options.class)
public abstract class OptionsMixin {
    @Shadow @Final @Mutable
    private OptionInstance<Double> gamma;

    @Shadow
    public boolean skipMultiplayerWarning;

    /** Set after options.txt is read, so the warning stays off even if the file says otherwise. */
    @Inject(method = "<init>", at = @At("RETURN"))
    private void nekoroQol$skipMultiplayerWarning(CallbackInfo ci) {
        if (QolConfig.get().skipMultiplayerWarning) skipMultiplayerWarning = true;
    }

    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Options;load()V"))
    private void nekoroQol$longerBrightness(CallbackInfo ci) {
        if (QolConfig.get().brightnessMax > 100) gamma = Brightness.option();
    }
}
