package dev.xardas.nekoroqol.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import dev.xardas.nekoroqol.Platform;

/** Client-side things that differ between NeoForge and Fabric. Only touched from client code. */
public interface ClientPlatform {
    ClientPlatform INSTANCE = Platform.load(ClientPlatform.class);

    void sendToServer(CustomPacketPayload payload);

    /** Whether the server has this mod too (features that need the server are hidden otherwise). */
    boolean serverHas(CustomPacketPayload.Type<?> type);

    /** The key or mouse button a key mapping is set to. */
    InputConstants.Key boundKey(KeyMapping key);

    /** NeoForge key modifiers (Ctrl+X vs X); always false on Fabric, which has no modifiers. */
    boolean modifierConflict(KeyMapping key, KeyMapping other);
}
