package dev.xardas.nekoroqol;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.slf4j.Logger;

import dev.xardas.nekoroqol.client.QolClient;
import dev.xardas.nekoroqol.inventory.HandRefill;
import dev.xardas.nekoroqol.inventory.InventoryPayload;

@Mod(NekoroQol.MOD_ID)
public final class NekoroQol {
    public static final String MOD_ID = "nekoro_qol";
    public static final Logger LOGGER = LogUtils.getLogger();

    public NekoroQol(IEventBus modBus) {
        QolConfig.load();
        EnchantToggle.COMPONENTS.register(modBus);
        // optional: players with the mod can join servers without it (server features are then just missing)
        modBus.addListener((RegisterPayloadHandlersEvent event) -> event.registrar("1").optional()
                .playToServer(ToggleEnchantPayload.TYPE, ToggleEnchantPayload.STREAM_CODEC, ToggleEnchantPayload::handle)
                .playToServer(InventoryPayload.TYPE, InventoryPayload.STREAM_CODEC, InventoryPayload::handle));

        NeoForge.EVENT_BUS.addListener((AddReloadListenerEvent event) -> QolConfig.load());
        NeoForge.EVENT_BUS.addListener((OnDatapackSyncEvent event) -> RecipeUnlock.onDatapackSync(event));
        NeoForge.EVENT_BUS.addListener((PlayerInteractEvent.RightClickBlock event) -> DoubleDoors.onRightClick(event));
        HandRefill.register(NeoForge.EVENT_BUS);

        if (FMLEnvironment.dist.isClient()) QolClient.init(modBus);
    }
}
