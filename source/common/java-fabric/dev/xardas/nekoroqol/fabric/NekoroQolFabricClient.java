package dev.xardas.nekoroqol.fabric;

import java.util.List;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;

import dev.xardas.nekoroqol.client.CleanTooltips;
import dev.xardas.nekoroqol.client.InventoryButtons;
import dev.xardas.nekoroqol.client.InventoryMouse;
import dev.xardas.nekoroqol.client.QolClient;

/** Fabric client events, connected to the shared client code. F3, the Key Binds screen and dragging come from mixins. */
public final class NekoroQolFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        QolClient.keyMappings().forEach(KeyBindingHelper::registerKeyBinding);
        ClientTickEvents.END_CLIENT_TICK.register(client -> QolClient.onTick());
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> QolClient.onLogin());
        ItemTooltipCallback.EVENT.register((stack, context, flags, lines) -> CleanTooltips.onTooltip(stack, lines, flags));

        ScreenEvents.BEFORE_INIT.register((client, screen, width, height) -> QolClient.onScreenInitPre(screen));
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            var widgets = Screens.getButtons(screen);
            QolClient.recipeBookButtons(screen, List.copyOf(widgets)).forEach(widgets::remove);
            widgets.addAll(InventoryButtons.create(screen));
            // per-screen events; Fabric starts them fresh every time a screen is set up
            ScreenMouseEvents.allowMouseScroll(screen).register((s, mouseX, mouseY, scrollX, scrollY) ->
                    !InventoryMouse.onScroll(s, mouseX, mouseY, scrollY));
            ScreenMouseEvents.allowMouseClick(screen).register((s, mouseX, mouseY, button) ->
                    !InventoryMouse.onPress(s, mouseX, mouseY, button));
            ScreenMouseEvents.afterMouseRelease(screen).register((s, mouseX, mouseY, button) -> InventoryMouse.onRelease());
        });
    }
}
