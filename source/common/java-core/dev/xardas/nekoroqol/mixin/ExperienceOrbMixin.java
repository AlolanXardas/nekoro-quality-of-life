package dev.xardas.nekoroqol.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.xardas.nekoroqol.Platform;
import dev.xardas.nekoroqol.QolConfig;

/**
 * Instant XP pickup. Vanilla takes one orb from a merged stack every 2 ticks (takeXpDelay); here the whole
 * stack is taken at once. Mending still repairs first, exactly as vanilla does for each orb.
 */
@Mixin(ExperienceOrb.class)
public abstract class ExperienceOrbMixin {
    @Shadow private int value;
    @Shadow private int count;

    @Shadow
    protected abstract int repairPlayerItems(ServerPlayer player, int amount);

    @Shadow private int age;

    /** Vanilla merges only orbs of the same value whose ids differ by a multiple of 40, so almost never. */
    @Inject(method = "canMerge(Lnet/minecraft/world/entity/ExperienceOrb;)Z", at = @At("HEAD"), cancellable = true)
    private void nekoroQol$canMerge(ExperienceOrb other, CallbackInfoReturnable<Boolean> cir) {
        if (QolConfig.get().mergeXpOrbs) cir.setReturnValue(other != (Object) this && !other.isRemoved());
    }

    /** Merging orbs of different values: one orb with the sum of all their experience. */
    @Inject(method = "merge", at = @At("HEAD"), cancellable = true)
    private void nekoroQol$merge(ExperienceOrb other, CallbackInfo ci) {
        if (!QolConfig.get().mergeXpOrbs) return;
        ExperienceOrbMixin o = (ExperienceOrbMixin) (Object) other;
        long total = (long) value * count + (long) o.value * o.count;
        value = (int) Math.min(Integer.MAX_VALUE, total);
        count = 1;
        age = Math.min(age, o.age);
        other.discard();
        ci.cancel();
    }

    @Inject(method = "playerTouch", at = @At("HEAD"), cancellable = true)
    private void nekoroQol$instantXp(Player player, CallbackInfo ci) {
        if (!QolConfig.get().instantXp || !(player instanceof ServerPlayer serverPlayer)) return;
        ExperienceOrb self = (ExperienceOrb) (Object) this;
        ci.cancel();
        if (!Platform.INSTANCE.allowXpPickup(player, self)) return;
        player.take(self, 1);
        int rest = 0;
        for (int i = 0; i < count; i++) rest += repairPlayerItems(serverPlayer, value);
        if (rest > 0) player.giveExperiencePoints(rest);
        self.discard();
    }
}
