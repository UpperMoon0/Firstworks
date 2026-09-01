package com.nstut.firstworks.content.workshop;
import com.mojang.serialization.MapCodec;
public final class PotteryWheelBlock extends WorkshopBlock {
    public static final MapCodec<PotteryWheelBlock> CODEC = simpleCodec(PotteryWheelBlock::new);
    public PotteryWheelBlock(Properties properties) { super(properties, WorkshopRecipe.POTTERY_WHEEL); }
    @Override protected MapCodec<? extends PotteryWheelBlock> codec() { return CODEC; }
}
