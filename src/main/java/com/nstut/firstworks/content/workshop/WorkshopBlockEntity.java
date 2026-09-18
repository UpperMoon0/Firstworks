package com.nstut.firstworks.content.workshop;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.block.CampfireBlock;
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
    private int forgeHeat;
    private long lastForgeTick = Long.MIN_VALUE;
    private String lastForgeAction = "";
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
        if (workshop.forgeHeat > 0) {
            workshop.forgeHeat--;
            workshop.setChanged();
            if (workshop.forgeHeat % 20 == 0) workshop.sync();
        }
        boolean stokeExpired = false;
        if (workshop.stokeTicks > 0) {
            workshop.stokeTicks--;
            stokeExpired = workshop.stokeTicks == 0;
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
        if (workshop.stokeTicks <= 0) {
            if (stokeExpired) {
                workshop.sync();
            }
            return;
        }

        boolean started = false;
        if (!workshop.running) {
            if (workshop.fuel.isEmpty()) {
                if (stokeExpired) {
                    workshop.sync();
                }
                return;
            }
            if (!workshop.tryBegin(holder.get())) {
                return;
            }
            workshop.fuel.shrink(1);
            if (workshop.fuel.isEmpty()) {
                workshop.fuel = ItemStack.EMPTY;
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

    public boolean stoke(int ticks) {
        if (!station().equals(WorkshopRecipe.CRUCIBLE_FURNACE)) {
            return false;
        }
        processCancelled = false;
        stokeTicks = Math.max(stokeTicks, Math.max(1, ticks));
        sync();
        return true;
    }

    public boolean isHot() {
        return heated() && running && stokeTicks > 0 && !input.isEmpty() && output.isEmpty();
    }

    public int getForgeHeat() { return forgeHeat; }
    public String getLastForgeAction() { return lastForgeAction; }

    public Component anvilHint(String action, boolean reheat, boolean hammer) {
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
        if (reheat || forge.get().heatTicks() > 0 && forgeHeat == 0)
            return Component.translatable(hasForgeHeatSource() ? "hint.firstworks.anvil.reheat_ready" : "hint.firstworks.anvil.reheat");
        String next = forge.get().actions().get(Math.min(progress, forge.get().actions().size() - 1));
        if (forge.get().heatTicks() == 0) return Component.translatable("hint.firstworks.anvil.action_cold",
                Component.translatable("action.firstworks." + action), Component.translatable("action.firstworks." + next));
        return Component.translatable("hint.firstworks.anvil.action",
                Component.translatable("action.firstworks." + action),
                Component.translatable("action.firstworks." + next), (forgeHeat + 19) / 20);
    }

    public boolean hasForgeHeatSource() {
        if (level == null) return false;
        for (Direction side : Direction.values()) {
            BlockPos source = worldPosition.relative(side);
            var state = level.getBlockState(source);
            if (state.getBlock() instanceof CampfireBlock
                    && state.getValue(CampfireBlock.LIT)) return true;
            if (level.getBlockEntity(source) instanceof WorkshopBlockEntity furnace && furnace.isHot()) return true;
        }
        return false;
    }

    public boolean reheat(Player player) {
        if (!(level instanceof ServerLevel) || !hasHammer(player) || !hasForgeHeatSource() || !output.isEmpty()) return false;
        var data = activeRecipe().flatMap(h -> h.value().forge());
        if (data.isEmpty() || data.get().heatTicks() == 0) return false;
        forgeHeat = data.get().heatTicks();
        level.playSound(null, worldPosition, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.4F, 1.1F);
        sync();
        return true;
    }

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
                || data.heatTicks() > 0 && forgeHeat <= 0) return false;
        processCancelled = false;
        if (!tryBegin(active.get())) return false;
        lastForgeTick = level.getGameTime();
        lastForgeAction = action;
        progress++;
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
        if (level == null || input.isEmpty()) {
            return Optional.empty();
        }
        return stationRecipes()
                .filter(holder -> holder.value().ingredient().test(input)
                        && input.getCount() >= holder.value().inputCount())
                .filter(holder -> holder.value().catalystMatches(catalyst))
                .sorted(Comparator.comparingInt((RecipeHolder<WorkshopRecipe> holder) -> holder.value().priority())
                        .reversed()
                        .thenComparing(Comparator.comparingInt(
                                (RecipeHolder<WorkshopRecipe> holder) -> holder.value().inputCount()).reversed())
                        .thenComparingInt(holder -> holder.value().hasCatalyst() ? 0 : 1)
                        .thenComparing(holder -> holder.id().toString()))
                .findFirst();
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
        return stack.is(ModTags.CRUCIBLE_FURNACE_FUELS);
    }

    private boolean heated() {
        return station().equals(WorkshopRecipe.CRUCIBLE_FURNACE);
    }

    private boolean validInput(ItemStack stack) {
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
        return !(station().equals(WorkshopRecipe.STONE_ANVIL) && progress > 0) && validInput(stack) && canStack(input, stack);
    }

    private boolean canInsertCatalyst(ItemStack stack) {
        return !(station().equals(WorkshopRecipe.STONE_ANVIL) && progress > 0) && validCatalyst(stack) && canStack(catalyst, stack);
    }

    public boolean canInsertFuel(ItemStack stack) {
        return heated() && isFuel(stack) && canStack(fuel, stack) && output.isEmpty();
    }

    private boolean recipeNeedsMoreInput(ItemStack stack) {
        if (input.isEmpty() || !ItemStack.isSameItemSameComponents(input, stack)) {
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
        if (stack.isEmpty() || !output.isEmpty()) {
            return -1;
        }

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
            input = addOne(input, held);
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
        tag.putLong("LastForgeTick", lastForgeTick);
        tag.putString("LastForgeAction", lastForgeAction);
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
        stokeTicks = tag.getInt("StokeTicks");
        forgeHeat = Math.max(0, tag.getInt("ForgeHeat"));
        lastForgeTick = tag.contains("LastForgeTick") ? tag.getLong("LastForgeTick") : Long.MIN_VALUE;
        lastForgeAction = tag.getString("LastForgeAction");
        running = tag.getBoolean("Running");
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
        if (stack.isEmpty() || !output.isEmpty()) {
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
            if (!current.isEmpty() && !ItemStack.isSameItemSameComponents(current, stack)) {
                return stack;
            }
            int accepted = Math.min(stack.getMaxStackSize() - current.getCount(), stack.getCount());
            if (accepted <= 0) {
                return stack;
            }
            if (!simulate) {
                Optional<ResourceLocation> previousRecipe = slot == FUEL_SLOT ? Optional.empty() : activeRecipeId();
                ItemStack target = current.isEmpty()
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
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return isSlotValid(slot, stack);
        }
    }
}
