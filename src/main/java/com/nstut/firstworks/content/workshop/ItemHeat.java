package com.nstut.firstworks.content.workshop;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.nstut.firstworks.registry.ModDataComponents;
import com.nstut.firstworks.registry.ModRecipes;
import com.nstut.firstworks.registry.ModTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Heat expires against world time, including in inventories and unloaded containers. */
public record ItemHeat(int ticks, int capacity, long updatedAt, double temperatureCelsius, double maximumCelsius) {
    public ItemHeat(int ticks, int capacity, long updatedAt) { this(ticks, capacity, updatedAt, -1, -1); }
    public static final Codec<ItemHeat> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.intRange(0, 72000).fieldOf("ticks").forGetter(ItemHeat::ticks),
            Codec.intRange(1, 72000).fieldOf("capacity").forGetter(ItemHeat::capacity),
            Codec.LONG.fieldOf("updated_at").forGetter(ItemHeat::updatedAt),
            Codec.doubleRange(-1, 5000).optionalFieldOf("temperature_celsius", -1.0).forGetter(ItemHeat::temperatureCelsius),
            Codec.doubleRange(-1, 5000).optionalFieldOf("maximum_celsius", -1.0).forGetter(ItemHeat::maximumCelsius)
    ).apply(i, ItemHeat::new));

    public int remaining(long now) {
        return (int) Math.max(0, Math.min(ticks, capacity) - Math.max(0, now - updatedAt));
    }
    public static int remaining(ItemStack stack, Level level) {
        ItemHeat heat = stack.get(ModDataComponents.HEAT.get());
        return heat == null || level == null ? 0 : heat.remaining(level.getGameTime());
    }
    public static float fraction(ItemStack stack, Level level) {
        ItemHeat heat = stack.get(ModDataComponents.HEAT.get());
        if (heat == null) return 0;
        double maximum = heat.maximumCelsius > ThermalModel.AMBIENT ? heat.maximumCelsius : HeatTemperature.maximum(stack);
        return (float) Math.max(0, Math.min(1, (celsius(stack, level) - ThermalModel.AMBIENT) / (maximum - ThermalModel.AMBIENT)));
    }
    public static boolean workable(ItemStack stack, Level level) { return celsius(stack, level) >= minimumWorkable(stack, level); }
    public static double celsius(ItemStack stack, Level level) {
        ItemHeat heat = stack.get(ModDataComponents.HEAT.get());
        if (heat == null || level == null) return ThermalModel.AMBIENT;
        double maximum = heat.maximumCelsius > ThermalModel.AMBIENT ? heat.maximumCelsius : HeatTemperature.maximum(stack);
        double initial = heat.temperatureCelsius >= ThermalModel.AMBIENT ? heat.temperatureCelsius
                : HeatTemperature.celsius((float) heat.ticks / heat.capacity, (int) maximum);
        return ThermalModel.cool(initial, level.getGameTime() - heat.updatedAt, (maximum - ThermalModel.AMBIENT) / heat.capacity);
    }
    public static double coolingRate(ItemStack stack) {
        ItemHeat heat = stack.get(ModDataComponents.HEAT.get());
        return heat == null ? 0 : ((heat.maximumCelsius > ThermalModel.AMBIENT ? heat.maximumCelsius : HeatTemperature.maximum(stack))
                - ThermalModel.AMBIENT) / heat.capacity;
    }
    public static double minimumWorkable(ItemStack stack, Level level) {
        if (level == null) return 500;
        ForgeProgress progress = stack.get(ModDataComponents.FORGE_PROGRESS.get());
        return level.getRecipeManager().getAllRecipesFor(ModRecipes.WORKSHOP_PROCESSING_TYPE.get()).stream()
                .filter(h -> WorkshopRecipe.STONE_ANVIL.equals(h.value().station()) && h.value().ingredient().test(stack)
                        && (progress == null || h.id().toString().equals(progress.recipe())))
                .flatMap(h -> h.value().forge().stream()).filter(f -> f.heatTicks() > 0)
                .mapToInt(ForgeData::minimumTemperature).min().orElse(500);
    }
    public static String state(ItemStack stack, Level level) {
        return workable(stack, level) ? "workable" : celsius(stack, level) > ThermalModel.AMBIENT ? "warm" : "cold";
    }
    public static int light(ItemStack stack, Level level) { return Math.round(12 * fraction(stack, level)); }
    public static String state(float fraction) {
        return fraction >= 0.25F ? "workable" : fraction > 0 ? "warm" : "cold";
    }
    public static int capacity(ItemStack stack, Level level) {
        if (stack.isEmpty() || level == null) return 0;
        int recipeHeat = level.getRecipeManager().getAllRecipesFor(ModRecipes.WORKSHOP_PROCESSING_TYPE.get()).stream()
                .map(h -> h.value()).filter(r -> WorkshopRecipe.STONE_ANVIL.equals(r.station()) && r.ingredient().test(stack))
                .flatMap(r -> r.forge().stream()).mapToInt(ForgeData::heatTicks).max().orElse(0);
        return recipeHeat > 0 ? recipeHeat : stack.is(ModTags.HEATABLE_ITEMS) ? 3600 : 0;
    }
    public static void set(ItemStack stack, Level level, int ticks, int capacity) {
        setTemperature(stack, level, HeatTemperature.celsius((float) Math.min(ticks, capacity) / capacity,
                HeatTemperature.maximum(stack)), capacity);
    }
    public static void setTemperature(ItemStack stack, Level level, double temperature, int capacity) {
        if (stack.isEmpty() || level == null || capacity <= 0) return;
        int maximum = HeatTemperature.maximum(stack);
        double actual = Math.max(ThermalModel.AMBIENT, Math.min(maximum, temperature));
        int remaining = (int) Math.ceil((actual - ThermalModel.AMBIENT) / (maximum - ThermalModel.AMBIENT) * capacity);
        stack.set(ModDataComponents.HEAT.get(), new ItemHeat(remaining, capacity, level.getGameTime(), actual, maximum));
    }
    private static java.util.function.Supplier<Level> clientLevel = () -> null;

    /** Client setup supplies its thread-safe level lookup without loading client classes on servers. */
    public static void setClientLevelSupplier(java.util.function.Supplier<Level> supplier) { clientLevel = supplier; }

    public static void clearExpired(ItemStack stack, Level level) {
        if (level != null && !stack.isEmpty() && stack.has(ModDataComponents.HEAT.get())
                && remaining(stack, level) == 0) stack.remove(ModDataComponents.HEAT.get());
    }

    /** Normalize only when comparing stacks, including dormant storage and third-party transfers.
     * Hot stacks and all other components (especially forging progress) remain distinct.
     */
    public static void normalizeForComparison(ItemStack first, ItemStack second) {
        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        Level level = server != null && server.isSameThread() ? server.overworld() : clientLevel.get();
        if (level == null || !ModDataComponents.HEAT.isBound()) return;
        clearExpired(first, level);
        clearExpired(second, level);
    }

    public static void copy(ItemStack source, ItemStack target, Level level) {
        ItemHeat heat = source.get(ModDataComponents.HEAT.get());
        if (heat != null && celsius(source, level) > ThermalModel.AMBIENT)
            target.set(ModDataComponents.HEAT.get(), new ItemHeat(remaining(source, level), heat.capacity(), level.getGameTime(),
                    celsius(source, level), heat.maximumCelsius > ThermalModel.AMBIENT ? heat.maximumCelsius : HeatTemperature.maximum(source)));
    }
}
