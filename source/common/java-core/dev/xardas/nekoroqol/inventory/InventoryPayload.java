package dev.xardas.nekoroqol.inventory;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import dev.xardas.nekoroqol.NekoroQol;

/** Client -> server: a sort or transfer button was pressed for the group of slots that holds {@code slot}. */
public record InventoryPayload(int containerId, int slot, int action, int mode) implements CustomPacketPayload {
    public static final Type<InventoryPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(NekoroQol.MOD_ID, "inventory_action"));
    public static final StreamCodec<ByteBuf, InventoryPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, InventoryPayload::containerId,
            ByteBufCodecs.VAR_INT, InventoryPayload::slot,
            ByteBufCodecs.VAR_INT, InventoryPayload::action,
            ByteBufCodecs.VAR_INT, InventoryPayload::mode,
            InventoryPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** On the server thread. */
    public static void handle(InventoryPayload payload, ServerPlayer player) {
        InventoryActions.handle(player, payload);
    }
}
