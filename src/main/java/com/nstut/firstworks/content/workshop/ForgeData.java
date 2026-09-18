package com.nstut.firstworks.content.workshop;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import java.util.List;
import java.util.Optional;

/** Recipe-owned operations; item models never determine the interaction geometry. */
public record ForgeData(List<String> actions, int heatTicks, Optional<Visual> visual) {
    public static final Codec<String> ACTION = Codec.STRING.validate(value ->
            List.of("flatten", "draw", "bend").contains(value) ? DataResult.success(value)
                    : DataResult.error(() -> "Forge action must be flatten, draw, or bend"));
    public static final Codec<ForgeData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ACTION.listOf().validate(list -> !list.isEmpty() && list.size() <= 64
                    ? DataResult.success(list) : DataResult.error(() -> "Forge needs 1 to 64 actions"))
                    .fieldOf("actions").forGetter(ForgeData::actions),
            Codec.intRange(0, 72000).optionalFieldOf("heat_ticks", 1200).forGetter(ForgeData::heatTicks),
            Visual.CODEC.optionalFieldOf("visual").forGetter(ForgeData::visual)
    ).apply(instance, ForgeData::new));

    public ForgeData {
        actions = List.copyOf(actions);
        if (actions.isEmpty() || actions.size() > 64 || actions.stream().anyMatch(a -> !List.of("flatten", "draw", "bend").contains(a)))
            throw new IllegalArgumentException("Forge needs 1 to 64 valid actions");
        if (heatTicks < 0 || heatTicks > 72000) throw new IllegalArgumentException("Invalid forge heat window");
    }

    public record Visual(String type, String initialProfile, float length, float width, float height,
                         List<ResourceLocation> models) {
        public static final Codec<Visual> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.validate(s -> s.equals("deformable") || s.equals("stages")
                        ? DataResult.success(s) : DataResult.error(() -> "Unknown forge visual type"))
                        .optionalFieldOf("type", "deformable").forGetter(Visual::type),
                Codec.STRING.validate(s -> s.equals("billet") || s.equals("plate")
                        ? DataResult.success(s) : DataResult.error(() -> "Unknown forge profile"))
                        .optionalFieldOf("initial_profile", "billet").forGetter(Visual::initialProfile),
                Codec.floatRange(0.05F, 0.6F).optionalFieldOf("length", 0.32F).forGetter(Visual::length),
                Codec.floatRange(0.05F, 0.4F).optionalFieldOf("width", 0.16F).forGetter(Visual::width),
                Codec.floatRange(0.02F, 0.3F).optionalFieldOf("height", 0.12F).forGetter(Visual::height),
                ResourceLocation.CODEC.listOf().validate(list -> list.size() <= 65 && list.stream()
                        .allMatch(id -> id.getPath().startsWith("forge_workpieces/"))
                        ? DataResult.success(list) : DataResult.error(() -> "Use up to 65 forge_workpieces/ models"))
                        .optionalFieldOf("models", List.of()).forGetter(Visual::models)
        ).apply(instance, Visual::new));

        public Visual { models = List.copyOf(models); }
    }
}
