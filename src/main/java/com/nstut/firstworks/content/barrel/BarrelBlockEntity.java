package com.nstut.firstworks.content.barrel;

import com.nstut.firstworks.FirstworksConfig;
import com.nstut.firstworks.compat.OptionalIntegrations;
import com.nstut.firstworks.content.ColoredFleeceItem;
import com.nstut.firstworks.registry.ModBlockEntities;
import com.nstut.firstworks.registry.ModDataComponents;
import com.nstut.firstworks.registry.ModFluids;
import com.nstut.firstworks.registry.ModItems;
import com.nstut.firstworks.registry.ModRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

public class BarrelBlockEntity extends BlockEntity {
    public static final int CAPACITY = 4_000;

    private final FluidTank inputTank;
    private final FluidTank outputTank;
    private final IFluidHandler inputFluidHandler;
    private final IFluidHandler outputFluidHandler;
    private final IFluidHandler combinedFluidHandler;
    private ItemStack ingredient = ItemStack.EMPTY;
    private ItemStack output = ItemStack.EMPTY;
    private int progress;
    private boolean processCancelled;
    private final IItemHandler inputHandler = new BarrelItemHandler(true, false);
    private final IItemHandler outputHandler = new BarrelItemHandler(false, true);
    private final IItemHandler combinedHandler = new BarrelItemHandler(true, true);

    public BarrelBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BARREL.get(), pos, state);
        inputTank = new FluidTank(CAPACITY) {
            @Override
            public int fill(FluidStack resource, FluidAction action) {
                if (resource.isEmpty()) return 0;
                if (!isEmpty() && !getFluid().is(resource.getFluid())) return 0;
                int space = getAvailableFluidSpace();
                if (space <= 0) return 0;
                return super.fill(resource.copyWithAmount(Math.min(resource.getAmount(), space)), action);
            }

            @Override
            protected void onContentsChanged() {
                onInputChanged();
            }
        };
        outputTank = new FluidTank(CAPACITY) {
            @Override
            public int fill(FluidStack resource, FluidAction action) {
                if (resource.isEmpty()) return 0;
                int space = getAvailableFluidSpace();
                if (space <= 0) return 0;
                return super.fill(resource.copyWithAmount(Math.min(resource.getAmount(), space)), action);
            }

            @Override
            protected void onContentsChanged() {
                onOutputChanged();
            }
        };
        inputFluidHandler = new IFluidHandler() {
            @Override public int getTanks() { return 1; }
            @Override public FluidStack getFluidInTank(int tankIndex) {
                return tankIndex == 0 ? inputTank.getFluid() : FluidStack.EMPTY;
            }
            @Override public int getTankCapacity(int tankIndex) {
                return tankIndex == 0 ? CAPACITY : 0;
            }
            @Override public boolean isFluidValid(int tankIndex, FluidStack stack) {
                return tankIndex == 0 && inputTank.isFluidValid(stack);
            }
            @Override public int fill(FluidStack resource, FluidAction action) {
                return isSealed() ? 0 : inputTank.fill(resource, action);
            }
            @Override public FluidStack drain(FluidStack resource, FluidAction action) {
                if (isSealed() || resource.isEmpty() || inputTank.isEmpty() || !inputTank.getFluid().is(resource.getFluid()))
                    return FluidStack.EMPTY;
                return inputTank.drain(resource, action);
            }
            @Override public FluidStack drain(int maxDrain, FluidAction action) {
                return isSealed() ? FluidStack.EMPTY : inputTank.drain(maxDrain, action);
            }
        };
        outputFluidHandler = new IFluidHandler() {
            @Override public int getTanks() { return 1; }
            @Override public FluidStack getFluidInTank(int tankIndex) {
                return tankIndex == 0 ? outputTank.getFluid() : FluidStack.EMPTY;
            }
            @Override public int getTankCapacity(int tankIndex) {
                return tankIndex == 0 ? CAPACITY : 0;
            }
            @Override public boolean isFluidValid(int tankIndex, FluidStack stack) {
                return false;
            }
            @Override public int fill(FluidStack resource, FluidAction action) {
                return 0;
            }
            @Override public FluidStack drain(FluidStack resource, FluidAction action) {
                if (isSealed() || resource.isEmpty() || outputTank.isEmpty() || !outputTank.getFluid().is(resource.getFluid()))
                    return FluidStack.EMPTY;
                return outputTank.drain(resource, action);
            }
            @Override public FluidStack drain(int maxDrain, FluidAction action) {
                return isSealed() ? FluidStack.EMPTY : outputTank.drain(maxDrain, action);
            }
        };
        combinedFluidHandler = new IFluidHandler() {
            @Override public int getTanks() { return 2; }
            @Override public FluidStack getFluidInTank(int tankIndex) {
                return switch (tankIndex) {
                    case 0 -> inputTank.getFluid();
                    case 1 -> outputTank.getFluid();
                    default -> FluidStack.EMPTY;
                };
            }
            @Override public int getTankCapacity(int tankIndex) {
                return switch (tankIndex) {
                    case 0, 1 -> CAPACITY;
                    default -> 0;
                };
            }
            @Override public boolean isFluidValid(int tankIndex, FluidStack stack) {
                return tankIndex == 0 && inputTank.isFluidValid(stack);
            }
            @Override public int fill(FluidStack resource, FluidAction action) {
                return isSealed() ? 0 : inputTank.fill(resource, action);
            }
            @Override public FluidStack drain(FluidStack resource, FluidAction action) {
                if (isSealed() || resource.isEmpty()) return FluidStack.EMPTY;
                if (!outputTank.isEmpty() && outputTank.getFluid().is(resource.getFluid()))
                    return outputTank.drain(resource, action);
                if (!inputTank.isEmpty() && inputTank.getFluid().is(resource.getFluid()))
                    return inputTank.drain(resource, action);
                return FluidStack.EMPTY;
            }
            @Override public FluidStack drain(int maxDrain, FluidAction action) {
                if (isSealed()) return FluidStack.EMPTY;
                FluidTank source = !outputTank.isEmpty() ? outputTank : inputTank;
                return source.drain(maxDrain, action);
            }
        };
    }

    public static void tick(Level level, BlockPos pos, BlockState state, BarrelBlockEntity barrel) {
        if (level.isClientSide) return;
        Process process = barrel.currentProcess();
        if (process == null || !barrel.output.isEmpty()) {
            barrel.progress = 0;
            return;
        }
        if (barrel.progress == 0 && level instanceof ServerLevel serverLevel
                && OptionalIntegrations.fireBarrelProcessStarting(serverLevel, barrel, process.recipeId(), process.recipe(),
                        process.input(), process.inputFluid(), process.result(), process.outputFluidStack())) {
            barrel.processCancelled = true;
            barrel.setChangedAndSync();
            return;
        }
        barrel.progress++;
        if (barrel.progress >= process.duration()) {
            barrel.complete(process);
        } else if (barrel.progress % 200 == 0) {
            barrel.setChangedAndSync();
        }
    }

    private static int RECIPE_VERSION = 0;
    private int localRecipeVersion = -1;
    private Boolean cachedSealed = null;
    private Process cachedProcess = null;
    private boolean processDirty = true;

    public static void invalidateAllBarrels() {
        RECIPE_VERSION++;
    }

    public void onLidChanged(boolean sealed) {
        processCancelled = false;
        progress = 0;
        cachedSealed = sealed;
        invalidateProcess();
        setChangedAndSync();
    }

    public void invalidateProcess() {
        processDirty = true;
        cachedProcess = null;
    }

    private Process currentProcess() {
        if (localRecipeVersion != RECIPE_VERSION) {
            localRecipeVersion = RECIPE_VERSION;
            processDirty = true;
        }
        boolean sealed = getBlockState().getValue(BarrelBlock.SEALED);
        if (cachedSealed == null || cachedSealed != sealed) {
            cachedSealed = sealed;
            processDirty = true;
        }
        if (ingredient.isEmpty() || level == null || inputTank.isEmpty() || processCancelled || !output.isEmpty()) {
            cachedProcess = null;
            processDirty = false;
            return null;
        }
        if (processDirty) {
            cachedProcess = computeProcess();
            processDirty = false;
        }
        return cachedProcess;
    }

    private Process computeProcess() {
        if (ingredient.isEmpty() || level == null || inputTank.isEmpty() || processCancelled || !output.isEmpty()) return null;
        for (RecipeHolder<BarrelRecipe> holder : level.getRecipeManager().getAllRecipesFor(ModRecipes.BARREL_PROCESSING_TYPE.get())) {
            BarrelRecipe recipe = holder.value();
            if (!recipe.matches(new SingleRecipeInput(ingredient), level) || !recipe.matchesFluid(inputTank.getFluid())) continue;
            if (recipe.sealed() != getBlockState().getValue(BarrelBlock.SEALED)) continue;
            int batches = Math.min(ingredient.getCount() / recipe.inputCount(), inputTank.getFluidAmount() / recipe.fluidAmount());
            if (batches <= 0) continue;

            ItemStack perBatch = recipe.result().isEmpty() ? ItemStack.EMPTY : recipe.result().copy();
            if (!perBatch.isEmpty()) {
                if (!output.isEmpty() && (!ItemStack.isSameItemSameComponents(output, perBatch)
                        || output.getCount() + perBatch.getCount() * batches > output.getMaxStackSize())) continue;
                int itemSpace = output.isEmpty() ? perBatch.getMaxStackSize() : output.getMaxStackSize() - output.getCount();
                batches = Math.min(batches, itemSpace / perBatch.getCount());
                if (batches <= 0) continue;
            }

            Fluid outType = Fluids.EMPTY;
            int outputFluidAmount = recipe.outputFluidAmount();
            if (outputFluidAmount > 0) {
                ResourceLocation outFluid = recipe.outputFluid();
                if (outFluid.equals(BarrelRecipe.NO_FLUID)) continue;
                outType = BuiltInRegistries.FLUID.getOptional(outFluid).orElse(Fluids.EMPTY);
                if (outType == Fluids.EMPTY) continue;
                if (!outputTank.isEmpty() && !outputTank.getFluid().is(outType)) continue;
                int fluidOutputBatches = (CAPACITY - outputTank.getFluidAmount()) / outputFluidAmount;
                batches = Math.min(batches, fluidOutputBatches);
                if (batches <= 0) continue;
                int net = outputFluidAmount - recipe.fluidAmount();
                if (net > 0) {
                    int sharedBatches = (CAPACITY - getTotalFluidAmount()) / net;
                    batches = Math.min(batches, sharedBatches);
                    if (batches <= 0) continue;
                }
            }

            ItemStack result = perBatch.isEmpty() ? ItemStack.EMPTY : perBatch.copyWithCount(perBatch.getCount() * batches);
            if (ingredient.is(ModItems.RAW_FLEECE.get()) && result.is(ModItems.CLEAN_WOOL.get())) {
                DyeColor color = ColoredFleeceItem.color(ingredient);
                if (color != DyeColor.WHITE) result.set(ModDataComponents.FLEECE_COLOR.get(), color);
            }
            FluidStack outputFluidStack = outType == Fluids.EMPTY || outputFluidAmount <= 0
                    ? FluidStack.EMPTY
                    : new FluidStack(outType, outputFluidAmount * batches);
            return new Process(holder.id(), recipe, recipe.inputCount() * batches, recipe.fluidAmount() * batches,
                    ingredient.copyWithCount(recipe.inputCount() * batches),
                    inputTank.getFluid().copyWithAmount(recipe.fluidAmount() * batches),
                    result, outputFluidStack, recipe.duration());
        }
        return null;
    }

    private void complete(Process process) {
        inputTank.drain(process.fluidAmount(), IFluidHandler.FluidAction.EXECUTE);
        ingredient.shrink(process.inputCount());
        if (ingredient.isEmpty()) ingredient = ItemStack.EMPTY;
        if (!process.outputFluidStack().isEmpty()) {
            outputTank.fill(process.outputFluidStack().copy(), IFluidHandler.FluidAction.EXECUTE);
        }
        if (!process.result().isEmpty()) {
            if (output.isEmpty()) output = process.result().copy();
            else output.grow(process.result().getCount());
        }
        progress = 0;
        setChangedAndSync();
        if (level instanceof ServerLevel serverLevel) {
            OptionalIntegrations.fireBarrelProcessCompleted(serverLevel, this, process.recipeId(), process.recipe(),
                    process.input(), process.inputFluid(), process.result(), process.outputFluidStack());
        }
    }

    public void addRainWater() {
        if (!FirstworksConfig.RAIN_FILLS_BARRELS.get()) return;
        if (isSealed()) return;
        if (progress > 0 || processCancelled) return;
        if (!inputTank.isEmpty() && !inputTank.getFluid().is(Fluids.WATER)) return;
        int amount = Math.min(FirstworksConfig.RAIN_FILL_AMOUNT.get(), getAvailableFluidSpace());
        if (amount <= 0) return;
        inputTank.fill(new FluidStack(Fluids.WATER, amount), IFluidHandler.FluidAction.EXECUTE);
    }

    public boolean addInputWater(int amount) {
        return addInputFluid(new FluidStack(Fluids.WATER, amount));
    }

    public boolean addInputFluid(FluidStack fluid) {
        if (fluid.isEmpty()) return false;
        if (!inputTank.isEmpty() && !inputTank.getFluid().is(fluid.getFluid())) return false;
        int space = getAvailableFluidSpace();
        if (space < fluid.getAmount()) return false;
        return inputTank.fill(fluid.copy(), IFluidHandler.FluidAction.EXECUTE) == fluid.getAmount();
    }

    public ItemStack drainBucket() {
        FluidTank source = !outputTank.isEmpty() ? outputTank : inputTank;
        if (source.getFluidAmount() < 1_000) return ItemStack.EMPTY;
        var bucket = source.getFluid().getFluid().getBucket();
        if (bucket == net.minecraft.world.item.Items.AIR) return ItemStack.EMPTY;
        source.drain(1_000, IFluidHandler.FluidAction.EXECUTE);
        return new ItemStack(bucket);
    }

    public ItemStack drainClayBucket() {
        FluidTank source = !outputTank.isEmpty() ? outputTank : inputTank;
        if (source.getFluidAmount() < 1_000) return ItemStack.EMPTY;
        ItemStack filled;
        if (source.getFluid().is(Fluids.WATER)) {
            filled = new ItemStack(ModItems.WATER_CLAY_BUCKET.get());
        } else if (source.getFluid().is(ModFluids.TANNIN_SOLUTION.get())) {
            filled = new ItemStack(ModItems.TANNIN_CLAY_BUCKET.get());
        } else {
            return ItemStack.EMPTY;
        }
        source.drain(1_000, IFluidHandler.FluidAction.EXECUTE);
        return filled;
    }

    public boolean insertIngredient(ItemStack held, boolean creative) {
        if (!ingredient.isEmpty() && !ItemStack.isSameItemSameComponents(ingredient, held)) return false;
        if (ingredient.getCount() >= ingredient.getMaxStackSize()) return false;
        if (ingredient.isEmpty()) ingredient = held.copyWithCount(1);
        else ingredient.grow(1);
        if (!creative) held.shrink(1);
        progress = 0;
        processCancelled = false;
        setChangedAndSync();
        return true;
    }

    public boolean retrieveInput(Player player) {
        if (isSealed() || ingredient.isEmpty()) return false;
        player.getInventory().placeItemBackInInventory(ingredient.copy());
        ingredient = ItemStack.EMPTY;
        progress = 0;
        processCancelled = false;
        setChangedAndSync();
        return true;
    }

    private ItemStack insertIngredientStack(ItemStack stack, boolean simulate) {
        if (!canInsert(stack)) return stack;
        int existing = ingredient.isEmpty() ? 0 : ingredient.getCount();
        int accepted = Math.min(stack.getCount(), stack.getMaxStackSize() - existing);
        if (accepted <= 0) return stack;
        if (!simulate) {
            if (ingredient.isEmpty()) ingredient = stack.copyWithCount(accepted);
            else ingredient.grow(accepted);
            progress = 0;
            processCancelled = false;
            setChangedAndSync();
        }
        return stack.copyWithCount(stack.getCount() - accepted);
    }

    private ItemStack extractOutput(int amount, boolean simulate) {
        if (output.isEmpty() || amount <= 0) return ItemStack.EMPTY;
        int extracted = Math.min(amount, output.getCount());
        ItemStack result = output.copyWithCount(extracted);
        if (!simulate) {
            output.shrink(extracted);
            if (output.isEmpty()) output = ItemStack.EMPTY;
            progress = 0;
            processCancelled = false;
            setChangedAndSync();
        }
        return result;
    }

    public boolean canInsert(ItemStack stack) {
        if (stack.isEmpty() || level == null) return false;
        if (getBlockState().getValue(BarrelBlock.SEALED)) return false;
        if (!ingredient.isEmpty() && !ItemStack.isSameItemSameComponents(ingredient, stack)) return false;
        if (!ingredient.isEmpty() && ingredient.getCount() >= ingredient.getMaxStackSize()) return false;
        return level.getRecipeManager().getAllRecipesFor(ModRecipes.BARREL_PROCESSING_TYPE.get()).stream()
                .anyMatch(holder -> holder.value().ingredient().test(stack));
    }

    public boolean takeOutput(Player player) {
        if (isSealed() || output.isEmpty()) return false;
        player.getInventory().placeItemBackInInventory(output.copy());
        output = ItemStack.EMPTY;
        progress = 0;
        processCancelled = false;
        setChangedAndSync();
        return true;
    }

    public void cancelProcess() {
        progress = 0;
        processCancelled = false;
        setChangedAndSync();
    }

    private void onInputChanged() {
        progress = 0;
        processCancelled = false;
        setChangedAndSync();
    }

    private void onOutputChanged() {
        progress = 0;
        setChangedAndSync();
    }

    private void setChangedAndSync() {
        invalidateProcess();
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public FluidTank getInputTank() { return inputTank; }
    public FluidTank getOutputTank() { return outputTank; }
    public IFluidHandler getFluidHandler(@Nullable Direction side) {
        if (side == Direction.UP) return inputFluidHandler;
        if (side == Direction.DOWN) return outputFluidHandler;
        return combinedFluidHandler;
    }
    public IFluidHandler getAutomationFluidHandler() { return combinedFluidHandler; }
    public IItemHandler getItemHandler(@Nullable Direction side) {
        if (side == Direction.UP) return inputHandler;
        if (side == Direction.DOWN) return outputHandler;
        return combinedHandler;
    }
    public ItemStack getIngredient() { return ingredient; }
    public ItemStack getOutput() { return output; }
    public int getProgress() { return progress; }
    public boolean isProcessCancelled() { return processCancelled; }
    public int getTotalFluidAmount() { return inputTank.getFluidAmount() + outputTank.getFluidAmount(); }
    public int getAvailableFluidSpace() { return CAPACITY - getTotalFluidAmount(); }
    public boolean hasOutputFluid() { return !outputTank.isEmpty(); }
    private boolean isSealed() { return getBlockState().getValue(BarrelBlock.SEALED); }
    public java.util.Optional<RecipeHolder<BarrelRecipe>> getActiveRecipe() {
        Process process = currentProcess();
        if (process == null || level == null) return java.util.Optional.empty();
        return level.getRecipeManager().byKey(process.recipeId())
                .filter(holder -> holder.value() instanceof BarrelRecipe)
                .map(holder -> new RecipeHolder<>(holder.id(), (BarrelRecipe) holder.value()));
    }
    public int getActiveDuration() {
        Process process = currentProcess();
        return process == null ? 0 : process.duration();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("InputTank", inputTank.writeToNBT(registries, new CompoundTag()));
        tag.put("OutputTank", outputTank.writeToNBT(registries, new CompoundTag()));
        tag.put("Ingredient", ingredient.saveOptional(registries));
        tag.put("Output", output.saveOptional(registries));
        tag.putInt("Progress", progress);
        tag.putBoolean("ProcessCancelled", processCancelled);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("InputTank")) {
            inputTank.readFromNBT(registries, tag.getCompound("InputTank"));
            outputTank.readFromNBT(registries, tag.getCompound("OutputTank"));
        } else if (tag.contains("Tank")) {
            inputTank.readFromNBT(registries, tag.getCompound("Tank"));
            outputTank.setFluid(FluidStack.EMPTY);
        }
        ingredient = ItemStack.parseOptional(registries, tag.getCompound("Ingredient"));
        output = ItemStack.parseOptional(registries, tag.getCompound("Output"));
        progress = tag.getInt("Progress");
        processCancelled = tag.getBoolean("ProcessCancelled");
        if (getTotalFluidAmount() > CAPACITY) {
            inputTank.drain(getTotalFluidAmount() - CAPACITY, IFluidHandler.FluidAction.EXECUTE);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this, BlockEntity::getUpdateTag);
    }

    private record Process(net.minecraft.resources.ResourceLocation recipeId, BarrelRecipe recipe,
            int inputCount, int fluidAmount, ItemStack input, FluidStack inputFluid,
            ItemStack result, FluidStack outputFluidStack, int duration) {}

    private final class BarrelItemHandler implements IItemHandler {
        private final boolean allowInput;
        private final boolean allowOutput;

        private BarrelItemHandler(boolean allowInput, boolean allowOutput) {
            this.allowInput = allowInput;
            this.allowOutput = allowOutput;
        }

        @Override public int getSlots() { return 2; }
        @Override public ItemStack getStackInSlot(int slot) {
            return slot == 0 ? ingredient.copy() : slot == 1 ? output.copy() : ItemStack.EMPTY;
        }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return !isSealed() && slot == 0 && allowInput ? insertIngredientStack(stack, simulate) : stack;
        }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return !isSealed() && slot == 1 && allowOutput ? extractOutput(amount, simulate) : ItemStack.EMPTY;
        }
        @Override public int getSlotLimit(int slot) { return 64; }
        @Override public boolean isItemValid(int slot, ItemStack stack) {
            return !isSealed() && slot == 0 && allowInput && canInsert(stack);
        }
    }
}
