package com.nstut.firstworks.content.brick_mold;

import com.nstut.firstworks.registry.ModBlockEntities;
import com.nstut.firstworks.registry.ModRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class BrickMoldBlockEntity extends BlockEntity {
    public static final int PRESS_ANIMATION_TICKS = 8;

    private ItemStack input = ItemStack.EMPTY;
    private ItemStack output = ItemStack.EMPTY;
    private int pressProgress;
    private int pressAnimationTicks;
    private final IItemHandler itemHandler = new MoldItemHandler();

    public BrickMoldBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BRICK_MOLD.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, BrickMoldBlockEntity mold) {
        if (mold.pressAnimationTicks > 0) mold.pressAnimationTicks--;
    }

    private Optional<RecipeHolder<BrickMoldingRecipe>> findRecipe(ItemStack stack) {
        if (stack.isEmpty() || level == null) return Optional.empty();
        return level.getRecipeManager().getAllRecipesFor(ModRecipes.BRICK_MOLDING_TYPE.get()).stream()
                .filter(holder -> holder.value().ingredient().test(stack))
                .findFirst();
    }

    public boolean canInsert(ItemStack stack) {
        if (stack.isEmpty() || !output.isEmpty()) return false;
        if (!input.isEmpty() && !ItemStack.isSameItemSameComponents(input, stack)) return false;
        return findRecipe(stack).map(holder -> input.getCount() < holder.value().inputCount()).orElse(false);
    }

    public boolean insert(ItemStack held, boolean creative) {
        if (!canInsert(held)) return false;
        if (input.isEmpty()) input = held.copyWithCount(1);
        else input.grow(1);
        if (!creative) held.shrink(1);
        setChangedAndSync();
        return true;
    }

    public boolean press() {
        if (input.isEmpty() || !output.isEmpty()) return false;
        Optional<RecipeHolder<BrickMoldingRecipe>> holder = findRecipe(input);
        if (holder.isEmpty() || input.getCount() < holder.get().value().inputCount()) return false;

        BrickMoldingRecipe recipe = holder.get().value();
        pressProgress++;
        pressAnimationTicks = PRESS_ANIMATION_TICKS;
        if (pressProgress >= Math.max(1, recipe.presses())) {
            input.shrink(recipe.inputCount());
            if (input.isEmpty()) input = ItemStack.EMPTY;
            output = recipe.result().copy();
        }
        setChangedAndSync();
        return true;
    }

    public boolean takeOutput(Player player) {
        if (output.isEmpty()) return false;
        player.getInventory().placeItemBackInInventory(output.copy());
        output = ItemStack.EMPTY;
        pressProgress = 0;
        setChangedAndSync();
        return true;
    }

    public boolean takeInput(Player player) {
        if (input.isEmpty() || !output.isEmpty()) return false;
        player.getInventory().placeItemBackInInventory(input.copy());
        input = ItemStack.EMPTY;
        pressProgress = 0;
        setChangedAndSync();
        return true;
    }

    public int getRequiredInputCount() {
        return findRecipe(input).map(holder -> holder.value().inputCount()).orElse(0);
    }

    public Optional<RecipeHolder<BrickMoldingRecipe>> getMatchingRecipe() {
        return findRecipe(input);
    }

    public int getRequiredPresses() {
        return findRecipe(input).map(holder -> Math.max(1, holder.value().presses())).orElse(1);
    }

    private void setChangedAndSync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public ItemStack getInput() { return input; }
    public ItemStack getOutput() { return output; }
    public int getPressProgress() { return pressProgress; }
    public int getPressAnimationTicks() { return pressAnimationTicks; }
    public IItemHandler getItemHandler(@Nullable Direction side) { return itemHandler; }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Input", input.saveOptional(registries));
        tag.put("Output", output.saveOptional(registries));
        tag.putInt("PressProgress", pressProgress);
        tag.putInt("PressAnimation", pressAnimationTicks);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        input = ItemStack.parseOptional(registries, tag.getCompound("Input"));
        output = ItemStack.parseOptional(registries, tag.getCompound("Output"));
        pressProgress = tag.getInt("PressProgress");
        pressAnimationTicks = tag.getInt("PressAnimation");
    }

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this, BlockEntity::getUpdateTag);
    }

    private final class MoldItemHandler implements IItemHandler {
        @Override public int getSlots() { return 2; }
        @Override public ItemStack getStackInSlot(int slot) {
            return slot == 0 ? input.copy() : slot == 1 ? output.copy() : ItemStack.EMPTY;
        }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot != 0 || !canInsert(stack)) return stack;
            int required = findRecipe(stack).map(holder -> holder.value().inputCount()).orElse(0);
            int accepted = Math.min(required - input.getCount(), stack.getCount());
            if (!simulate && accepted > 0) {
                if (input.isEmpty()) input = stack.copyWithCount(accepted);
                else input.grow(accepted);
                setChangedAndSync();
            }
            return stack.copyWithCount(stack.getCount() - accepted);
        }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot != 1 || output.isEmpty() || amount <= 0) return ItemStack.EMPTY;
            int extracted = Math.min(amount, output.getCount());
            ItemStack result = output.copyWithCount(extracted);
            if (!simulate) {
                output.shrink(extracted);
                if (output.isEmpty()) output = ItemStack.EMPTY;
                setChangedAndSync();
            }
            return result;
        }
        @Override public int getSlotLimit(int slot) {
            return slot == 0 ? Math.max(1, getRequiredInputCount()) : 64;
        }
        @Override public boolean isItemValid(int slot, ItemStack stack) { return slot == 0 && canInsert(stack); }
    }
}
