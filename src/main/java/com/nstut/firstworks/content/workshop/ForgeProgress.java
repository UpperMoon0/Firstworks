package com.nstut.firstworks.content.workshop;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Progress applies to one complete recipe batch and its exact action sequence. */
public record ForgeProgress(String recipe, String sequence, int completed, int batchSize) {
    public static final Codec<ForgeProgress> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("recipe").forGetter(ForgeProgress::recipe),
            Codec.STRING.fieldOf("sequence").forGetter(ForgeProgress::sequence),
            Codec.intRange(0, 64).fieldOf("completed").forGetter(ForgeProgress::completed),
            Codec.intRange(1, 64).fieldOf("batch_size").forGetter(ForgeProgress::batchSize)
    ).apply(i, ForgeProgress::new));
}
