package com.nstut.firstworks.content;

import com.nstut.firstworks.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.util.Mth;

/**
 * Keeps the Bellows press animation synchronized without turning the interaction into a GUI.
 * The actual furnace stoke remains server-authoritative in {@link BellowsBlock}; this entity only
 * records the last successful press so clients can render a smooth compression/rebound stroke.
 */
public final class BellowsBlockEntity extends BlockEntity {
    private long lastPressTick = Long.MIN_VALUE;

    public BellowsBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BELLOWS.get(), pos, state);
    }

    public void press() {
        if (level == null) return;
        lastPressTick = level.getGameTime();
        sync();
    }

    public float getCompression(float partialTick) {
        if (level == null || lastPressTick == Long.MIN_VALUE) return 0.0F;
        float elapsed = (float) (level.getGameTime() + partialTick - lastPressTick);
        if (elapsed < 0.0F || elapsed >= 10.0F) return 0.0F;
        float t = Mth.clamp(elapsed / 10.0F, 0.0F, 1.0F);
        return Mth.sin((float) Math.PI * t);
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("LastPressTick", lastPressTick);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        lastPressTick = tag.getLong("LastPressTick");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this, BlockEntity::getUpdateTag);
    }
}
