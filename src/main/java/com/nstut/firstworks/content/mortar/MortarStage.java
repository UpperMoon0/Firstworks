package com.nstut.firstworks.content.mortar;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record MortarStage(String action, int count, int duration) {
    public static final Codec<MortarStage> CODEC = RecordCodecBuilder.<MortarStage>create(instance -> instance.group(
            Codec.STRING.fieldOf("action").forGetter(MortarStage::action),
            Codec.intRange(0, 64).optionalFieldOf("count", 0).forGetter(MortarStage::count),
            Codec.intRange(0, 72000).optionalFieldOf("duration", 0).forGetter(MortarStage::duration)
    ).apply(instance, MortarStage::new)).validate(stage -> stage.valid()
            ? DataResult.success(stage) : DataResult.error(() -> "Use crush with count 1..64, or grind with duration 1..72000"));

    public boolean valid() {
        return action.equals("crush") && count > 0 && count <= 64 && duration == 0
                || action.equals("grind") && duration > 0 && duration <= 72000 && count == 0;
    }
    public int work() { return action.equals("crush") ? count : duration; }
}
