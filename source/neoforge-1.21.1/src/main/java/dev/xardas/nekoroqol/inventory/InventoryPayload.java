package dev.xardas.nekoroqol.inventory;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

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

    public static void handle(InventoryPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) InventoryActions.handle(player, payload);
        });
    }
}
