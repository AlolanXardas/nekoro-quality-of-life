package dev.xardas.nekoroqol.mixin.client;

import com.mojang.serialization.Lifecycle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldOpenFlows;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.xardas.nekoroqol.QolConfig;

/** Skips the questions about experimental world settings when creating and opening a world. */
@Mixin(WorldOpenFlows.class)
public abstract class WorldOpenFlowsMixin {
    /** Creating a world with experimental settings: the world is made at once, as after "Proceed". */
    @Inject(method = "confirmWorldCreation", at = @At("HEAD"), cancellable = true)
    private static void nekoroQol$skipCreationWarning(Minecraft minecraft, CreateWorldScreen screen, Lifecycle lifecycle,
            Runnable createWorld, boolean skipWarnings, CallbackInfo ci) {
        if (skipWarnings || lifecycle != Lifecycle.experimental() || !QolConfig.get().skipExperimentalWorld) return;
        createWorld.run();
        ci.cancel();
    }

    /** Opening a world: the backup question is answered "no backup, go on", except for very old customized worlds. */
    @Inject(method = "askForBackup", at = @At("HEAD"), cancellable = true)
    private void nekoroQol$skipBackupQuestion(LevelStorageSource.LevelStorageAccess access, boolean oldCustomized,
            Runnable proceed, Runnable cancel, CallbackInfo ci) {
        if (oldCustomized || !QolConfig.get().skipExperimentalWorld) return;
        proceed.run();
        ci.cancel();
    }
}
