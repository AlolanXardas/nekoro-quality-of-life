package dev.xardas.nekoroqol.fabric;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import dev.xardas.nekoroqol.client.ClientPlatform;

public final class FabricClientPlatform implements ClientPlatform {
    @Override
    public void sendToServer(CustomPacketPayload payload) {
        ClientPlayNetworking.send(payload);
    }

    @Override
    public boolean serverHas(CustomPacketPayload.Type<?> type) {
        return ClientPlayNetworking.canSend(type);
    }

    @Override
    public InputConstants.Key boundKey(KeyMapping key) {
        return KeyBindingHelper.getBoundKeyOf(key);
    }

    @Override
    public boolean modifierConflict(KeyMapping key, KeyMapping other) {
        return false;
    }
}
