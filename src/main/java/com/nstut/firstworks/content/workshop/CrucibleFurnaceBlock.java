package com.nstut.firstworks.content.workshop;
import com.mojang.serialization.MapCodec;
public final class CrucibleFurnaceBlock extends WorkshopBlock {
    public static final MapCodec<CrucibleFurnaceBlock> CODEC = simpleCodec(CrucibleFurnaceBlock::new);
    public CrucibleFurnaceBlock(Properties properties) { super(properties, WorkshopRecipe.CRUCIBLE_FURNACE); }
    @Override protected MapCodec<? extends CrucibleFurnaceBlock> codec() { return CODEC; }
}
