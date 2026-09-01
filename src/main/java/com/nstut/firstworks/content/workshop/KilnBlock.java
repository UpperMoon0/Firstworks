package com.nstut.firstworks.content.workshop;
import com.mojang.serialization.MapCodec;
public final class KilnBlock extends WorkshopBlock {
    public static final MapCodec<KilnBlock> CODEC = simpleCodec(KilnBlock::new);
    public KilnBlock(Properties properties) { super(properties, WorkshopRecipe.KILN); }
    @Override protected MapCodec<? extends KilnBlock> codec() { return CODEC; }
}
