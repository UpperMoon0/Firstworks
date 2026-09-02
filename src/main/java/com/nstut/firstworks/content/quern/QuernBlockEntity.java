package com.nstut.firstworks.content.quern;

import com.nstut.firstworks.FirstworksConfig;
import com.nstut.firstworks.compat.OptionalIntegrations;
import com.nstut.firstworks.registry.ModBlockEntities;
import com.nstut.firstworks.registry.ModBlocks;
import com.nstut.firstworks.registry.ModRecipes;
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
import java.util.Optional;

public final class QuernBlockEntity extends BlockEntity {
    private ItemStack input = ItemStack.EMPTY;
    private ItemStack output = ItemStack.EMPTY;
    private int progress;
    private long rotationSteps;
    private int lastVisualWork = 1;
    private double clientPrevRotation;
    private double clientRotation;
    private double rotationTarget;
    private boolean clientInitialized;

    private final IItemHandler inputHandler = new QuernItemHandler(true, false);
    private final IItemHandler outputHandler = new QuernItemHandler(false, true);
    private final IItemHandler combinedHandler = new QuernItemHandler(true, true);

    public QuernBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.QUERN.get(), pos, state);
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, QuernBlockEntity quern) {
        quern.clientPrevRotation = quern.clientRotation;
        if (quern.clientRotation < quern.rotationTarget) {
            double diff = quern.rotationTarget - quern.clientRotation;
            if (diff < 1.0D) {
                quern.clientRotation = quern.rotationTarget;
            } else {
                // Distance remains proportional to work, while catch-up speed expands with both the
                // applied work and current backlog. Faster cranks therefore look faster instead of
                // merely leaving the renderer several seconds behind the server state.
                double minimumStep = Mth.clamp(quern.lastVisualWork * 9.0D, 22.5D, 180.0D);
                double adaptiveStep = Math.max(minimumStep, diff * 0.65D);
                quern.clientRotation += Math.min(diff, adaptiveStep);
            }
        } else if (quern.clientRotation > quern.rotationTarget) {
            quern.clientRotation = quern.rotationTarget;
            quern.clientPrevRotation = quern.rotationTarget;
        }
    }

    private boolean tryBegin(RecipeHolder<QuernGrindingRecipe> holder) {
        if (progress != 0) {
            return true;
        }
        return !(level instanceof ServerLevel server)
                || !OptionalIntegrations.fireQuernGrindingStarting(
                server, this, holder.id(), holder.value(), input.copy(), holder.value().result());
    }

    public boolean work() {
        Optional<RecipeHolder<QuernGrindingRecipe>> holder = recipe();
        if (holder.isEmpty() || !output.isEmpty()
                || input.getCount() < holder.get().value().inputCount()) {
            return false;
        }
        if (!tryBegin(holder.get())) {
            return false;
        }

        boolean rotary = getBlockState().is(ModBlocks.ROTARY_QUERN.get());
        int workAmount = FirstworksConfig.QUERN_MANUAL_WORK_PER_CRANK.get() * (rotary ? 4 : 1);
        progress += workAmount;
        lastVisualWork = Math.max(1, workAmount);
        rotationSteps += workAmount;

        level.playSound(null, worldPosition, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS,
                0.38F, 0.72F + level.random.nextFloat() * 0.1F);
        if (level instanceof ServerLevel server) {
            server.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, input.copyWithCount(1)),
                    worldPosition.getX() + 0.5, worldPosition.getY() + 0.55, worldPosition.getZ() + 0.5,
                    3, 0.14, 0.03, 0.14, 0.015);
        }

        if (progress >= holder.get().value().work()) {
            complete(holder.get());
        } else {
            sync();
        }
        return true;
    }

    private void complete(RecipeHolder<QuernGrindingRecipe> holder) {
        QuernGrindingRecipe recipe = holder.value();
        ItemStack consumed = input.copyWithCount(recipe.inputCount());
        input.shrink(recipe.inputCount());
        if (input.isEmpty()) {
            input = ItemStack.EMPTY;
        }
        output = recipe.result().copy();
        progress = 0;
        if (level instanceof ServerLevel server) {
            OptionalIntegrations.fireQuernGrindingCompleted(server, this, holder.id(), recipe, consumed, output);
        }
        level.playSound(null, worldPosition, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.25F, 1.55F);
        sync();
    }

    public Optional<RecipeHolder<QuernGrindingRecipe>> findRecipeForIngredient(ItemStack stack) {
        return bestRecipeFor(stack);
    }

    private Optional<RecipeHolder<QuernGrindingRecipe>> recipe() {
        return bestRecipeFor(input);
    }

    private Optional<RecipeHolder<QuernGrindingRecipe>> bestRecipeFor(ItemStack stack) {
        if (stack.isEmpty() || level == null) {
            return Optional.empty();
        }
        return level.getRecipeManager().getAllRecipesFor(ModRecipes.QUERN_GRINDING_TYPE.get()).stream()
                .filter(holder -> holder.value().ingredient().test(stack))
                .max(Comparator.comparingInt((RecipeHolder<QuernGrindingRecipe> holder) -> holder.value().priority())
                        .thenComparing(holder -> holder.id().toString()));
    }

    public boolean canInsert(ItemStack stack) {
        if (!output.isEmpty() || stack.isEmpty()) {
            return false;
        }
        Optional<RecipeHolder<QuernGrindingRecipe>> recipe = findRecipeForIngredient(stack);
        return recipe.isPresent()
                && (input.isEmpty() || ItemStack.isSameItemSameComponents(input, stack))
                && input.getCount() < recipe.get().value().inputCount();
    }

    public void insert(ItemStack stack, boolean creative) {
        if (!canInsert(stack)) {
            return;
        }
        if (input.isEmpty()) {
            input = stack.copyWithCount(1);
        } else {
            input.grow(1);
        }
        if (!creative) {
            stack.shrink(1);
        }
        reset();
        sync();
    }

    public boolean takeOutput(Player player) {
        if (output.isEmpty()) {
            return false;
        }
        player.getInventory().placeItemBackInInventory(output.copy());
        output = ItemStack.EMPTY;
        sync();
        return true;
    }

    public boolean takeInput(Player player) {
        if (input.isEmpty()) {
            return false;
        }
        player.getInventory().placeItemBackInInventory(input.copy());
        input = ItemStack.EMPTY;
        reset();
        sync();
        return true;
    }

    private void reset() {
        progress = 0;
    }

    public ItemStack getInput() {
        return input;
    }

    public ItemStack getOutput() {
        return output;
    }

    public int getProgress() {
        return progress;
    }

    public float getRotation(float partialTick) {
        if (level != null && level.isClientSide) {
            double interpolated = clientPrevRotation + (clientRotation - clientPrevRotation) * partialTick;
            return (float) (interpolated % 360.0D);
        }
        return Math.floorMod(rotationSteps, 8L) * 45.0F;
    }

    public int requiredWork() {
        return recipe().map(holder -> holder.value().work()).orElse(0);
    }

    public IItemHandler getItemHandler(@Nullable Direction side) {
        if (side == Direction.UP) {
            return inputHandler;
        }
        if (side == Direction.DOWN) {
            return outputHandler;
        }
        return combinedHandler;
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
        if (!input.isEmpty()) {
            tag.put("Input", input.save(regs));
        }
        if (!output.isEmpty()) {
            tag.put("Output", output.save(regs));
        }
        tag.putInt("Progress", progress);
        tag.putLong("RotationSteps", rotationSteps);
        tag.putInt("VisualWork", lastVisualWork);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.loadAdditional(tag, regs);
        input = ItemStack.parseOptional(regs, tag.getCompound("Input"));
        output = ItemStack.parseOptional(regs, tag.getCompound("Output"));
        progress = tag.getInt("Progress");
        rotationSteps = tag.contains("RotationSteps")
                ? tag.getLong("RotationSteps")
                : Math.round(tag.getFloat("Rotation") / 45.0F);
        lastVisualWork = Math.max(1, tag.contains("VisualWork") ? tag.getInt("VisualWork") : 1);

        if (level != null && level.isClientSide) {
            rotationTarget = rotationSteps * 45.0D;
            if (!clientInitialized) {
                clientRotation = rotationTarget;
                clientPrevRotation = rotationTarget;
                clientInitialized = true;
            }
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider regs) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, regs);
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private final class QuernItemHandler implements IItemHandler {
        private final boolean allowInput;
        private final boolean allowOutput;

        private QuernItemHandler(boolean allowInput, boolean allowOutput) {
            this.allowInput = allowInput;
            this.allowOutput = allowOutput;
        }

        @Override
        public int getSlots() {
            return 2;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return slot == 0 ? input.copy() : slot == 1 ? output.copy() : ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot != 0 || !allowInput || !canInsert(stack)) {
                return stack;
            }
            int required = findRecipeForIngredient(stack)
                    .map(holder -> holder.value().inputCount())
                    .orElse(0);
            int accepted = Math.min(required - input.getCount(), stack.getCount());
            if (accepted <= 0) {
                return stack;
            }
            if (!simulate) {
                if (input.isEmpty()) {
                    input = stack.copyWithCount(accepted);
                } else {
                    input.grow(accepted);
                }
                reset();
                sync();
            }
            return stack.copyWithCount(stack.getCount() - accepted);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot != 1 || !allowOutput || output.isEmpty() || amount <= 0) {
                return ItemStack.EMPTY;
            }
            int extracted = Math.min(amount, output.getCount());
            ItemStack result = output.copyWithCount(extracted);
            if (!simulate) {
                output.shrink(extracted);
                if (output.isEmpty()) {
                    output = ItemStack.EMPTY;
                }
                sync();
            }
            return result;
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot == 0
                    ? findRecipeForIngredient(input).map(holder -> holder.value().inputCount()).orElse(64)
                    : 64;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == 0 && allowInput && canInsert(stack);
        }
    }
}
