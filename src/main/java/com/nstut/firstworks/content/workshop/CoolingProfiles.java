package com.nstut.firstworks.content.workshop;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.nstut.firstworks.Firstworks;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.*;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.material.FluidState;
import java.util.*;

/** Reloadable media with stable IDs and deterministic priority/ID selection. */
public final class CoolingProfiles extends SimpleJsonResourceReloadListener {
    public record Profile(String medium, double multiplier, double endpoint, int priority) {
        public static final Codec<Profile> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("medium").forGetter(Profile::medium),
                Codec.doubleRange(0.01,100).fieldOf("multiplier").forGetter(Profile::multiplier),
                Codec.doubleRange(20,5000).optionalFieldOf("endpoint",20.0).forGetter(Profile::endpoint),
                Codec.INT.optionalFieldOf("priority",0).forGetter(Profile::priority)
        ).apply(i,Profile::new));
        public Profile {
            if (!Double.isFinite(multiplier) || !Double.isFinite(endpoint) || multiplier < 0.01 || multiplier > 100 || endpoint < 20 || endpoint > 5000)
                throw new IllegalArgumentException("Invalid cooling profile");
            if (!medium.equals("air") && ResourceLocation.tryParse(medium) == null) throw new IllegalArgumentException("Invalid medium fluid tag");
        }
    }
    public record Selected(String id, Profile profile) {}
    public static final CoolingProfiles INSTANCE = new CoolingProfiles();
    private static List<Selected> profiles = List.of();
    private static final Selected AIR = new Selected("firstworks:air",new Profile("air",1,20,0));
    private static final Selected WATER = new Selected("firstworks:water",new Profile("minecraft:water",10,20,0));
    private CoolingProfiles() { super(new Gson(),"firstworks/cooling_profiles"); }
    @Override protected void apply(Map<ResourceLocation,JsonElement> resources, ResourceManager manager, ProfilerFiller profiler) {
        List<Selected> loaded = new ArrayList<>();
        resources.forEach((id,json) -> Profile.CODEC.parse(JsonOps.INSTANCE,json)
                .resultOrPartial(err -> Firstworks.LOGGER.error("Invalid cooling profile {}: {}",id,err))
                .ifPresent(p -> loaded.add(new Selected(id.toString(),p))));
        loaded.sort(Comparator.<Selected>comparingInt(s -> s.profile.priority).reversed().thenComparing(Selected::id));
        profiles = List.copyOf(loaded);
    }
    public static Selected select(FluidState fluid) {
        // Lava is never a coolant even if a datapack puts it in another fluid tag.
        if (fluid.is(net.minecraft.tags.FluidTags.LAVA)) return air();
        return profiles.stream().filter(s -> !s.profile.medium.equals("air")
                && fluid.is(TagKey.create(Registries.FLUID,ResourceLocation.parse(s.profile.medium))))
                .findFirst().orElse(fluid.is(net.minecraft.tags.FluidTags.WATER) ? WATER : air());
    }
    public static Selected air() { return profiles.stream().filter(s -> s.profile.medium.equals("air")).findFirst().orElse(AIR); }
}
