package com.nstut.firstworks.content.animal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;

import java.util.Optional;

public record AnimalMaterialProfile(
        EntityType<?> entity,
        Optional<DropRange> bones,
        Optional<DropRange> hide
) {
    public record DropRange(int min, int max, int lootingBonus) {
        public static final Codec<DropRange> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("min").forGetter(DropRange::min),
                Codec.INT.fieldOf("max").forGetter(DropRange::max),
                Codec.INT.optionalFieldOf("looting_bonus", 1).forGetter(DropRange::lootingBonus)
        ).apply(instance, DropRange::new));

        public int roll(RandomSource random, int lootingLevel) {
            int base = min + (max > min ? random.nextInt(max - min + 1) : 0);
            int bonus = lootingLevel > 0 && lootingBonus > 0 ? random.nextInt(lootingLevel * lootingBonus + 1) : 0;
            return base + bonus;
        }
    }

    public static final Codec<AnimalMaterialProfile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BuiltInRegistries.ENTITY_TYPE.byNameCodec().fieldOf("entity").forGetter(AnimalMaterialProfile::entity),
            DropRange.CODEC.optionalFieldOf("bones").forGetter(AnimalMaterialProfile::bones),
            DropRange.CODEC.optionalFieldOf("hide").forGetter(AnimalMaterialProfile::hide)
    ).apply(instance, AnimalMaterialProfile::new));
}
