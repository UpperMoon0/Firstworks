package com.nstut.firstworks.content.loom;

import com.nstut.firstworks.compat.OptionalIntegrations;
import com.nstut.firstworks.registry.ModBlockEntities;
import com.nstut.firstworks.registry.ModBlocks;
import com.nstut.firstworks.registry.ModRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class LoomBlockEntity extends BlockEntity {
    private ItemStack input = ItemStack.EMPTY;
    private ItemStack output = ItemStack.EMPTY;
    private int progress;
    private long lastStrokeTick = Long.MIN_VALUE;
    private boolean shuttleRight;
    private boolean processCancelled;
    private final IItemHandler itemHandler = new LoomItemHandler();

    public LoomBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LOOM.get(), pos, state);
    }

    public boolean canInsert(ItemStack stack) {
        if (stack.isEmpty() || level == null) return false;
        if (!input.isEmpty() && !ItemStack.isSameItemSameComponents(input, stack)) return false;
        if (!input.isEmpty() && input.getCount() >= input.getMaxStackSize()) return false;
        return level.getRecipeManager().getAllRecipesFor(ModRecipes.LOOM_WEAVING_TYPE.get()).stream()
                .anyMatch(h -> h.value().ingredient().test(stack));
    }

    public boolean insert(ItemStack held, boolean creative) {
        if (!canInsert(held)) return false;
        if (input.isEmpty()) input = held.copyWithCount(1);
        else input.grow(1);
        if (!creative) held.shrink(1);
        progress = 0;
        processCancelled = false;
        sync();
        return true;
    }

    public boolean weave(Player player) {
        if (processCancelled) return false;
        Optional<RecipeHolder<LoomRecipe>> active = getActiveRecipe();
        if (active.isEmpty() || !canAccept(active.get().value().result())) return false;
        RecipeHolder<LoomRecipe> holder = active.get();
        LoomRecipe recipe = holder.value();
        ItemStack consumed = input.copyWithCount(recipe.inputCount());
        if (progress == 0 && level instanceof ServerLevel server
                && OptionalIntegrations.fireLoomWeavingStarting(server, this, holder.id(), recipe, consumed, recipe.result())) {
            processCancelled = true;
            sync();
            return false;
        }
        progress++;
        shuttleRight = !shuttleRight;
        lastStrokeTick = level == null ? 0 : level.getGameTime();
        player.swing(player.getUsedItemHand(), true);
        if (level != null) {
            level.playSound(null, worldPosition, SoundEvents.UI_LOOM_SELECT_PATTERN, SoundSource.BLOCKS,
                    0.8F, 0.92F + level.random.nextFloat() * 0.16F);
        }
        if (progress >= requiredStrokes(recipe)) complete(holder.id(), recipe, consumed);
        else sync();
        return true;
    }

    private int requiredStrokes(LoomRecipe recipe) {
        return getBlockState().is(ModBlocks.COPPER_LOOM.get())
                ? Math.max(1, (recipe.strokes() + 1) / 2)
                : Math.max(1, recipe.strokes());
    }

    public int getRequiredStrokes() {
        return getMatchingRecipe().map(holder -> requiredStrokes(holder.value())).orElse(1);
    }

    private boolean canAccept(ItemStack result) {
        return output.isEmpty() || ItemStack.isSameItemSameComponents(output, result)
                && output.getCount() + result.getCount() <= output.getMaxStackSize();
    }

    private void complete(net.minecraft.resources.ResourceLocation id, LoomRecipe recipe, ItemStack consumed) {
        input.shrink(recipe.inputCount());
        if (input.isEmpty()) input = ItemStack.EMPTY;
        if (output.isEmpty()) output = recipe.result().copy();
        else output.grow(recipe.result().getCount());
        progress = 0;
        sync();
        if (level instanceof ServerLevel server) {
            OptionalIntegrations.fireLoomWeavingCompleted(server, this, id, recipe, consumed, recipe.result());
        }
    }

    public boolean takeOutput(Player player) {
        if (output.isEmpty()) return false;
        player.getInventory().placeItemBackInInventory(output.copy());
        output = ItemStack.EMPTY;
        sync();
        return true;
    }

    public boolean takeInput(Player player) {
        if (input.isEmpty()) return false;
        player.getInventory().placeItemBackInInventory(input.copy());
        input = ItemStack.EMPTY;
        progress = 0;
        processCancelled = false;
        sync();
        return true;
    }

    public Optional<RecipeHolder<LoomRecipe>> getActiveRecipe() {
        if (level == null || input.isEmpty()) return Optional.empty();
        return level.getRecipeManager().getRecipeFor(ModRecipes.LOOM_WEAVING_TYPE.get(), new SingleRecipeInput(input), level);
    }

    public Optional<RecipeHolder<LoomRecipe>> getMatchingRecipe() {
        if (level == null || input.isEmpty()) return Optional.empty();
        return level.getRecipeManager().getAllRecipesFor(ModRecipes.LOOM_WEAVING_TYPE.get()).stream()
                .filter(h -> h.value().ingredient().test(input)).findFirst();
    }

    public float getShuttleOffset(float partial) {
        if (level == null) return shuttleRight ? 0.20F : -0.20F;
        float elapsed = Math.max(0.0F, level.getGameTime() + partial - lastStrokeTick);
        float t = Math.min(1.0F, elapsed / 6.0F);
        float eased = 0.5F - 0.5F * (float) Math.cos(Math.PI * t);
        float from = shuttleRight ? -0.20F : 0.20F;
        return from + (-from - from) * eased;
    }

    /** 0→1→0 pulse used by the moving beater/reed on each hand stroke. */
    public float getStrokeAnimation(float partial) {
        if (level == null || lastStrokeTick == Long.MIN_VALUE) return 0.0F;
        float elapsed = Math.max(0.0F, level.getGameTime() + partial - lastStrokeTick);
        if (elapsed >= 6.0F) return 0.0F;
        return Mth.sin((float) Math.PI * Mth.clamp(elapsed / 6.0F, 0.0F, 1.0F));
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public ItemStack getInput() { return input; }
    public ItemStack getOutput() { return output; }
    public int getProgress() { return progress; }
    public boolean isProcessCancelled() { return processCancelled; }
    public IItemHandler getItemHandler(@Nullable Direction side) { return itemHandler; }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Input", input.saveOptional(registries));
        tag.put("Output", output.saveOptional(registries));
        tag.putInt("Progress", progress);
        tag.putLong("LastStroke", lastStrokeTick);
        tag.putBoolean("ShuttleRight", shuttleRight);
        tag.putBoolean("ProcessCancelled", processCancelled);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        input = ItemStack.parseOptional(registries, tag.getCompound("Input"));
        output = ItemStack.parseOptional(registries, tag.getCompound("Output"));
        progress = tag.getInt("Progress");
        lastStrokeTick = tag.getLong("LastStroke");
        shuttleRight = tag.getBoolean("ShuttleRight");
        processCancelled = tag.getBoolean("ProcessCancelled");
    }

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this, BlockEntity::getUpdateTag); }

    private final class LoomItemHandler implements IItemHandler {
        @Override public int getSlots() { return 2; }
        @Override public ItemStack getStackInSlot(int slot) { return slot == 0 ? input.copy() : slot == 1 ? output.copy() : ItemStack.EMPTY; }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot != 0 || !canInsert(stack)) return stack;
            int room = input.isEmpty() ? stack.getMaxStackSize() : input.getMaxStackSize() - input.getCount();
            int accepted = Math.min(room, stack.getCount());
            if (!simulate && accepted > 0) {
                if (input.isEmpty()) input = stack.copyWithCount(accepted);
                else input.grow(accepted);
                progress = 0;
                processCancelled = false;
                sync();
            }
            return stack.copyWithCount(stack.getCount() - accepted);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot != 1 || output.isEmpty() || amount <= 0) return ItemStack.EMPTY;
            int extracted = Math.min(amount, output.getCount());
            ItemStack result = output.copyWithCount(extracted);
            if (!simulate) {
                output.shrink(extracted);
                if (output.isEmpty()) output = ItemStack.EMPTY;
                sync();
            }
            return result;
        }

        @Override public int getSlotLimit(int slot) { return 64; }
        @Override public boolean isItemValid(int slot, ItemStack stack) { return slot == 0 && canInsert(stack); }
    }
}
