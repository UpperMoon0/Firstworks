package com.nstut.firstworks.content.barrel;

import com.nstut.firstworks.compat.OptionalIntegrations;
import com.nstut.firstworks.registry.ModBlockEntities;
import com.nstut.firstworks.registry.ModFluids;
import com.nstut.firstworks.registry.ModRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

public class BarrelBlockEntity extends BlockEntity {
    public static final int CAPACITY = 4_000;
    private final FluidTank tank;
    private ItemStack ingredient = ItemStack.EMPTY;
    private ItemStack output = ItemStack.EMPTY;
    private int progress;
    private boolean processCancelled;
    private final IItemHandler inputHandler = new BarrelItemHandler(true, false);
    private final IItemHandler outputHandler = new BarrelItemHandler(false, true);
    private final IItemHandler combinedHandler = new BarrelItemHandler(true, true);

    public BarrelBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BARREL.get(), pos, state);
        tank = new FluidTank(CAPACITY, fluid -> true) {
            @Override
            protected void onContentsChanged() {
                processCancelled = false;
                setChangedAndSync();
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

    private Process currentProcess() {
        if (ingredient.isEmpty() || level == null || tank.isEmpty() || processCancelled) return null;
        var fluidId = BuiltInRegistries.FLUID.getKey(tank.getFluid().getFluid());
        for (RecipeHolder<BarrelRecipe> holder : level.getRecipeManager().getAllRecipesFor(ModRecipes.BARREL_PROCESSING_TYPE.get())) {
            BarrelRecipe recipe = holder.value();
            if (!recipe.matches(new SingleRecipeInput(ingredient), level) || !recipe.fluid().equals(fluidId)) continue;
            if (recipe.sealed() != getBlockState().getValue(BarrelBlock.SEALED)) continue;
            int batches = Math.min(ingredient.getCount() / recipe.inputCount(), tank.getFluidAmount() / recipe.fluidAmount());
            if (batches <= 0) continue;
            if (!recipe.outputFluid().equals(BarrelRecipe.NO_FLUID)
                    && tank.getFluidAmount() != recipe.fluidAmount() * batches) continue;
            ItemStack result = recipe.result().isEmpty() ? ItemStack.EMPTY : recipe.result().copyWithCount(recipe.result().getCount() * batches);
            if (!output.isEmpty() && (!ItemStack.isSameItemSameComponents(output, result)
                    || output.getCount() + result.getCount() > output.getMaxStackSize())) continue;
            Fluid outputFluid = BuiltInRegistries.FLUID.getOptional(recipe.outputFluid()).orElse(Fluids.EMPTY);
            FluidStack outputFluidStack = outputFluid == Fluids.EMPTY || recipe.outputFluidAmount() <= 0
                    ? FluidStack.EMPTY
                    : new FluidStack(outputFluid, recipe.outputFluidAmount() * batches);
            return new Process(holder.id(), recipe, recipe.inputCount() * batches, recipe.fluidAmount() * batches,
                    ingredient.copyWithCount(recipe.inputCount() * batches), tank.getFluid().copyWithAmount(recipe.fluidAmount() * batches),
                    result, outputFluidStack, recipe.duration());
        }
        return null;
    }

    private void complete(Process process) {
        tank.drain(process.fluidAmount(), IFluidHandler.FluidAction.EXECUTE);
        ingredient.shrink(process.inputCount());
        if (ingredient.isEmpty()) ingredient = ItemStack.EMPTY;
        if (!process.outputFluidStack().isEmpty()) {
            tank.fill(process.outputFluidStack().copy(), IFluidHandler.FluidAction.EXECUTE);
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

    public boolean addWater(int amount) {
        return addFluid(new FluidStack(Fluids.WATER, amount));
    }

    public boolean addFluid(FluidStack fluid) {
        if (fluid.isEmpty() || tank.fill(fluid, IFluidHandler.FluidAction.SIMULATE) != fluid.getAmount()) return false;
        tank.fill(fluid.copy(), IFluidHandler.FluidAction.EXECUTE);
        return true;
    }

    public ItemStack drainBucket() {
        if (tank.getFluidAmount() < 1_000) return ItemStack.EMPTY;
        var bucket = tank.getFluid().getFluid().getBucket();
        if (bucket == net.minecraft.world.item.Items.AIR) return ItemStack.EMPTY;
        tank.drain(1_000, IFluidHandler.FluidAction.EXECUTE);
        return new ItemStack(bucket);
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
        if (output.isEmpty()) return false;
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

    private void setChangedAndSync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public FluidTank getTank() { return tank; }
    public IItemHandler getItemHandler(@Nullable Direction side) {
        if (side == Direction.UP) return inputHandler;
        if (side == Direction.DOWN) return outputHandler;
        return combinedHandler;
    }
    public ItemStack getIngredient() { return ingredient; }
    public ItemStack getOutput() { return output; }
    public int getProgress() { return progress; }
    public boolean isProcessCancelled() { return processCancelled; }
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
        tag.put("Tank", tank.writeToNBT(registries, new CompoundTag()));
        tag.put("Ingredient", ingredient.saveOptional(registries));
        tag.put("Output", output.saveOptional(registries));
        tag.putInt("Progress", progress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Tank")) tank.readFromNBT(registries, tag.getCompound("Tank"));
        ingredient = ItemStack.parseOptional(registries, tag.getCompound("Ingredient"));
        output = ItemStack.parseOptional(registries, tag.getCompound("Output"));
        progress = tag.getInt("Progress");
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
            return slot == 0 && allowInput ? insertIngredientStack(stack, simulate) : stack;
        }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot == 1 && allowOutput ? extractOutput(amount, simulate) : ItemStack.EMPTY;
        }
        @Override public int getSlotLimit(int slot) { return 64; }
        @Override public boolean isItemValid(int slot, ItemStack stack) {
            return slot == 0 && allowInput && canInsert(stack);
        }
    }
}
