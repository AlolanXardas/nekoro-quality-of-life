package dev.xardas.nekoroqol.mixin.client;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import dev.xardas.nekoroqol.client.QolClient;

/** The mouse turns the player through Entity.turn; with the looking direction locked, the left/right part is dropped. */
@Mixin(Entity.class)
public abstract class EntityTurnMixin {
    @ModifyVariable(method = "turn", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private double nekoroQol$lockYaw(double yRot) {
        return QolClient.rotationLocked && (Object) this instanceof LocalPlayer ? 0 : yRot;
    }
}
