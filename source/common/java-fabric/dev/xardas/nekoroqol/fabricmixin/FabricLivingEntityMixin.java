package dev.xardas.nekoroqol.fabricmixin;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.xardas.nekoroqol.inventory.HandRefill;

/** Fabric has no events for a broken tool or finished eating, which the hand refill needs. */
@Mixin(LivingEntity.class)
public abstract class FabricLivingEntityMixin {
    @Shadow protected ItemStack useItem;

    @Shadow
    public abstract InteractionHand getUsedItemHand();

    @Inject(method = "onEquippedItemBroken", at = @At("HEAD"))
    private void nekoroQol$itemBroken(Item item, EquipmentSlot slot, CallbackInfo ci) {
        if (!((Object) this instanceof Player player)) return;
        if (slot == EquipmentSlot.MAINHAND) HandRefill.used(player, InteractionHand.MAIN_HAND, new ItemStack(item));
        else if (slot == EquipmentSlot.OFFHAND) HandRefill.used(player, InteractionHand.OFF_HAND, new ItemStack(item));
    }

    @Inject(method = "completeUsingItem", at = @At("HEAD"))
    private void nekoroQol$finishedUsing(CallbackInfo ci) {
        if ((Object) this instanceof Player player && !useItem.isEmpty()) HandRefill.used(player, getUsedItemHand(), useItem);
    }
}
