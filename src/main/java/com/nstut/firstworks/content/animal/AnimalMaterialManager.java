package com.nstut.firstworks.content.animal;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import com.nstut.firstworks.Firstworks;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class AnimalMaterialManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().create();
    public static final AnimalMaterialManager INSTANCE = new AnimalMaterialManager();
    private static Map<EntityType<?>, AnimalMaterialProfile> PROFILES = Collections.emptyMap();

    public AnimalMaterialManager() {
        super(GSON, "firstworks/animal_materials");
    }

    @Override
    protected void apply(@NotNull Map<ResourceLocation, JsonElement> resources,
                         @NotNull ResourceManager resourceManager,
                         @NotNull ProfilerFiller profiler) {
        Map<EntityType<?>, AnimalMaterialProfile> map = new HashMap<>();
        resources.forEach((location, json) -> {
            AnimalMaterialProfile.CODEC.parse(JsonOps.INSTANCE, json)
                    .resultOrPartial(err -> Firstworks.LOGGER.error("Failed to parse animal material profile {}: {}", location, err))
                    .ifPresent(profile -> map.put(profile.entity(), profile));
        });
        PROFILES = Map.copyOf(map);
        Firstworks.LOGGER.info("Loaded {} animal material profiles", PROFILES.size());
    }

    public static Optional<AnimalMaterialProfile> getProfile(EntityType<?> type) {
        return Optional.ofNullable(PROFILES.get(type));
    }
}
