package dev.xardas.nekoroqol.neoforge;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import dev.xardas.nekoroqol.client.CleanTooltips;
import dev.xardas.nekoroqol.client.ControlsScreen;
import dev.xardas.nekoroqol.client.DebugScreen;
import dev.xardas.nekoroqol.client.InventoryButtons;
import dev.xardas.nekoroqol.client.InventoryMouse;
import dev.xardas.nekoroqol.client.QolClient;

/** NeoForge client events, connected to the shared client code. */
public final class NeoForgeClient {
    private NeoForgeClient() { }

    public static void init(IEventBus modBus) {
        modBus.addListener((RegisterKeyMappingsEvent event) -> QolClient.keyMappings().forEach(event::register));
        IEventBus bus = NeoForge.EVENT_BUS;
        bus.addListener((ClientTickEvent.Post event) -> QolClient.onTick());
        bus.addListener((ClientPlayerNetworkEvent.LoggingIn event) -> QolClient.onLogin());
        bus.addListener((ItemTooltipEvent event) -> CleanTooltips.onTooltip(event.getItemStack(), event.getToolTip(), event.getFlags()));
        // last, so lines other mods add are already there
        bus.addListener(EventPriority.LOWEST, (CustomizeGuiOverlayEvent.DebugText event) -> {
            DebugScreen.edit(event.getLeft(), true);
            DebugScreen.edit(event.getRight(), false);
        });
        bus.addListener((ScreenEvent.Init.Pre event) -> QolClient.onScreenInitPre(event.getScreen()));
        bus.addListener((ScreenEvent.Init.Post event) -> {
            QolClient.recipeBookButtons(event.getScreen(), event.getListenersList()).forEach(event::removeListener);
            InventoryButtons.create(event.getScreen()).forEach(event::addListener);
        });
        bus.addListener((ScreenEvent.Opening event) -> event.setNewScreen(ControlsScreen.replace(event.getCurrentScreen(), event.getNewScreen())));
        bus.addListener((ScreenEvent.MouseScrolled.Pre event) -> {
            if (InventoryMouse.onScroll(event.getScreen(), event.getMouseX(), event.getMouseY(), event.getScrollDeltaY())) event.setCanceled(true);
        });
        bus.addListener((ScreenEvent.MouseButtonPressed.Pre event) -> {
            if (InventoryMouse.onPress(event.getScreen(), event.getMouseX(), event.getMouseY(), event.getButton())) event.setCanceled(true);
        });
        bus.addListener((ScreenEvent.MouseDragged.Pre event) ->
                InventoryMouse.onDrag(event.getScreen(), event.getMouseX(), event.getMouseY(), event.getMouseButton()));
        bus.addListener((ScreenEvent.MouseButtonReleased.Pre event) -> InventoryMouse.onRelease());
    }
}
