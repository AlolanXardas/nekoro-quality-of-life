package dev.xardas.nekoroqol.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import dev.xardas.nekoroqol.DoubleDoors;
import dev.xardas.nekoroqol.EnchantToggle;
import dev.xardas.nekoroqol.NekoroQol;
import dev.xardas.nekoroqol.QolConfig;
import dev.xardas.nekoroqol.RecipeUnlock;
import dev.xardas.nekoroqol.ToggleEnchantPayload;
import dev.xardas.nekoroqol.inventory.HandRefill;
import dev.xardas.nekoroqol.inventory.InventoryPayload;

/** Fabric entry point: connects Fabric API events to the shared code. */
public final class NekoroQolFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        QolConfig.load();
        DataComponentType<ItemEnchantments> disabled = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
                ResourceLocation.fromNamespaceAndPath(NekoroQol.MOD_ID, EnchantToggle.COMPONENT_NAME), EnchantToggle.createComponent());
        EnchantToggle.DISABLED = () -> disabled;

        PayloadTypeRegistry.playC2S().register(ToggleEnchantPayload.TYPE, ToggleEnchantPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(InventoryPayload.TYPE, InventoryPayload.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ToggleEnchantPayload.TYPE,
                (payload, context) -> context.server().execute(() -> ToggleEnchantPayload.handle(payload, context.player())));
        ServerPlayNetworking.registerGlobalReceiver(InventoryPayload.TYPE,
                (payload, context) -> context.server().execute(() -> InventoryPayload.handle(payload, context.player())));

        ServerLifecycleEvents.SERVER_STARTING.register(server -> QolConfig.load());
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resources, success) -> QolConfig.load());
        ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.register((player, joined) -> RecipeUnlock.onDatapackSync(player));

        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            DoubleDoors.onUseBlock(player, level, hand, hit.getBlockPos());
            HandRefill.used(player, hand, player.getItemInHand(hand));
            return InteractionResult.PASS;
        });
        UseItemCallback.EVENT.register((player, level, hand) -> {
            HandRefill.used(player, hand, player.getItemInHand(hand));
            return InteractionResultHolder.pass(player.getItemInHand(hand));
        });
        // broken tools and eaten food come from FabricLivingEntityMixin
        ServerTickEvents.END_SERVER_TICK.register(server -> server.getPlayerList().getPlayers().forEach(HandRefill::refill));
    }
}
