package com.nstut.firstworks.content.mortar;

import com.nstut.firstworks.Firstworks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Each held-input sample can earn at most one server tick; there is no autonomous work lease. */
public record MortarInputPayload(BlockPos pos, boolean held) implements CustomPacketPayload {
    public static final Type<MortarInputPayload> TYPE = new Type<>(Firstworks.id("mortar_input"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MortarInputPayload> CODEC = StreamCodec.of(
            (buffer, value) -> { buffer.writeBlockPos(value.pos); buffer.writeBoolean(value.held); },
            buffer -> new MortarInputPayload(buffer.readBlockPos(), buffer.readBoolean()));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(TYPE, CODEC, (payload, context) -> {
            var player = context.player();
            if (!player.level().hasChunkAt(payload.pos) || !player.canInteractWithBlock(payload.pos, 0)) return;
            if (player.level().getBlockEntity(payload.pos) instanceof MortarBlockEntity mortar) {
                if (payload.held) mortar.operate(player, "grind");
                else mortar.stopGrinding(player);
            }
        });
    }
}
