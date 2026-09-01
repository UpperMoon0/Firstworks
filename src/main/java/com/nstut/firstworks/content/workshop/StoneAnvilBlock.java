package com.nstut.firstworks.content.workshop;
import com.mojang.serialization.MapCodec;
public final class StoneAnvilBlock extends WorkshopBlock {
    public static final MapCodec<StoneAnvilBlock> CODEC = simpleCodec(StoneAnvilBlock::new);
    public StoneAnvilBlock(Properties properties) { super(properties, WorkshopRecipe.STONE_ANVIL); }
    @Override protected MapCodec<? extends StoneAnvilBlock> codec() { return CODEC; }
}
