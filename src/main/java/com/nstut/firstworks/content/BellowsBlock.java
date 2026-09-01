package com.nstut.firstworks.content;

import com.nstut.firstworks.content.workshop.WorkshopBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class BellowsBlock extends Block {
    public BellowsBlock(Properties properties) { super(properties); }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        boolean found = false;
        if (!level.isClientSide) {
            for (Direction direction : Direction.values()) {
                if (level.getBlockEntity(pos.relative(direction)) instanceof WorkshopBlockEntity workshop && workshop.stoke(160)) {
                    found = true;
                }
            }
            if (found) level.playSound(null, pos, SoundEvents.WIND_CHARGE_BURST.value(), SoundSource.BLOCKS, .55F, .72F);
        }
        return found ? InteractionResult.sidedSuccess(level.isClientSide) : InteractionResult.PASS;
    }
}
