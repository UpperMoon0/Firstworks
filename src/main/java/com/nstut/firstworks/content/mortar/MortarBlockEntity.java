package com.nstut.firstworks.content.mortar;

import com.nstut.firstworks.content.MortarGrindingRecipe;
import com.nstut.firstworks.registry.ModBlockEntities;
import com.nstut.firstworks.registry.ModRecipes;
import com.nstut.firstworks.compat.OptionalIntegrations;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public final class MortarBlockEntity extends BlockEntity {
    private ItemStack input = ItemStack.EMPTY;
    private ItemStack output = ItemStack.EMPTY;
    private boolean grinding;
    private long finishGameTime;
    private final IItemHandler itemHandler = new MortarItemHandler();

    public MortarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MORTAR.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MortarBlockEntity mortar) {
        if (level.isClientSide || !mortar.grinding) return;
        Optional<RecipeHolder<MortarGrindingRecipe>> active = mortar.findRecipe(mortar.input);
        if (active.isEmpty() || mortar.output.isEmpty() == false
                || mortar.input.getCount() < active.get().value().inputCount()) {
            mortar.stopGrinding();
            return;
        }
        if (level.getGameTime() % 10L == 0L) {
            level.playSound(null, pos, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.28F, 0.78F);
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, mortar.input.copyWithCount(1)),
                        pos.getX() + 0.5, pos.getY() + 0.38, pos.getZ() + 0.5,
                        1, 0.08, 0.02, 0.08, 0.006);
            }
        }
        if (level.getGameTime() < mortar.finishGameTime) return;

        MortarGrindingRecipe recipe = active.get().value();
        mortar.input.shrink(recipe.inputCount());
        if (mortar.input.isEmpty()) mortar.input = ItemStack.EMPTY;
        mortar.output = recipe.result().copy();
        if (level instanceof ServerLevel serverLevel) {
            OptionalIntegrations.fireMortarGrindingCompleted(serverLevel, mortar, active.get().id(), recipe,
                    mortar.input.copy(), mortar.output.copy());
        }
        mortar.grinding = false;
        mortar.finishGameTime = 0L;
        mortar.setChangedAndSync();
    }

    private Optional<RecipeHolder<MortarGrindingRecipe>> findRecipe(ItemStack stack) {
        if (stack.isEmpty() || level == null) return Optional.empty();
        return level.getRecipeManager().getRecipeFor(ModRecipes.MORTAR_GRINDING_TYPE.get(),
                new SingleRecipeInput(stack), level);
    }

    public boolean canInsert(ItemStack stack) {
        if (grinding || stack.isEmpty() || !output.isEmpty()) return false;
        Optional<RecipeHolder<MortarGrindingRecipe>> recipe = findRecipe(stack);
        if (recipe.isEmpty()) return false;
        if (!input.isEmpty() && !ItemStack.isSameItemSameComponents(input, stack)) return false;
        return input.getCount() < recipe.get().value().inputCount();
    }

    public boolean insert(ItemStack held, boolean creative) {
        if (!canInsert(held)) return false;
        if (input.isEmpty()) input = held.copyWithCount(1);
        else input.grow(1);
        if (!creative) held.shrink(1);
        setChangedAndSync();
        return true;
    }

    public boolean startGrinding() {
        if (grinding || !output.isEmpty() || level == null) return false;
        Optional<RecipeHolder<MortarGrindingRecipe>> active = findRecipe(input);
        if (active.isEmpty() || input.getCount() < active.get().value().inputCount()) return false;
        if (level instanceof ServerLevel serverLevel && !OptionalIntegrations.fireMortarGrindingStarting(
                serverLevel, this, active.get().id(), active.get().value(), input.copy(), active.get().value().result())) return false;
        grinding = true;
        finishGameTime = level.getGameTime() + active.get().value().duration();
        setChangedAndSync();
        return true;
    }

    private void stopGrinding() {
        grinding = false;
        finishGameTime = 0L;
        setChangedAndSync();
    }

    public boolean takeOutput(Player player) {
        if (output.isEmpty()) return false;
        player.getInventory().placeItemBackInInventory(output.copy());
        output = ItemStack.EMPTY;
        setChangedAndSync();
        return true;
    }

    public boolean takeInput(Player player) {
        if (input.isEmpty() || grinding) return false;
        player.getInventory().placeItemBackInInventory(input.copy());
        input = ItemStack.EMPTY;
        setChangedAndSync();
        return true;
    }

    public float getGrindingProgress(float partialTick) {
        if (!grinding || level == null) return 0.0F;
        return level.getGameTime() + partialTick;
    }

    public ItemStack getInput() { return input; }
    public ItemStack getOutput() { return output; }
    public boolean isGrinding() { return grinding; }
    public IItemHandler getItemHandler(@Nullable Direction side) { return itemHandler; }

    private void setChangedAndSync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Input", input.saveOptional(registries));
        tag.put("Output", output.saveOptional(registries));
        tag.putBoolean("Grinding", grinding);
        tag.putLong("FinishGameTime", finishGameTime);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        input = ItemStack.parseOptional(registries, tag.getCompound("Input"));
        output = ItemStack.parseOptional(registries, tag.getCompound("Output"));
        grinding = tag.getBoolean("Grinding");
        finishGameTime = tag.getLong("FinishGameTime");
    }

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this, BlockEntity::getUpdateTag);
    }

    private final class MortarItemHandler implements IItemHandler {
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
            return slot == 0 ? findRecipe(input).map(holder -> holder.value().inputCount()).orElse(64) : 64;
        }
        @Override public boolean isItemValid(int slot, ItemStack stack) { return slot == 0 && canInsert(stack); }
    }
}
