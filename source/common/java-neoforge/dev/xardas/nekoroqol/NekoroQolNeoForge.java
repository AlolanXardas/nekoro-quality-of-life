package dev.xardas.nekoroqol;

import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerDestroyItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.registries.DeferredRegister;

import dev.xardas.nekoroqol.inventory.HandRefill;
import dev.xardas.nekoroqol.inventory.InventoryPayload;
import dev.xardas.nekoroqol.neoforge.NeoForgeClient;

/** NeoForge entry point: connects NeoForge's events to the shared code. */
@Mod(NekoroQol.MOD_ID)
public final class NekoroQolNeoForge {
    private static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, EnchantToggle.COMPONENT_NAMESPACE);

    static {
        EnchantToggle.DISABLED = COMPONENTS.register(EnchantToggle.COMPONENT_NAME, EnchantToggle::createComponent);
    }

    public NekoroQolNeoForge(IEventBus modBus) {
        QolConfig.load();
        COMPONENTS.register(modBus);
        // optional: players with the mod can join servers without it (server features are then just missing)
        modBus.addListener((RegisterPayloadHandlersEvent event) -> event.registrar("1").optional()
                .playToServer(ToggleEnchantPayload.TYPE, ToggleEnchantPayload.STREAM_CODEC,
                        (ToggleEnchantPayload payload, IPayloadContext context) -> context.enqueueWork(() -> {
                            if (context.player() instanceof ServerPlayer player) ToggleEnchantPayload.handle(payload, player);
                        }))
                .playToServer(InventoryPayload.TYPE, InventoryPayload.STREAM_CODEC,
                        (InventoryPayload payload, IPayloadContext context) -> context.enqueueWork(() -> {
                            if (context.player() instanceof ServerPlayer player) InventoryPayload.handle(payload, player);
                        })));

        var bus = NeoForge.EVENT_BUS;
        bus.addListener((AddReloadListenerEvent event) -> QolConfig.load());
        bus.addListener((OnDatapackSyncEvent event) -> event.getRelevantPlayers().forEach(RecipeUnlock::onDatapackSync));
        bus.addListener((PlayerInteractEvent.RightClickBlock event) -> {
            if (event.getUseBlock() != TriState.FALSE) {
                DoubleDoors.onUseBlock(event.getEntity(), event.getLevel(), event.getHand(), event.getPos());
            }
            HandRefill.used(event.getEntity(), event.getHand(), event.getItemStack());
        });
        bus.addListener((PlayerInteractEvent.RightClickItem event) -> HandRefill.used(event.getEntity(), event.getHand(), event.getItemStack()));
        bus.addListener((PlayerDestroyItemEvent event) -> HandRefill.used(event.getEntity(), event.getHand(), event.getOriginal()));
        bus.addListener((LivingEntityUseItemEvent.Finish event) -> {
            if (event.getEntity() instanceof Player player) HandRefill.used(player, player.getUsedItemHand(), event.getItem());
        });
        bus.addListener((PlayerTickEvent.Post event) -> HandRefill.refill(event.getEntity()));

        if (FMLEnvironment.dist.isClient()) NeoForgeClient.init(modBus);
    }
}
