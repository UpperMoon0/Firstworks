package com.nstut.firstworks.content.quern;

import com.nstut.firstworks.FirstworksConfig;
import com.nstut.firstworks.registry.*;
import com.nstut.firstworks.compat.OptionalIntegrations;
import net.minecraft.core.*;
import net.minecraft.core.particles.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;
import java.util.Optional;

public final class QuernBlockEntity extends BlockEntity implements QuernDriveable {
    private ItemStack input = ItemStack.EMPTY, output = ItemStack.EMPTY;
    private int progress;
    private boolean rotating;
    private int driveRate;
    private float rotation;
    private float clientPrevRotation, clientRotation, rotationTarget;
    private boolean clientInitialized;
    private final IItemHandler inputHandler = new QuernItemHandler(true, false);
    private final IItemHandler outputHandler = new QuernItemHandler(false, true);
    private final IItemHandler combinedHandler = new QuernItemHandler(true, true);

    public QuernBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.QUERN.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, QuernBlockEntity quern) {
        if (level.isClientSide) {
            quern.clientPrevRotation = quern.clientRotation;
            if (quern.rotating) {
                quern.clientRotation = (quern.clientRotation + 9F) % 360F;
                quern.rotationTarget = quern.clientRotation;
            } else if (quern.clientRotation != quern.rotationTarget) {
                float diff = quern.rotationTarget - quern.clientRotation;
                while (diff < -180F) diff += 360F;
                while (diff > 180F) diff -= 360F;
                if (Math.abs(diff) < 1.0F) {
                    quern.clientRotation = quern.rotationTarget;
                } else {
                    float step = Math.min(Math.abs(diff), 9F) * Math.signum(diff);
                    quern.clientRotation = (quern.clientRotation + step + 360F) % 360F;
                }
            }
            return;
        }
        if (!quern.rotating || quern.driveRate <= 0) return;
        Optional<RecipeHolder<QuernGrindingRecipe>> holder = quern.recipe();
        if (holder.isEmpty() || !quern.output.isEmpty()
                || quern.input.getCount() < holder.get().value().inputCount()
                || !quern.tryBegin(holder.get())) {
            quern.stop();
            return;
        }
        quern.rotation = (quern.rotation + 9F) % 360F;
        quern.rotationTarget = quern.rotation;
        quern.progress += quern.driveRate;
        if (quern.progress % 12 == 0) {
            level.playSound(null, pos, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, .3F, .65F);
            if (level instanceof ServerLevel sl) {
                sl.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, quern.input.copyWithCount(1)),
                        pos.getX() + .5, pos.getY() + .55, pos.getZ() + .5, 2, .12, .03, .12, .01);
            }
        }
        if (quern.progress >= holder.get().value().work()) {
            quern.complete(holder.get());
        } else if (quern.progress % 5 == 0) {
            quern.sync();
        }
    }

    private boolean tryBegin(RecipeHolder<QuernGrindingRecipe> holder) {
        if (progress != 0) {
            return true;
        }
        if (level instanceof ServerLevel server && OptionalIntegrations.fireQuernGrindingStarting(
                server, this, holder.id(), holder.value(), input.copy(), holder.value().result())) {
            return false;
        }
        return true;
    }

    public boolean work(Player player) {
        Optional<RecipeHolder<QuernGrindingRecipe>> holder = recipe();
        if (holder.isEmpty() || !output.isEmpty()
                || input.getCount() < holder.get().value().inputCount()) {
            return false;
        }
        if (!tryBegin(holder.get())) {
            return false;
        }
        int workAmount = FirstworksConfig.QUERN_MANUAL_WORK_PER_CRANK.get();
        progress += workAmount;
        rotation = (rotation + 45F) % 360F;
        rotationTarget += 45F;
        level.playSound(null, worldPosition, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, .38F, .72F + level.random.nextFloat() * .1F);
        if (level instanceof ServerLevel sl) {
            sl.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, input.copyWithCount(1)),
                    worldPosition.getX() + .5, worldPosition.getY() + .55, worldPosition.getZ() + .5, 3, .14, .03, .14, .015);
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
        if (input.isEmpty()) input = ItemStack.EMPTY;
        output = recipe.result().copy();
        progress = 0;
        rotating = false;
        driveRate = 0;
        if (level instanceof ServerLevel server) {
            OptionalIntegrations.fireQuernGrindingCompleted(server, this, holder.id(), recipe, consumed, output);
        }
        level.playSound(null, worldPosition, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, .25F, 1.55F);
        sync();
    }

    public Optional<RecipeHolder<QuernGrindingRecipe>> findRecipeForIngredient(ItemStack stack) {
        if (stack.isEmpty() || level == null) return Optional.empty();
        return level.getRecipeManager().getAllRecipesFor(ModRecipes.QUERN_GRINDING_TYPE.get()).stream()
                .filter(holder -> holder.value().ingredient().test(stack))
                .findFirst();
    }

    private Optional<RecipeHolder<QuernGrindingRecipe>> recipe() {
        if (level == null || input.isEmpty()) return Optional.empty();
        return level.getRecipeManager().getRecipeFor(ModRecipes.QUERN_GRINDING_TYPE.get(), new SingleRecipeInput(input), level);
    }

    public boolean canInsert(ItemStack stack) {
        if (rotating || !output.isEmpty() || stack.isEmpty()) return false;
        Optional<RecipeHolder<QuernGrindingRecipe>> r = findRecipeForIngredient(stack);
        return r.isPresent() && (input.isEmpty() || ItemStack.isSameItemSameComponents(input, stack)) && input.getCount() < r.get().value().inputCount();
    }

    public void insert(ItemStack stack, boolean creative) {
        if (!canInsert(stack)) return;
        if (input.isEmpty()) input = stack.copyWithCount(1);
        else input.grow(1);
        if (!creative) stack.shrink(1);
        reset();
        sync();
    }

    public boolean takeOutput(Player p) {
        if (output.isEmpty()) return false;
        p.getInventory().placeItemBackInInventory(output.copy());
        output = ItemStack.EMPTY;
        sync();
        return true;
    }

    public boolean takeInput(Player p) {
        if (input.isEmpty() || rotating) return false;
        p.getInventory().placeItemBackInInventory(input.copy());
        input = ItemStack.EMPTY;
        reset();
        sync();
        return true;
    }

    private void reset() {
        progress = 0;
        rotating = false;
        driveRate = 0;
    }

    private void stop() {
        rotating = false;
        driveRate = 0;
        progress = 0;
        sync();
    }

    @Override
    public boolean canDrive() {
        var r = recipe();
        return r.isPresent() && output.isEmpty() && input.getCount() >= r.get().value().inputCount();
    }

    @Override
    public int getDriveRate() {
        return driveRate;
    }

    @Override
    public void setDriveRate(int workPerTick) {
        int clamped = Math.max(0, workPerTick);
        if (this.driveRate != clamped) {
            this.driveRate = clamped;
            this.rotating = clamped > 0;
            sync();
        }
    }

    @Override
    public void setDriven(boolean driven) {
        setDriveRate(driven ? FirstworksConfig.QUERN_DEFAULT_DRIVEN_WORK_PER_TICK.get() : 0);
    }

    public void setRotating(boolean rotating) {
        setDriven(rotating);
    }

    public ItemStack getInput() { return input; }
    public ItemStack getOutput() { return output; }
    public int getProgress() { return progress; }
    public boolean isRotating() { return rotating; }

    public float getRotation(float partial) {
        if (level != null && level.isClientSide) {
            float prev = clientPrevRotation;
            float cur = clientRotation;
            float diff = cur - prev;
            while (diff < -180F) diff += 360F;
            while (diff > 180F) diff -= 360F;
            return prev + diff * partial;
        }
        return rotation;
    }

    public int requiredWork() {
        return recipe().map(r -> r.value().work()).orElse(0);
    }

    public IItemHandler getItemHandler(@Nullable Direction side) {
        if (side == Direction.UP) return inputHandler;
        if (side == Direction.DOWN) return outputHandler;
        return combinedHandler;
    }

    private void sync() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.saveAdditional(tag, regs);
        if (!input.isEmpty()) tag.put("Input", input.save(regs));
        if (!output.isEmpty()) tag.put("Output", output.save(regs));
        tag.putInt("Progress", progress);
        tag.putBoolean("Rotating", rotating);
        tag.putInt("DriveRate", driveRate);
        tag.putFloat("Rotation", rotation);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.loadAdditional(tag, regs);
        input = ItemStack.parseOptional(regs, tag.getCompound("Input"));
        output = ItemStack.parseOptional(regs, tag.getCompound("Output"));
        progress = tag.getInt("Progress");
        rotating = tag.getBoolean("Rotating");
        driveRate = tag.getInt("DriveRate");
        rotation = tag.getFloat("Rotation");
        if (level != null && level.isClientSide) {
            rotationTarget = rotation;
            if (!clientInitialized) {
                clientRotation = rotation;
                clientPrevRotation = rotation;
                clientInitialized = true;
            }
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider regs) {
        CompoundTag t = new CompoundTag();
        saveAdditional(t, regs);
        return t;
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
            if (slot != 0 || !allowInput || !canInsert(stack)) return stack;
            int required = findRecipeForIngredient(stack).map(holder -> holder.value().inputCount()).orElse(0);
            int accepted = Math.min(required - input.getCount(), stack.getCount());
            if (accepted <= 0) return stack;
            if (!simulate) {
                if (input.isEmpty()) input = stack.copyWithCount(accepted);
                else input.grow(accepted);
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
            return slot == 0 ? findRecipeForIngredient(input).map(holder -> holder.value().inputCount()).orElse(64) : 64;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == 0 && allowInput && canInsert(stack);
        }
    }
}
