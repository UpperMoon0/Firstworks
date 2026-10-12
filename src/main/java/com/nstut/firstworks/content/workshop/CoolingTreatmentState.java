package com.nstut.firstworks.content.workshop;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.nstut.firstworks.registry.ModDataComponents;
import net.minecraft.world.item.ItemStack;

/** A selected ID (or empty selection) stays latched until completion or reheating, including across reloads. */
public record CoolingTreatmentState(String medium, String recipe, double completion, boolean finished) {
    public static final Codec<CoolingTreatmentState> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("medium").forGetter(CoolingTreatmentState::medium),
            Codec.STRING.fieldOf("recipe").forGetter(CoolingTreatmentState::recipe),
            Codec.doubleRange(20, 5000).fieldOf("completion").forGetter(CoolingTreatmentState::completion),
            Codec.BOOL.fieldOf("finished").forGetter(CoolingTreatmentState::finished)
    ).apply(i, CoolingTreatmentState::new));
    public static boolean blocksReheating(ItemStack stack) { return Boolean.TRUE.equals(stack.get(ModDataComponents.BLOCK_REHEATING.get())); }
}
