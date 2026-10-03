package com.nstut.firstworks.content.workshop;

import net.minecraft.core.particles.ParticleTypes;
import com.nstut.firstworks.registry.ModDataComponents;
import com.nstut.firstworks.FirstworksConfig;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.network.chat.Component;
import com.nstut.firstworks.compat.OptionalIntegrations;
import com.nstut.firstworks.registry.ModBlockEntities;
import com.nstut.firstworks.registry.ModRecipes;
import com.nstut.firstworks.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public final class WorkshopBlockEntity extends BlockEntity {
    private static final int INPUT_SLOT = 0;
    private static final int CATALYST_SLOT = 1;
    private static final int FUEL_SLOT = 2;
    private static final int OUTPUT_SLOT = 3;

    private ItemStack input = ItemStack.EMPTY;
    private ItemStack catalyst = ItemStack.EMPTY;
    private ItemStack fuel = ItemStack.EMPTY;
    private ItemStack output = ItemStack.EMPTY;
    private int progress;
    private int stokeTicks;
    private int forgeHeat; // Legacy block-owned heat, imported once into the workpiece.
    private int burnTicks;
    private boolean ignited;
    private double temperatureCelsius = ThermalModel.AMBIENT;
    private long lastBellowsTick = Long.MIN_VALUE;
    private double heatRemainder;
    private boolean importItemState;
    private long lastForgeTick = Long.MIN_VALUE;
    private String lastForgeAction = "";
    private boolean legacyForgeProgressPending;
    private boolean running;
    private boolean processCancelled;
    private long actionSteps;

    private double clientPrevRotation;
    private double clientRotation;
    private double rotationTarget;
    private boolean clientRotationInitialized;
    private long clientObservedActionSteps = Long.MIN_VALUE;
    private long clientActionTick = Long.MIN_VALUE;

    private final IItemHandler handler = new WorkshopItemHandler();

    public WorkshopBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WORKSHOP.get(), pos, state);
    }

    public String station() {
        return getBlockState().getBlock() instanceof WorkshopBlock workshop ? workshop.station() : "";
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, WorkshopBlockEntity workshop) {
        workshop.clientPrevRotation = workshop.clientRotation;
        if (workshop.clientRotation < workshop.rotationTarget) {
            double diff = workshop.rotationTarget - workshop.clientRotation;
            workshop.clientRotation += Math.min(diff, 18.0D);
        } else if (workshop.clientRotation > workshop.rotationTarget) {
            workshop.clientRotation = workshop.rotationTarget;
            workshop.clientPrevRotation = workshop.rotationTarget;
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, WorkshopBlockEntity workshop) {
        // Return surplus molds from older saves instead of deleting paid items.
        if (!level.isClientSide && workshop.station().equals(WorkshopRecipe.CRUCIBLE_FURNACE)
                && workshop.catalyst.getCount() > 1) {
            ItemStack surplus = workshop.catalyst.split(workshop.catalyst.getCount() - 1);
            net.minecraft.world.Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, surplus);
            workshop.sync();
        }
        workshop.migrateLegacyForgeProgress();
        workshop.importLegacyItemState();
        if (workshop.station().equals(WorkshopRecipe.KILN)) {
            workshop.tickKiln();
            return;
        }
        workshop.updateHeatLight();
        boolean burning = workshop.heated() && workshop.tickFire();
        boolean stokeExpired = false;
        if (workshop.stokeTicks > 0) {
            workshop.stokeTicks--;
            stokeExpired = workshop.stokeTicks == 0;
        }

        if (workshop.station().equals(WorkshopRecipe.CRUCIBLE_FURNACE)) {
            double ceiling = workshop.getMaxTemperature();
            workshop.temperatureCelsius = burning
                    ? ThermalModel.approach(workshop.temperatureCelsius, ceiling, FirstworksConfig.CRUCIBLE_HEATING_RATE.get())
                    : ThermalModel.cool(Math.min(workshop.temperatureCelsius, ceiling), 1, FirstworksConfig.STATION_COOLING_RATE.get());
            workshop.setChanged();
            if (level.getGameTime() % 5 == 0) workshop.sync();
        }
        String station = workshop.station();
        if (!station.equals(WorkshopRecipe.CRUCIBLE_FURNACE)) {
            if (stokeExpired) {
                workshop.sync();
            }
            return;
        }

        Optional<RecipeHolder<WorkshopRecipe>> holder = workshop.activeRecipe();
        if (holder.isEmpty() || !workshop.output.isEmpty()) {
            if (workshop.progress != 0 || workshop.running || workshop.processCancelled || stokeExpired) {
                workshop.progress = 0;
                workshop.running = false;
                workshop.processCancelled = false;
                workshop.sync();
            }
            return;
        }
        if (!burning || workshop.temperatureCelsius < holder.get().value().requiredTemperature()) {
            if (stokeExpired) {
                workshop.sync();
            }
            return;
        }

        boolean started = false;
        if (!workshop.running) {
            if (!workshop.tryBegin(holder.get())) {
                return;
            }
            workshop.running = true;
            started = true;
            level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.55F, 0.85F);
        }

        workshop.progress++;
        if (workshop.progress >= holder.get().value().work()) {
            workshop.complete(holder.get());
        } else if (started || stokeExpired || workshop.progress % 20 == 0) {
            workshop.sync();
        }
    }

    public boolean needsIgnition() {
        return heated() && burnTicks == 0 && fuel.getBurnTime(RecipeType.SMELTING) > 0;
    }

    /** Ignite fuel explicitly; bellows and inventory insertion never start a fire. */
    public boolean ignite() {
        if (level == null || level.isClientSide || !needsIgnition()) return false;
        if (!consumeFuel()) return false;
        level.playSound(null, worldPosition, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
        sync();
        return true;
    }

    public boolean stoke(int ticks) {
        if (!station().equals(WorkshopRecipe.CRUCIBLE_FURNACE)) {
            return false;
        }
        processCancelled = false;
        stokeTicks = Math.min(FirstworksConfig.BELLOWS_HOLD_TICKS.get() + FirstworksConfig.BELLOWS_DECAY_TICKS.get(), Math.max(1, ticks));
        sync();
        return true;
    }

    public boolean blowBellows(Player player) {
        if (level == null || level.isClientSide || !station().equals(WorkshopRecipe.CRUCIBLE_FURNACE) || !isHot()) return false;
        long now = level.getGameTime();
        if (lastBellowsTick != Long.MIN_VALUE && now - lastBellowsTick < FirstworksConfig.BELLOWS_COOLDOWN_TICKS.get()) return false;
        int cost = FirstworksConfig.BELLOWS_FOOD_COST.get();
        if (!player.hasInfiniteMaterials() && player.getFoodData().getFoodLevel() < cost) return false;
        int duration = FirstworksConfig.BELLOWS_HOLD_TICKS.get() + FirstworksConfig.BELLOWS_DECAY_TICKS.get();
        if (stokeTicks >= duration) return false;
        stoke(duration);
        lastBellowsTick = now;
        if (!player.hasInfiniteMaterials()) {
            var food = player.getFoodData();
            food.setFoodLevel(food.getFoodLevel() - cost);
            food.setSaturation(Math.min(food.getSaturationLevel(), food.getFoodLevel()));
        }
        sync();
        return true;
    }

    public double getTemperature() {
        return station().equals(WorkshopRecipe.KILN) || station().equals(WorkshopRecipe.STONE_ANVIL)
                ? ItemHeat.celsius(input.isEmpty() ? output : input, level) : temperatureCelsius;
    }
    public double getMaxTemperature() {
        if (station().equals(WorkshopRecipe.KILN) || station().equals(WorkshopRecipe.STONE_ANVIL))
            return HeatTemperature.maximum(input.isEmpty() ? output : input);
        double base = FirstworksConfig.CRUCIBLE_BASE_TEMPERATURE.get();
        return ThermalModel.boostedCeiling(base, Math.max(base, FirstworksConfig.CRUCIBLE_BOOST_TEMPERATURE.get()),
                stokeTicks, FirstworksConfig.BELLOWS_DECAY_TICKS.get());
    }

    private void migrateLegacyForgeProgress() {
        if (!legacyForgeProgressPending || level == null || level.isClientSide) return;
        legacyForgeProgressPending = false;
        if (!station().equals(WorkshopRecipe.STONE_ANVIL) || progress <= 0) return;
        var active = activeRecipe();
        if (active.isEmpty() || active.get().value().forge().isEmpty()) return;
        WorkshopRecipe recipe = active.get().value();
        int legacyWork = Math.max(1, recipe.work());
        int forgeWork = Math.max(1, recipe.requiredWork());
        progress = Mth.clamp((int) ((long) progress * forgeWork / legacyWork), 0, forgeWork - 1);
        forgeHeat = 0;
        lastForgeTick = Long.MIN_VALUE;
        lastForgeAction = "";
        setChanged();
    }

    public boolean isHot() {
        return heated() && burnTicks > 0;
    }

    public boolean hasHotCrucibleContents() {
        return station().equals(WorkshopRecipe.CRUCIBLE_FURNACE) && isHot()
                && running && !input.isEmpty() && output.isEmpty()
                && activeRecipe().map(h -> temperatureCelsius >= h.value().requiredTemperature()).orElse(false);
    }

    public int getForgeHeat() { return ItemHeat.remaining(input, level); }
    public String getLastForgeAction() { return lastForgeAction; }

    public Component anvilHint(String action, boolean hammer) {
        if (!output.isEmpty()) return Component.translatable("hint.firstworks.collect");
        if (input.isEmpty()) return Component.translatable("jade.firstworks.workshop.empty");
        var active = activeRecipe();
        if (active.isEmpty()) {
            var candidate = stationRecipes().filter(h -> h.value().ingredient().test(input))
                    .sorted(Comparator.comparingInt(h -> h.value().inputCount())).findFirst();
            if (candidate.isEmpty()) return Component.translatable("hint.firstworks.unsupported");
            if (input.getCount() < candidate.get().value().inputCount())
                return Component.translatable("hint.firstworks.input_count", input.getCount(), candidate.get().value().inputCount());
            return Component.translatable("hint.firstworks.catalyst", candidate.get().value().catalystCount());
        }
        if (!hammer) return Component.translatable("hint.firstworks.anvil.hammer");
        if (processCancelled) return Component.translatable("hint.firstworks.cancelled");
        var forge = active.get().value().forge();
        if (forge.isEmpty()) return Component.translatable("hint.firstworks.anvil.smash");
        if (forge.get().heatTicks() > 0 && ItemHeat.celsius(input, level) < forge.get().minimumTemperature())
            return Component.translatable("hint.firstworks.anvil.reheat");
        String next = forge.get().actions().get(Math.min(progress, forge.get().actions().size() - 1));
        if (forge.get().heatTicks() == 0) return Component.translatable("hint.firstworks.anvil.action_cold",
                Component.translatable("action.firstworks." + action), Component.translatable("action.firstworks." + next));
        return Component.translatable("hint.firstworks.anvil.action",
                Component.translatable("action.firstworks." + action),
                Component.translatable("action.firstworks." + next), (getForgeHeat() + 19) / 20);
    }

    private void tickKiln() {
        int capacity = ItemHeat.capacity(input, level);
        int heat = ItemHeat.remaining(input, level);
        boolean burning = tickFire();
        if (burning) {
            if (capacity > 0) {
                double maximum = HeatTemperature.maximum(input);
                double rate = (maximum - ThermalModel.AMBIENT) / (FirstworksConfig.KILN_HEATING_TICKS.get() * (double) input.getCount());
                ItemHeat.setTemperature(input, level, ThermalModel.approach(ItemHeat.celsius(input, level), maximum,
                        rate + ItemHeat.coolingRate(input)), capacity);
            }
            setChanged();
        }
        updateHeatLight();
        if (level.getGameTime() % (burning && heat < capacity ? 5 : 20) == 0) sync();
    }

    /** Fire lifetime is independent of recipes, workpieces, output collection, and bellows air. */
    private boolean tickFire() {
        if (burnTicks <= 0) return false;
        burnTicks--;
        if (burnTicks == 0) {
            consumeFuel();
            sync();
        } else if (level.getGameTime() % 20 == 0) sync();
        setChanged();
        return true;
    }

    private boolean consumeFuel() {
        int duration = fuel.getBurnTime(RecipeType.SMELTING);
        if (duration <= 0) return false;
        ItemStack remainder = fuel.getCraftingRemainingItem();
        fuel.shrink(1);
        if (fuel.isEmpty()) fuel = remainder;
        else if (!remainder.isEmpty()) net.minecraft.world.Containers.dropItemStack(level,
                worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, remainder);
        burnTicks = duration;
        ignited = true;
        return true;
    }

    private void updateHeatLight() {
        if (level == null || level.isClientSide) return;
        if (!station().equals(WorkshopRecipe.KILN) && !station().equals(WorkshopRecipe.STONE_ANVIL)) return;
        int light = Math.max(ItemHeat.light(input, level), ItemHeat.light(output, level));
        if (station().equals(WorkshopRecipe.KILN) && burnTicks > 0) light = Math.max(light, 8);
        if (getBlockState().getValue(WorkshopBlock.HEAT_LIGHT) != light)
            level.setBlock(worldPosition, getBlockState().setValue(WorkshopBlock.HEAT_LIGHT, light), Block.UPDATE_CLIENTS);
    }

    private void importLegacyItemState() {
        if (!importItemState || level == null || level.isClientSide) return;
        importItemState = false;
        if (!station().equals(WorkshopRecipe.STONE_ANVIL) || input.isEmpty()) return;
        if (forgeHeat > 0) {
            int capacity = Math.max(forgeHeat, ItemHeat.capacity(input, level));
            ItemHeat.set(input, level, forgeHeat, capacity);
        }
        forgeHeat = 0;
        storeForgeProgress();
        sync();
    }

    private void storeForgeProgress() {
        if (!station().equals(WorkshopRecipe.STONE_ANVIL) || input.isEmpty() || progress <= 0) return;
        activeRecipe().filter(h -> h.value().forge().isPresent()).ifPresent(h ->
                input.set(ModDataComponents.FORGE_PROGRESS.get(), new ForgeProgress(h.id().toString(),
                        String.join(",", h.value().forge().get().actions()), progress, h.value().inputCount())));
    }

    private void restoreForgeProgress() {
        progress = 0;
        ForgeProgress saved = input.get(ModDataComponents.FORGE_PROGRESS.get());
        if (saved == null || !station().equals(WorkshopRecipe.STONE_ANVIL)) return;
        activeRecipe().filter(h -> h.id().toString().equals(saved.recipe())
                && h.value().forge().isPresent() && h.value().inputCount() == saved.batchSize()
                && input.getCount() == saved.batchSize()
                && String.join(",", h.value().forge().get().actions()).equals(saved.sequence()))
                .ifPresent(h -> progress = Math.min(saved.completed(), h.value().requiredWork() - 1));
    }

    public int getBurnTicks() { return burnTicks; }

    private static boolean hasHammer(Player player) {
        return player.getMainHandItem().is(ModTags.HAMMERS) || player.getOffhandItem().is(ModTags.HAMMERS);
    }

    public boolean forge(Player player, String action) {
        if (!(level instanceof ServerLevel server) || !station().equals(WorkshopRecipe.STONE_ANVIL)
                || !hasHammer(player) || lastForgeTick == level.getGameTime()) return false;
        var active = activeRecipe();
        if (active.isEmpty() || !output.isEmpty()) return false;
        var recipe = active.get().value();
        if (recipe.forge().isEmpty()) return work(player);
        var data = recipe.forge().get();
        if (progress >= data.actions().size() || !data.actions().get(progress).equals(action)
                || input.getCount() != recipe.inputCount()
                || data.heatTicks() > 0 && ItemHeat.celsius(input, level) < data.minimumTemperature()) return false;
        processCancelled = false;
        if (!tryBegin(active.get())) return false;
        lastForgeTick = level.getGameTime();
        lastForgeAction = action;
        progress++;
        storeForgeProgress();
        actionSteps++;
        server.sendParticles(ParticleTypes.CRIT,
                worldPosition.getX() + 0.5, worldPosition.getY() + 0.72, worldPosition.getZ() + 0.5,
                3, 0.12, 0.02, 0.12, 0.02);
        level.playSound(null, worldPosition, SoundEvents.ANVIL_HIT, SoundSource.BLOCKS, 0.45F,
                action.equals("draw") ? 1.5F : action.equals("bend") ? 0.85F : 1.15F);
        if (progress >= recipe.requiredWork()) complete(active.get());
        else sync();
        return true;
    }

    public boolean work(Player player) {
        if (!(level instanceof ServerLevel)) return false;
        String station = station();
        if (!station.equals(WorkshopRecipe.POTTERY_WHEEL) && !station.equals(WorkshopRecipe.STONE_ANVIL)) {
            return false;
        }
        Optional<RecipeHolder<WorkshopRecipe>> holder = activeRecipe();
        if (holder.isEmpty() || !output.isEmpty()) {
            return false;
        }

        if (station.equals(WorkshopRecipe.STONE_ANVIL)
                && (!hasHammer(player) || holder.get().value().forge().isPresent())) return false;

        // Manual work is an explicit retry boundary; unlike the ticking furnace it cannot spam a veto handler.
        processCancelled = false;
        if (!tryBegin(holder.get())) {
            return false;
        }

        progress++;
        actionSteps++;
        if (level != null) {
            level.playSound(null, worldPosition,
                    station.equals(WorkshopRecipe.POTTERY_WHEEL) ? SoundEvents.BRUSH_GENERIC : SoundEvents.ANVIL_HIT,
                    SoundSource.BLOCKS, 0.45F, station.equals(WorkshopRecipe.POTTERY_WHEEL) ? 1.15F : 1.35F);
        }
        if (progress >= holder.get().value().work()) {
            complete(holder.get());
        } else {
            sync();
        }
        return true;
    }

    private boolean tryBegin(RecipeHolder<WorkshopRecipe> holder) {
        if (progress != 0 || running) {
            return true;
        }
        if (processCancelled) {
            return false;
        }
        if (level instanceof ServerLevel server
                && OptionalIntegrations.fireWorkshopProcessingStarting(server, this, holder.id(), holder.value(),
                        input.copy(), catalyst.copy(), holder.value().result())) {
            processCancelled = true;
            sync();
            return false;
        }
        return true;
    }

    private void complete(RecipeHolder<WorkshopRecipe> holder) {
        WorkshopRecipe recipe = holder.value();
        ItemStack eventInput = input.copy();
        ItemStack eventCatalyst = catalyst.copy();
        input.shrink(recipe.inputCount());
        if (input.isEmpty()) {
            input = ItemStack.EMPTY;
        }
        if (recipe.consumeCatalyst() && recipe.hasCatalyst()) {
            catalyst.shrink(recipe.catalystCount());
            if (catalyst.isEmpty()) {
                catalyst = ItemStack.EMPTY;
            }
        }
        output = recipe.result().copy();
        if (station().equals(WorkshopRecipe.STONE_ANVIL)) ItemHeat.copy(eventInput, output, level);
        input.remove(ModDataComponents.FORGE_PROGRESS.get());
        forgeHeat = 0;
        progress = 0;
        running = false;
        processCancelled = false;
        if (level instanceof ServerLevel server) {
            OptionalIntegrations.fireWorkshopProcessingCompleted(server, this, holder.id(), recipe,
                    eventInput, eventCatalyst, output);
        }
        if (level != null) {
            level.playSound(null, worldPosition, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.22F, 1.65F);
        }
        sync();
    }

    public Optional<RecipeHolder<WorkshopRecipe>> activeRecipe() {
        return matchingRecipes().findFirst();
    }

    public int getMatchingRecipeCount() {
        return (int) matchingRecipes().count();
    }

    private Stream<RecipeHolder<WorkshopRecipe>> matchingRecipes() {
        if (level == null || input.isEmpty()) {
            return Stream.empty();
        }
        return stationRecipes()
                .filter(holder -> holder.value().ingredient().test(input)
                        && input.getCount() >= holder.value().inputCount())
                .filter(holder -> holder.value().catalystMatches(catalyst))
                .filter(holder -> {
                    ForgeProgress saved = input.get(ModDataComponents.FORGE_PROGRESS.get());
                    return saved == null || holder.id().toString().equals(saved.recipe());
                })
                .sorted(Comparator.comparingInt((RecipeHolder<WorkshopRecipe> holder) -> holder.value().priority())
                        .reversed()
                        .thenComparing(Comparator.comparingInt(
                                (RecipeHolder<WorkshopRecipe> holder) -> holder.value().inputCount()).reversed())
                        .thenComparingInt(holder -> holder.value().hasCatalyst() ? 0 : 1)
                        .thenComparing(holder -> holder.id().toString()));
    }

    private Optional<ResourceLocation> activeRecipeId() {
        return activeRecipe().map(RecipeHolder::id);
    }

    private Stream<RecipeHolder<WorkshopRecipe>> stationRecipes() {
        if (level == null) {
            return Stream.empty();
        }
        String station = station();
        return level.getRecipeManager().getAllRecipesFor(ModRecipes.WORKSHOP_PROCESSING_TYPE.get()).stream()
                .filter(holder -> holder.value().station().equals(station));
    }

    private boolean isFuel(ItemStack stack) {
        return stack.getBurnTime(RecipeType.SMELTING) > 0;
    }

    private boolean heated() {
        return station().equals(WorkshopRecipe.CRUCIBLE_FURNACE) || station().equals(WorkshopRecipe.KILN);
    }

    private boolean validInput(ItemStack stack) {
        if (station().equals(WorkshopRecipe.KILN)) return ItemHeat.capacity(stack, level) > 0;
        return stationRecipes().anyMatch(holder -> holder.value().ingredient().test(stack));
    }

    private boolean validCatalyst(ItemStack stack) {
        return stationRecipes().anyMatch(holder -> holder.value().catalystIngredientMatches(stack));
    }

    private static boolean canStack(ItemStack stored, ItemStack stack) {
        return stored.isEmpty()
                || ItemStack.isSameItemSameComponents(stored, stack) && stored.getCount() < stored.getMaxStackSize();
    }

    private boolean canInsertInput(ItemStack stack) {
        if (station().equals(WorkshopRecipe.STONE_ANVIL) && progress > 0) return false;
        if (station().equals(WorkshopRecipe.KILN) && stack.has(ModDataComponents.FORGE_PROGRESS.get())
                && (!input.isEmpty() || stack.getCount() != stack.get(ModDataComponents.FORGE_PROGRESS.get()).batchSize()
                || stack.getCount() > stack.getMaxStackSize()
                || stack.get(ModDataComponents.FORGE_PROGRESS.get()).completed() <= 0)) return false;
        return validInput(stack) && compatibleInput(stack) && input.getCount() < inputLimit(stack);
    }

    /** Only anvil assembly ignores heat; inventory stacking and every other component stay strict. */
    private boolean compatibleInput(ItemStack stack) {
        if (input.isEmpty()) return true;
        if (!station().equals(WorkshopRecipe.STONE_ANVIL)) return canStack(input, stack);
        ItemStack stored = input.copy(), incoming = stack.copy();
        stored.remove(ModDataComponents.HEAT.get());
        incoming.remove(ModDataComponents.HEAT.get());
        return canStack(stored, incoming);
    }

    private ItemStack assembleInput(ItemStack incoming, int count) {
        if (input.isEmpty()) return incoming.copyWithCount(count);
        ItemStack combined = input.copyWithCount(input.getCount() + count);
        if (station().equals(WorkshopRecipe.STONE_ANVIL)) {
            double temperature = Math.min(ItemHeat.celsius(input, level), ItemHeat.celsius(incoming, level));
            ItemHeat first = input.get(ModDataComponents.HEAT.get()), second = incoming.get(ModDataComponents.HEAT.get());
            combined.remove(ModDataComponents.HEAT.get());
            if (first != null && second != null && temperature > ThermalModel.AMBIENT)
                ItemHeat.setTemperature(combined, level, temperature, Math.min(first.capacity(), second.capacity()));
        }
        return combined;
    }

    private int inputLimit(ItemStack stack) {
        if (station().equals(WorkshopRecipe.KILN)) {
            ForgeProgress saved = stack.get(ModDataComponents.FORGE_PROGRESS.get());
            return saved == null ? 1 : Math.min(stack.getMaxStackSize(), saved.batchSize());
        }
        if (!station().equals(WorkshopRecipe.STONE_ANVIL)) return stack.getMaxStackSize();
        ForgeProgress saved = stack.get(ModDataComponents.FORGE_PROGRESS.get());
        if (saved != null) return saved.batchSize();
        return stationRecipes().filter(h -> h.value().ingredient().test(stack))
                .mapToInt(h -> h.value().inputCount()).max().orElse(stack.getMaxStackSize());
    }

    private boolean canInsertCatalyst(ItemStack stack) {
        return !(station().equals(WorkshopRecipe.STONE_ANVIL) && progress > 0) && validCatalyst(stack)
                && canStack(catalyst, stack) && catalyst.getCount() < catalystLimit(stack);
    }

    private int catalystLimit(ItemStack stack) {
        return station().equals(WorkshopRecipe.CRUCIBLE_FURNACE) ? 1 : stack.getMaxStackSize();
    }

    public boolean canInsertFuel(ItemStack stack) {
        return heated() && isFuel(stack) && canStack(fuel, stack);
    }

    private boolean recipeNeedsMoreInput(ItemStack stack) {
        if (input.isEmpty() || !compatibleInput(stack)) {
            return false;
        }
        return stationRecipes().anyMatch(holder -> holder.value().ingredient().test(input)
                && holder.value().ingredient().test(stack)
                && input.getCount() < holder.value().inputCount());
    }

    private boolean loadedRecipeNeedsCatalyst(ItemStack stack) {
        if (input.isEmpty()) {
            return false;
        }
        return stationRecipes().anyMatch(holder -> holder.value().ingredient().test(input)
                && input.getCount() >= holder.value().inputCount()
                && holder.value().catalystIngredientMatches(stack)
                && catalyst.getCount() < holder.value().catalystCount());
    }

    private int preferredPlayerInsertionSlot(ItemStack stack) {
        if (stack.isEmpty()) {
            return -1;
        }
        if (!output.isEmpty()) return canInsertFuel(stack) ? FUEL_SLOT : -1;

        boolean inputCandidate = canInsertInput(stack);
        boolean catalystCandidate = canInsertCatalyst(stack);
        boolean fuelCandidate = canInsertFuel(stack);

        if (inputCandidate && (input.isEmpty() || recipeNeedsMoreInput(stack))) {
            return INPUT_SLOT;
        }
        if (catalystCandidate && loadedRecipeNeedsCatalyst(stack)) {
            return CATALYST_SLOT;
        }
        if (inputCandidate) {
            return INPUT_SLOT;
        }
        if (catalystCandidate) {
            return CATALYST_SLOT;
        }
        return fuelCandidate ? FUEL_SLOT : -1;
    }

    public boolean canInsert(ItemStack stack) {
        return preferredPlayerInsertionSlot(stack) >= 0;
    }

    public boolean insert(ItemStack held, boolean creative) {
        int slot = preferredPlayerInsertionSlot(held);
        if (slot == INPUT_SLOT && (station().equals(WorkshopRecipe.STONE_ANVIL) || station().equals(WorkshopRecipe.KILN))
                && input.isEmpty() && held.has(ModDataComponents.FORGE_PROGRESS.get())) {
            int count = held.get(ModDataComponents.FORGE_PROGRESS.get()).batchSize();
            if (held.getCount() < count) return false;
            input = held.copyWithCount(count);
            if (!creative) held.shrink(count);
            restoreForgeProgress();
            sync();
            return true;
        }
        return slot >= 0 && insertOne(slot, held, creative);
    }

    public boolean insertFuel(ItemStack held, boolean creative) {
        return canInsertFuel(held) && insertOne(FUEL_SLOT, held, creative);
    }

    private boolean insertOne(int slot, ItemStack held, boolean creative) {
        if (held.isEmpty() || !isSlotValid(slot, held)) {
            return false;
        }

        Optional<ResourceLocation> previousRecipe = slot == FUEL_SLOT ? Optional.empty() : activeRecipeId();
        if (slot == INPUT_SLOT) {
            input = assembleInput(held, 1);
        } else if (slot == CATALYST_SLOT) {
            catalyst = addOne(catalyst, held);
        } else if (slot == FUEL_SLOT) {
            fuel = addOne(fuel, held);
        } else {
            return false;
        }

        if (!creative) {
            held.shrink(1);
        }

        processCancelled = false;
        resetProcessingIfRecipeChanged(slot, previousRecipe);
        sync();
        return true;
    }

    private void resetProcessingIfRecipeChanged(int slot, Optional<ResourceLocation> previousRecipe) {
        if (slot == FUEL_SLOT) {
            return;
        }
        if (!heated() || !previousRecipe.equals(activeRecipeId())) {
            progress = 0;
            forgeHeat = 0;
            lastForgeAction = "";
            running = false;
            if (station().equals(WorkshopRecipe.STONE_ANVIL)) restoreForgeProgress();
        }
    }

    private static ItemStack addOne(ItemStack target, ItemStack source) {
        if (target.isEmpty()) {
            return source.copyWithCount(1);
        }
        target.grow(1);
        return target;
    }

    public boolean takeOutput(Player player) {
        if (output.isEmpty()) {
            return false;
        }
        player.getInventory().placeItemBackInInventory(output.copy());
        output = ItemStack.EMPTY;
        processCancelled = false;
        sync();
        return true;
    }

    public boolean takeStored(Player player) {
        ItemStack stack;
        if (!catalyst.isEmpty()) {
            stack = catalyst;
            catalyst = ItemStack.EMPTY;
        } else if (!input.isEmpty()) {
            stack = input;
            input = ItemStack.EMPTY;
        } else if (!fuel.isEmpty()) {
            stack = fuel;
            fuel = ItemStack.EMPTY;
        } else {
            return false;
        }
        player.getInventory().placeItemBackInInventory(stack.copy());
        forgeHeat = 0;
        lastForgeAction = "";
        progress = 0;
        running = false;
        processCancelled = false;
        sync();
        return true;
    }

    public List<ItemStack> allStacks() {
        return List.of(input, catalyst, fuel, output);
    }

    public IItemHandler getItemHandler(@Nullable Direction side) {
        return handler;
    }

    public int getProgress() {
        return progress;
    }

    public int getStokeTicks() {
        return stokeTicks;
    }

    public ItemStack getInput() {
        return input;
    }

    public ItemStack getCatalyst() {
        return catalyst;
    }

    public ItemStack getFuel() {
        return fuel;
    }

    public ItemStack getOutput() {
        return output;
    }

    public boolean isRunning() {
        return running;
    }

    public float getProgressFraction() {
        if (!output.isEmpty()) {
            return 1.0F;
        }
        return activeRecipe()
                .map(holder -> Mth.clamp((float) progress / Math.max(1, holder.value().requiredWork()), 0.0F, 1.0F))
                .orElse(0.0F);
    }

    public float getWheelRotation(float partialTick) {
        if (level != null && level.isClientSide) {
            double interpolated = clientPrevRotation + (clientRotation - clientPrevRotation) * partialTick;
            return (float) (interpolated % 360.0D);
        }
        return (float) ((actionSteps * 72L) % 360L);
    }

    public float getActionPulse(float partialTick) {
        if (level == null || clientActionTick == Long.MIN_VALUE) {
            return 0.0F;
        }
        float elapsed = (float) (level.getGameTime() + partialTick - clientActionTick);
        if (elapsed < 0.0F || elapsed >= 6.0F) {
            return 0.0F;
        }
        return Mth.sin((float) Math.PI * Mth.clamp(elapsed / 6.0F, 0.0F, 1.0F));
    }

    private void sync() {
        setChanged();
        updateHeatLight();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.saveAdditional(tag, regs);
        tag.put("Input", input.saveOptional(regs));
        tag.put("Catalyst", catalyst.saveOptional(regs));
        tag.put("Fuel", fuel.saveOptional(regs));
        tag.put("Output", output.saveOptional(regs));
        tag.putInt("Progress", progress);
        tag.putInt("StokeTicks", stokeTicks);
        tag.putInt("ForgeHeat", forgeHeat);
        tag.putInt("BurnTicks", burnTicks);
        tag.putBoolean("FuelBurnVersion", true);
        tag.putDouble("TemperatureCelsius", temperatureCelsius);
        tag.putLong("LastBellowsTick", lastBellowsTick);
        tag.putBoolean("Ignited", ignited);
        tag.putDouble("HeatRemainder", heatRemainder);
        tag.putBoolean("ItemHeatVersion", !importItemState);
        tag.putLong("LastForgeTick", lastForgeTick);
        tag.putString("LastForgeAction", lastForgeAction);
        tag.putBoolean("LegacyForgeProgressPending", legacyForgeProgressPending);
        tag.putBoolean("Running", running);
        tag.putBoolean("ProcessCancelled", processCancelled);
        tag.putLong("ActionSteps", actionSteps);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.loadAdditional(tag, regs);
        input = ItemStack.parseOptional(regs, tag.getCompound("Input"));
        catalyst = ItemStack.parseOptional(regs, tag.getCompound("Catalyst"));
        fuel = ItemStack.parseOptional(regs, tag.getCompound("Fuel"));
        output = ItemStack.parseOptional(regs, tag.getCompound("Output"));
        progress = tag.getInt("Progress");
        stokeTicks = Math.max(0, tag.getInt("StokeTicks"));
        temperatureCelsius = tag.contains("TemperatureCelsius")
                ? Mth.clamp(tag.getDouble("TemperatureCelsius"), ThermalModel.AMBIENT, 5000) : ThermalModel.AMBIENT;
        lastBellowsTick = tag.contains("LastBellowsTick") ? tag.getLong("LastBellowsTick") : Long.MIN_VALUE;
        boolean hasModernForgeState = tag.contains("ForgeHeat") || tag.contains("LastForgeTick")
                || tag.contains("LastForgeAction");
        forgeHeat = Math.max(0, tag.getInt("ForgeHeat"));
        burnTicks = Math.max(0, tag.getInt("BurnTicks"));
        heatRemainder = Math.max(0, Math.min(1, tag.getDouble("HeatRemainder")));
        importItemState = !tag.getBoolean("ItemHeatVersion");
        lastForgeTick = tag.contains("LastForgeTick") ? tag.getLong("LastForgeTick") : Long.MIN_VALUE;
        lastForgeAction = tag.getString("LastForgeAction");
        legacyForgeProgressPending = tag.getBoolean("LegacyForgeProgressPending")
                || (!hasModernForgeState && progress > 0);
        importItemState |= legacyForgeProgressPending;
        running = tag.getBoolean("Running");
        // Preserve an already-paid legacy crucible batch, but never light reserve fuel on load.
        ignited = tag.contains("Ignited") ? tag.getBoolean("Ignited") : running && stokeTicks > 0;
        if (station().equals(WorkshopRecipe.CRUCIBLE_FURNACE) && !tag.getBoolean("FuelBurnVersion")
                && running && ignited && burnTicks == 0) {
            // Old saves paid one charcoal per batch. Preserve that paid fire without consuming reserve fuel.
            burnTicks = new ItemStack(net.minecraft.world.item.Items.CHARCOAL).getBurnTime(RecipeType.SMELTING);
        }
        processCancelled = tag.getBoolean("ProcessCancelled");
        long loadedActionSteps = tag.getLong("ActionSteps");
        if (level != null && level.isClientSide) {
            if (clientObservedActionSteps != Long.MIN_VALUE && loadedActionSteps != clientObservedActionSteps) {
                clientActionTick = level.getGameTime();
            }
            clientObservedActionSteps = loadedActionSteps;
            rotationTarget = loadedActionSteps * 72.0D;
            if (!clientRotationInitialized) {
                clientRotation = rotationTarget;
                clientPrevRotation = rotationTarget;
                clientRotationInitialized = true;
            }
        }
        actionSteps = loadedActionSteps;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider regs) {
        return saveWithoutMetadata(regs);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this, BlockEntity::getUpdateTag);
    }

    private boolean isSlotValid(int slot, ItemStack stack) {
        if (stack.isEmpty() || slot != FUEL_SLOT && !output.isEmpty()) {
            return false;
        }
        return switch (slot) {
            case INPUT_SLOT -> canInsertInput(stack);
            case CATALYST_SLOT -> canInsertCatalyst(stack);
            case FUEL_SLOT -> canInsertFuel(stack);
            default -> false;
        };
    }

    private final class WorkshopItemHandler implements IItemHandler {
        @Override
        public int getSlots() {
            return 4;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return switch (slot) {
                case INPUT_SLOT -> input.copy();
                case CATALYST_SLOT -> catalyst.copy();
                case FUEL_SLOT -> fuel.copy();
                case OUTPUT_SLOT -> output.copy();
                default -> ItemStack.EMPTY;
            };
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (stack.isEmpty() || !isItemValid(slot, stack)) {
                return stack;
            }
            ItemStack current = getStackInSlot(slot);
            if (!current.isEmpty() && !(slot == INPUT_SLOT ? compatibleInput(stack) : ItemStack.isSameItemSameComponents(current, stack))) {
                return stack;
            }
            int limit = slot == INPUT_SLOT ? inputLimit(stack)
                    : slot == CATALYST_SLOT ? catalystLimit(stack) : stack.getMaxStackSize();
            int accepted = Math.min(limit - current.getCount(), stack.getCount());
            if (accepted <= 0) {
                return stack;
            }
            if (!simulate) {
                Optional<ResourceLocation> previousRecipe = slot == FUEL_SLOT ? Optional.empty() : activeRecipeId();
                ItemStack target = slot == INPUT_SLOT ? assembleInput(stack, accepted) : current.isEmpty()
                        ? stack.copyWithCount(accepted)
                        : current.copyWithCount(current.getCount() + accepted);
                if (slot == INPUT_SLOT) {
                    input = target;
                } else if (slot == CATALYST_SLOT) {
                    catalyst = target;
                } else {
                    fuel = target;
                }

                processCancelled = false;
                resetProcessingIfRecipeChanged(slot, previousRecipe);
                sync();
            }
            return stack.copyWithCount(stack.getCount() - accepted);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot == INPUT_SLOT && (station().equals(WorkshopRecipe.KILN) || station().equals(WorkshopRecipe.STONE_ANVIL))) {
                if (amount <= 0 || input.isEmpty()) return ItemStack.EMPTY;
                if (input.has(ModDataComponents.FORGE_PROGRESS.get()) && amount < input.getCount()) return ItemStack.EMPTY;
                int count = Math.min(amount, input.getCount());
                ItemStack result = input.copyWithCount(count);
                if (!simulate) {
                    input.shrink(count);
                    restoreForgeProgress();
                    lastForgeAction = "";
                    processCancelled = false;
                    sync();
                }
                return result;
            }
            if (slot != OUTPUT_SLOT || output.isEmpty() || amount <= 0) {
                return ItemStack.EMPTY;
            }
            int count = Math.min(amount, output.getCount());
            ItemStack result = output.copyWithCount(count);
            if (!simulate) {
                output.shrink(count);
                if (output.isEmpty()) {
                    output = ItemStack.EMPTY;
                }
                processCancelled = false;
                sync();
            }
            return result;
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot == INPUT_SLOT && station().equals(WorkshopRecipe.KILN)
                    ? input.isEmpty() ? 1 : inputLimit(input)
                    : slot == CATALYST_SLOT && station().equals(WorkshopRecipe.CRUCIBLE_FURNACE) ? 1 : 64;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return isSlotValid(slot, stack);
        }
    }
}
