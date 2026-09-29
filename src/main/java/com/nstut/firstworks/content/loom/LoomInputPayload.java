package com.nstut.firstworks.content.loom;

import com.nstut.firstworks.Firstworks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Held samples request movement; the server validates aim, ownership and stroke phases. */
public record LoomInputPayload(BlockPos pos, boolean held, boolean assisted) implements CustomPacketPayload {
    public static final Type<LoomInputPayload> TYPE = new Type<>(Firstworks.id("loom_input"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LoomInputPayload> CODEC = StreamCodec.of(
            (buffer, value) -> { buffer.writeBlockPos(value.pos); buffer.writeBoolean(value.held); buffer.writeBoolean(value.assisted); },
            buffer -> new LoomInputPayload(buffer.readBlockPos(), buffer.readBoolean(), buffer.readBoolean()));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(TYPE, CODEC, (payload, context) -> {
            var player = context.player();
            if (!player.level().hasChunkAt(payload.pos) || !player.canInteractWithBlock(payload.pos, 0)) return;
            if (player.level().getBlockEntity(payload.pos) instanceof LoomBlockEntity loom) {
                if (payload.held) loom.guide(player, payload.assisted);
                else loom.release(player);
            }
        });
    }
}
