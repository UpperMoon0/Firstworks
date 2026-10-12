package com.nstut.firstworks.content.workshop;

import com.nstut.firstworks.FirstworksConfig;
import com.nstut.firstworks.registry.ModDataComponents;
import com.nstut.firstworks.registry.ModRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import java.util.*;

public final class CoolingSystem {
    @FunctionalInterface public interface AmbientProvider { double multiplier(Level level, BlockPos pos); }
    private static AmbientProvider ambient = (level,pos) -> Math.max(0.5,Math.min(2,1.5 - level.getBiome(pos).value().getBaseTemperature()));
    private static final Map<Level,Map<Long,Long>> EFFECTS = new WeakHashMap<>();
    public static void setAmbientProvider(AmbientProvider provider) { ambient = Objects.requireNonNull(provider); }
    public static double ambientMultiplier(Level level,BlockPos pos) {
        if (!FirstworksConfig.ENABLE_AMBIENT_COOLING.get()) return 1;
        double value = ambient.multiplier(level,pos);
        return Double.isFinite(value) ? Math.max(0.5,Math.min(2,value)) : 1;
    }
    public static ItemStack observe(ItemStack stack,Level level,BlockPos pos,CoolingProfiles.Selected medium) {
        if (level.isClientSide || stack.isEmpty() || !stack.has(ModDataComponents.HEAT.get())) return stack;
        final ItemStack input = stack;
        var old = stack.get(ModDataComponents.HEAT.get());
        double temperature = ItemHeat.celsius(stack,level);
        var state = stack.get(ModDataComponents.COOLING_TREATMENT.get());
        // Never select a different recipe for a latched treatment, including after its recipe disappears.
        if (temperature > 20 && (state == null || state.recipe().isEmpty() && !state.medium().equals(medium.id()))) {
            var selected = level.getRecipeManager().getAllRecipesFor(ModRecipes.COOLING_TYPE.get()).stream()
                    .filter(h -> h.value().medium().equals(medium.id()) && eligible(h.value(),input,temperature,medium.profile().endpoint()))
                    .sorted(Comparator.<net.minecraft.world.item.crafting.RecipeHolder<CoolingRecipe>>comparingInt(h -> h.value().priority()).reversed()
                            .thenComparing(h -> h.id().toString())).findFirst();
            state = selected.map(h -> new CoolingTreatmentState(medium.id(),h.id().toString(),h.value().completion(),false))
                    .orElseGet(() -> new CoolingTreatmentState(medium.id(),"",20,false));
            stack.set(ModDataComponents.COOLING_TREATMENT.get(),state);
        }
        boolean finished = state != null && !state.finished() && !state.recipe().isEmpty()
                && temperature <= state.completion();
        if (finished) {
            var latched = state;
            var recipe = level.getRecipeManager().getAllRecipesFor(ModRecipes.COOLING_TYPE.get()).stream()
                    .filter(h -> h.id().toString().equals(latched.recipe())).findFirst();
            if (recipe.isPresent()) stack = apply(recipe.get().value(),stack);
            stack.set(ModDataComponents.COOLING_TREATMENT.get(),new CoolingTreatmentState(state.medium(),state.recipe(),state.completion(),true));
        }
        if (temperature <= 20) {
            stack.remove(ModDataComponents.HEAT.get());
            stack.remove(ModDataComponents.COOLING_TREATMENT.get());
        } else {
            double multiplier = medium.profile().multiplier();
            if (medium.profile().medium().equals("air")) {
                if (level.getGameTime()%20==0 || !old.medium().equals(medium.id())
                        || !old.dimension().equals(level.dimension().location().toString())) multiplier *= ambientMultiplier(level,pos);
                else multiplier = old.multiplier();
            }
            ItemHeat.profile(stack,level,multiplier,medium.profile().endpoint(),medium.id());
        }
        if (temperature > 100 && !medium.profile().medium().equals("air")
                && old.updatedAt() < level.getGameTime() && old.multiplier() > 1 && old.temperatureCelsius() > temperature)
            effects(level,pos);
        return stack;
    }
    private static boolean eligible(CoolingRecipe r,ItemStack stack,double temperature,double endpoint) {
        if (!r.ingredient().test(stack) || stack.getCount() % r.count() != 0 || temperature < r.minimumEntry()
                || r.completion() < endpoint || r.excludedMarker().filter(m -> m.equals(stack.get(ModDataComponents.THERMAL_MARKER.get()))).isPresent()) return false;
        if (r.result().isPresent()) {
            if (stack.has(ModDataComponents.FORGE_PROGRESS.get()) && !r.preserveForgeProgress()) return false;
            var result = r.result().get();
            return (long)result.getCount() * (stack.getCount() / r.count()) <= result.getMaxStackSize();
        }
        return true;
    }
    private static ItemStack apply(CoolingRecipe recipe,ItemStack stack) {
        // Revalidate count and input identity after transfers/splits; a stale selection never consumes a partial batch.
        if (!recipe.ingredient().test(stack) || stack.getCount() % recipe.count() != 0
                || recipe.excludedMarker().isPresent() && recipe.excludedMarker().get().equals(stack.get(ModDataComponents.THERMAL_MARKER.get()))) return stack;
        if (recipe.result().isPresent()) {
            if (stack.has(ModDataComponents.FORGE_PROGRESS.get()) && !recipe.preserveForgeProgress()) return stack;
            var result = recipe.result().get();
            int count = result.getCount() * (stack.getCount()/recipe.count());
            if (count > result.getMaxStackSize()) return stack;
            // Preserve unrelated input components, then apply explicit output components.
            var replacement = stack.transmuteCopy(result.getItem(),count);
            var heat = stack.get(ModDataComponents.HEAT.get());
            var progress = stack.get(ModDataComponents.FORGE_PROGRESS.get());
            replacement.applyComponents(result.getComponentsPatch());
            replacement.set(ModDataComponents.HEAT.get(),heat);
            if (progress != null) replacement.set(ModDataComponents.FORGE_PROGRESS.get(),progress);
            stack = replacement;
        }
        if (recipe.marker().isPresent()) stack.set(ModDataComponents.THERMAL_MARKER.get(),recipe.marker().get());
        if (recipe.blockReheating()) stack.set(ModDataComponents.BLOCK_REHEATING.get(),true);
        return stack;
    }
    private static void effects(Level level,BlockPos pos) {
        if (!(level instanceof ServerLevel server)) return;
        var positions = EFFECTS.computeIfAbsent(level,l -> new HashMap<>());
        long now = level.getGameTime();
        if (now - positions.getOrDefault(pos.asLong(),now-20) < 20) return;
        if (positions.size() >= 4096) {
            positions.entrySet().removeIf(e -> now-e.getValue()>40);
            if (positions.size() >= 4096 && !positions.containsKey(pos.asLong())) return;
        }
        positions.put(pos.asLong(),now);
        server.sendParticles(ParticleTypes.CLOUD,pos.getX()+0.5,pos.getY()+0.7,pos.getZ()+0.5,3,0.15,0.15,0.15,0.01);
        server.sendParticles(ParticleTypes.BUBBLE_POP,pos.getX()+0.5,pos.getY()+0.6,pos.getZ()+0.5,2,0.1,0.1,0.1,0.01);
        level.playSound(null,pos,SoundEvents.FIRE_EXTINGUISH,SoundSource.BLOCKS,0.15f,1.6f);
    }
    private CoolingSystem() {}
}
