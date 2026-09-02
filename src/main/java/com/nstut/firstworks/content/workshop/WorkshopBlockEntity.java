package com.nstut.firstworks.content.workshop;

import com.nstut.firstworks.registry.ModBlockEntities;
import com.nstut.firstworks.registry.ModRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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

public final class WorkshopBlockEntity extends BlockEntity {
    private ItemStack input = ItemStack.EMPTY;
    private ItemStack catalyst = ItemStack.EMPTY;
    private ItemStack fuel = ItemStack.EMPTY;
    private ItemStack output = ItemStack.EMPTY;
    private int progress;
    private int stokeTicks;
    private boolean running;
    private long actionSteps;

    private double clientPrevRotation;
    private double clientRotation;
    private double rotationTarget;
    private boolean clientRotationInitialized;
    private long clientObservedActionSteps = Long.MIN_VALUE;
    private long clientActionTick = Long.MIN_VALUE;

    private final IItemHandler handler = new WorkshopItemHandler();

    public WorkshopBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.WORKSHOP.get(), pos, state); }

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
        if (workshop.stokeTicks > 0) workshop.stokeTicks--;
        String station = workshop.station();
        if (!station.equals(WorkshopRecipe.KILN) && !station.equals(WorkshopRecipe.CRUCIBLE_FURNACE)) return;

        Optional<RecipeHolder<WorkshopRecipe>> holder = workshop.activeRecipe();
        if (holder.isEmpty() || !workshop.output.isEmpty()) {
            if (workshop.progress != 0 || workshop.running) {
                workshop.progress = 0;
                workshop.running = false;
                workshop.sync();
            }
            return;
        }
        if (station.equals(WorkshopRecipe.CRUCIBLE_FURNACE) && workshop.stokeTicks <= 0) return;

        if (!workshop.running) {
            if (workshop.fuel.isEmpty()) return;
            workshop.fuel.shrink(1);
            if (workshop.fuel.isEmpty()) workshop.fuel = ItemStack.EMPTY;
            workshop.running = true;
            level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.55F, 0.85F);
        }

        workshop.progress++;
        if (workshop.progress >= holder.get().value().work()) workshop.complete(holder.get().value());
        else if (workshop.progress % 20 == 0) workshop.sync();
    }

    public boolean stoke(int ticks) {
        if (!station().equals(WorkshopRecipe.CRUCIBLE_FURNACE)) return false;
        stokeTicks = Math.max(stokeTicks, Math.max(1, ticks));
        sync();
        return true;
    }

    public boolean work(Player player) {
        String station = station();
        if (!station.equals(WorkshopRecipe.POTTERY_WHEEL) && !station.equals(WorkshopRecipe.STONE_ANVIL)) return false;
        Optional<RecipeHolder<WorkshopRecipe>> holder = activeRecipe();
        if (holder.isEmpty() || !output.isEmpty()) return false;

        progress++;
        actionSteps++;
        if (level != null) level.playSound(null, worldPosition,
                station.equals(WorkshopRecipe.POTTERY_WHEEL) ? SoundEvents.BRUSH_GENERIC : SoundEvents.ANVIL_HIT,
                SoundSource.BLOCKS, 0.45F, station.equals(WorkshopRecipe.POTTERY_WHEEL) ? 1.15F : 1.35F);
        if (progress >= holder.get().value().work()) complete(holder.get().value());
        else sync();
        return true;
    }

    private void complete(WorkshopRecipe recipe) {
        input.shrink(recipe.inputCount());
        if (input.isEmpty()) input = ItemStack.EMPTY;
        if (recipe.consumeCatalyst() && recipe.hasCatalyst()) {
            catalyst.shrink(recipe.catalystCount());
            if (catalyst.isEmpty()) catalyst = ItemStack.EMPTY;
        }
        output = recipe.result().copy();
        progress = 0;
        running = false;
        if (level != null) level.playSound(null, worldPosition, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.22F, 1.65F);
        sync();
    }

    public Optional<RecipeHolder<WorkshopRecipe>> activeRecipe() {
        if (level == null || input.isEmpty()) return Optional.empty();
        return stationRecipes().stream()
                .filter(holder -> holder.value().ingredient().test(input) && input.getCount() >= holder.value().inputCount())
                .filter(holder -> holder.value().catalystMatches(catalyst))
                .sorted(Comparator.comparingInt((RecipeHolder<WorkshopRecipe> holder) -> holder.value().inputCount()).reversed()
                        .thenComparing(holder -> holder.id().toString()))
                .findFirst();
    }

    private List<RecipeHolder<WorkshopRecipe>> stationRecipes() {
        if (level == null) return List.of();
        return level.getRecipeManager().getAllRecipesFor(ModRecipes.WORKSHOP_PROCESSING_TYPE.get()).stream()
                .filter(holder -> holder.value().station().equals(station())).toList();
    }

    private boolean isFuel(ItemStack stack) { return stack.is(Items.CHARCOAL) || stack.is(Items.COAL); }
    private boolean heated() { return station().equals(WorkshopRecipe.KILN) || station().equals(WorkshopRecipe.CRUCIBLE_FURNACE); }
    private boolean validInput(ItemStack stack) { return stationRecipes().stream().anyMatch(h -> h.value().ingredient().test(stack)); }
    private boolean validCatalyst(ItemStack stack) { return stationRecipes().stream().anyMatch(h -> h.value().hasCatalyst() && h.value().catalyst().test(stack)); }

    public boolean canInsert(ItemStack stack) {
        if (stack.isEmpty() || !output.isEmpty()) return false;
        if (heated() && isFuel(stack)) return fuel.isEmpty() || ItemStack.isSameItemSameComponents(fuel, stack) && fuel.getCount() < fuel.getMaxStackSize();
        if (validInput(stack)) return input.isEmpty() || ItemStack.isSameItemSameComponents(input, stack) && input.getCount() < input.getMaxStackSize();
        return validCatalyst(stack) && (catalyst.isEmpty() || ItemStack.isSameItemSameComponents(catalyst, stack) && catalyst.getCount() < catalyst.getMaxStackSize());
    }

    public boolean insert(ItemStack held, boolean creative) {
        if (!canInsert(held)) return false;
        if (heated() && isFuel(held)) fuel = addOne(fuel, held);
        else if (validInput(held)) input = addOne(input, held);
        else catalyst = addOne(catalyst, held);
        if (!creative) held.shrink(1);
        progress = 0;
        running = false;
        sync();
        return true;
    }

    private static ItemStack addOne(ItemStack target, ItemStack source) {
        if (target.isEmpty()) return source.copyWithCount(1);
        target.grow(1);
        return target;
    }

    public boolean takeOutput(Player player) {
        if (output.isEmpty()) return false;
        player.getInventory().placeItemBackInInventory(output.copy());
        output = ItemStack.EMPTY;
        sync();
        return true;
    }

    public boolean takeStored(Player player) {
        ItemStack stack;
        if (!catalyst.isEmpty()) { stack = catalyst; catalyst = ItemStack.EMPTY; }
        else if (!input.isEmpty()) { stack = input; input = ItemStack.EMPTY; }
        else if (!fuel.isEmpty()) { stack = fuel; fuel = ItemStack.EMPTY; }
        else return false;
        player.getInventory().placeItemBackInInventory(stack.copy());
        progress = 0;
        running = false;
        sync();
        return true;
    }

    public List<ItemStack> allStacks() { return List.of(input, catalyst, fuel, output); }
    public IItemHandler getItemHandler(@Nullable Direction side) { return handler; }
    public int getProgress() { return progress; }
    public int getStokeTicks() { return stokeTicks; }
    public ItemStack getInput() { return input; }
    public ItemStack getCatalyst() { return catalyst; }
    public ItemStack getFuel() { return fuel; }
    public ItemStack getOutput() { return output; }
    public boolean isRunning() { return running; }

    public float getProgressFraction() {
        if (!output.isEmpty()) return 1.0F;
        return activeRecipe().map(holder -> Mth.clamp((float) progress / Math.max(1, holder.value().work()), 0.0F, 1.0F)).orElse(0.0F);
    }

    public float getWheelRotation(float partialTick) {
        if (level != null && level.isClientSide) {
            double interpolated = clientPrevRotation + (clientRotation - clientPrevRotation) * partialTick;
            return (float) (interpolated % 360.0D);
        }
        return (float) ((actionSteps * 72L) % 360L);
    }

    public float getActionPulse(float partialTick) {
        if (level == null || clientActionTick == Long.MIN_VALUE) return 0.0F;
        float elapsed = (float) (level.getGameTime() + partialTick - clientActionTick);
        if (elapsed < 0.0F || elapsed >= 6.0F) return 0.0F;
        return Mth.sin((float) Math.PI * Mth.clamp(elapsed / 6.0F, 0.0F, 1.0F));
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.saveAdditional(tag, regs);
        tag.put("Input", input.saveOptional(regs));
        tag.put("Catalyst", catalyst.saveOptional(regs));
        tag.put("Fuel", fuel.saveOptional(regs));
        tag.put("Output", output.saveOptional(regs));
        tag.putInt("Progress", progress);
        tag.putInt("StokeTicks", stokeTicks);
        tag.putBoolean("Running", running);
        tag.putLong("ActionSteps", actionSteps);
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.loadAdditional(tag, regs);
        input = ItemStack.parseOptional(regs, tag.getCompound("Input"));
        catalyst = ItemStack.parseOptional(regs, tag.getCompound("Catalyst"));
        fuel = ItemStack.parseOptional(regs, tag.getCompound("Fuel"));
        output = ItemStack.parseOptional(regs, tag.getCompound("Output"));
        progress = tag.getInt("Progress");
        stokeTicks = tag.getInt("StokeTicks");
        running = tag.getBoolean("Running");
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

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider regs) { return saveWithoutMetadata(regs); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this, BlockEntity::getUpdateTag); }

    private final class WorkshopItemHandler implements IItemHandler {
        @Override public int getSlots() { return 4; }
        @Override public ItemStack getStackInSlot(int slot) {
            return switch (slot) { case 0 -> input.copy(); case 1 -> catalyst.copy(); case 2 -> fuel.copy(); case 3 -> output.copy(); default -> ItemStack.EMPTY; };
        }

        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (stack.isEmpty() || !isItemValid(slot, stack)) return stack;
            ItemStack current = getStackInSlot(slot);
            if (!current.isEmpty() && !ItemStack.isSameItemSameComponents(current, stack)) return stack;
            int accepted = Math.min(stack.getMaxStackSize() - current.getCount(), stack.getCount());
            if (accepted <= 0) return stack;
            if (!simulate) {
                ItemStack target = current.isEmpty() ? stack.copyWithCount(accepted) : current.copyWithCount(current.getCount() + accepted);
                if (slot == 0) input = target;
                else if (slot == 1) catalyst = target;
                else fuel = target;
                progress = 0;
                running = false;
                sync();
            }
            return stack.copyWithCount(stack.getCount() - accepted);
        }

        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot != 3 || output.isEmpty() || amount <= 0) return ItemStack.EMPTY;
            int count = Math.min(amount, output.getCount());
            ItemStack result = output.copyWithCount(count);
            if (!simulate) {
                output.shrink(count);
                if (output.isEmpty()) output = ItemStack.EMPTY;
                sync();
            }
            return result;
        }

        @Override public int getSlotLimit(int slot) { return 64; }
        @Override public boolean isItemValid(int slot, ItemStack stack) {
            return switch (slot) {
                case 0 -> validInput(stack);
                case 1 -> validCatalyst(stack);
                case 2 -> heated() && isFuel(stack);
                default -> false;
            };
        }
    }
}
