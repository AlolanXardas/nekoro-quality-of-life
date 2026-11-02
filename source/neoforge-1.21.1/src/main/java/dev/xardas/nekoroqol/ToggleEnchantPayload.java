package dev.xardas.nekoroqol;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client -> server: switch this enchantment on the item in the main hand. */
public record ToggleEnchantPayload(ResourceLocation enchantment) implements CustomPacketPayload {
    public static final Type<ToggleEnchantPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(NekoroQol.MOD_ID, "toggle_enchantment"));
    public static final StreamCodec<ByteBuf, ToggleEnchantPayload> STREAM_CODEC =
            ResourceLocation.STREAM_CODEC.map(ToggleEnchantPayload::new, ToggleEnchantPayload::enchantment);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handle(ToggleEnchantPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) EnchantToggle.toggle(player, payload.enchantment());
        });
    }
}
