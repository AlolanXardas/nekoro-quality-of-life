package dev.xardas.nekoroqol.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.xardas.nekoroqol.QolConfig;

/**
 * Faster ladders. handleOnClimbable caps falling speed on ladders at 0.15; a player looking almost straight
 * down (and not sneaking) goes down at ladder_speed instead. Player movement is worked out by the player's
 * own game, so the client config decides.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Inject(method = "handleOnClimbable", at = @At("RETURN"), cancellable = true)
    private void nekoroQol$fasterLadders(Vec3 motion, CallbackInfoReturnable<Vec3> cir) {
        if (!((Object) this instanceof Player player) || !player.level().isClientSide()) return;
        QolConfig cfg = QolConfig.get();
        // only while looking down at least 60 degrees
        if (!cfg.fasterLadders || player.isShiftKeyDown() || player.getXRot() < 60F || player.getAbilities().flying) return;
        Vec3 result = cir.getReturnValue();
        if (result.y < 0) cir.setReturnValue(new Vec3(result.x, -cfg.ladderSpeed, result.z));
    }
}
