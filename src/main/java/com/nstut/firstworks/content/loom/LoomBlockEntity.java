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
    private boolean shedB;
    private boolean processCancelled;
    private float shuttlePosition;
    private int packingTicks;
    private boolean started;
    private java.util.UUID operator;
    private long lastInputTick = Long.MIN_VALUE;
    private final IItemHandler itemHandler = new LoomItemHandler();

    public LoomBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LOOM.get(), pos, state);
    }

    public boolean canInsert(ItemStack stack) {
        if (stack.isEmpty() || level == null || started || packingTicks > 0) return false;
        if (!input.isEmpty() && !ItemStack.isSameItemSameComponents(input, stack)) return false;
        if (!input.isEmpty() && input.getCount() >= input.getMaxStackSize()) return false;
        return level.getRecipeManager().getAllRecipesFor(ModRecipes.LOOM_WEAVING_TYPE.get()).stream()
                .anyMatch(h -> h.value().ingredient().test(stack));
    }

    public boolean insert(ItemStack held, boolean creative) {
        if (!canInsert(held)) return false;
        int count = Math.min(held.getCount(), held.getMaxStackSize() - input.getCount());
        if (input.isEmpty()) input = held.copyWithCount(count);
        else input.grow(count);
        if (!creative) held.shrink(count);
        progress = 0;
        processCancelled = false;
        sync();
        return true;
    }

    public String getShed() { return shedB ? "B" : "A"; }
    public boolean isShuttleRight() { return shuttleRight; }
    public float getShuttlePosition() { return shuttlePosition; }
    public boolean isPacking() { return packingTicks > 0; }
    public float getPackingProgress(float partial) {
        return packingTicks <= 0 || level == null ? 0 : Mth.clamp((6 - packingTicks + partial) / 6.0F, 0, 1);
    }

    public net.minecraft.network.chat.Component interactionHint() {
        var matching = getMatchingRecipe();
        if (output.isEmpty() && matching.isPresent() && input.getCount() < matching.get().value().inputCount())
            return net.minecraft.network.chat.Component.translatable("jade.firstworks.loom.loading", input.getCount(), matching.get().value().inputCount());
        String key = !output.isEmpty() ? "hint.firstworks.loom.collect"
                : processCancelled ? "jade.firstworks.loom.cancelled"
                : getActiveRecipe().isEmpty() ? "jade.firstworks.loom.empty"
                : isPacking() ? "hint.firstworks.loom.packing" : "hint.firstworks.loom.controls";
        return net.minecraft.network.chat.Component.translatable(key);
    }

    public void release(Player player) {
        if (player.getUUID().equals(operator)) operator = null;
    }

    /** Server derives the track coordinate from the player's aim; packets never supply progress. */
    public boolean guide(Player player, boolean assisted) {
        if (!(level instanceof ServerLevel server) || player.level() != level || player.isSpectator()
                || !player.isAlive() || !player.mayBuild() || !level.mayInteract(player, worldPosition)
                || !player.getMainHandItem().isEmpty() || player.isShiftKeyDown()
                || !player.canInteractWithBlock(worldPosition, 0) || processCancelled) return false;
        var aim = LoomBlock.trackAim(player, worldPosition, getBlockState());
        if (aim == null) { release(player); return false; }
        long now = level.getGameTime();
        if (lastInputTick == now) return false;
        if (operator != null && now - lastInputTick > 5) operator = null;
        if (operator != null && !operator.equals(player.getUUID())) return false;
        if (operator == null && !LoomBlock.hitsShuttle(player, this)) return false;
        var active = getActiveRecipe();
        if (active.isEmpty() || !canAccept(active.get().value().result())) return false;
        operator = player.getUUID();
        lastInputTick = now;
        if (packingTicks > 0) return false;
        var holder = active.get();
        var recipe = holder.value();
        if (!started) {
            if (OptionalIntegrations.fireLoomWeavingStarting(server, this, holder.id(), recipe,
                    input.copyWithCount(recipe.inputCount()), recipe.result())) {
                processCancelled = true;
                sync();
                return false;
            }
            started = true;
            shedB = recipe.requiredShed(progress).equals("B");
        }
        float target = assisted ? (shuttleRight ? 0 : 1) : (float) Mth.clamp((aim.x - 0.3) / 0.4, 0, 1);
        // Forgiving endpoint seats suppress floating-point drift and tiny endpoint movements.
        if (target < 0.05F) target = 0;
        if (target > 0.95F) target = 1;
        // Eight held samples per crossing at minimum, in either input mode.
        shuttlePosition += Mth.clamp(target - shuttlePosition, -0.125F, 0.125F);
        if ((!shuttleRight && shuttlePosition >= 1) || (shuttleRight && shuttlePosition <= 0)) {
            packingTicks = 6;
            lastStrokeTick = now;
            level.playSound(null, worldPosition, SoundEvents.UI_LOOM_SELECT_PATTERN, SoundSource.BLOCKS, 0.8F, 1);
        }
        sync();
        return true;
    }

    public static void tick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, LoomBlockEntity loom) {
        if (level.isClientSide || loom.packingTicks <= 0) return;
        if (--loom.packingTicks == 0) {
            var active = loom.getActiveRecipe();
            if (active.isPresent()) {
                var holder = active.get();
                loom.progress++;
                loom.shuttleRight = !loom.shuttleRight;
                loom.shedB = holder.value().requiredShed(loom.progress).equals("B");
                if (loom.progress >= holder.value().passes())
                    loom.complete(holder.id(), holder.value(), loom.input.copyWithCount(holder.value().inputCount()));
            }
        }
        loom.sync();
    }

    private int requiredStrokes(LoomRecipe recipe) {
        return recipe.passes();
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
        started = false;
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
        shedB = false;
        shuttleRight = false;
        shuttlePosition = 0;
        packingTicks = 0;
        started = false;
        operator = null;
        lastStrokeTick = Long.MIN_VALUE;
        sync();
        return true;
    }

    public Optional<RecipeHolder<LoomRecipe>> getActiveRecipe() {
        if (level == null || input.isEmpty()) return Optional.empty();
        return matchingRecipes().filter(h -> h.value().matches(new SingleRecipeInput(input), level)).findFirst();
    }

    public Optional<RecipeHolder<LoomRecipe>> getMatchingRecipe() {
        if (level == null || input.isEmpty()) return Optional.empty();
        return getActiveRecipe().or(() -> matchingRecipes().min(java.util.Comparator
                .comparingInt((RecipeHolder<LoomRecipe> h) -> h.value().inputCount())
                .thenComparing(h -> h.id().toString())));
    }

    private java.util.stream.Stream<RecipeHolder<LoomRecipe>> matchingRecipes() {
        return level.getRecipeManager().getAllRecipesFor(ModRecipes.LOOM_WEAVING_TYPE.get()).stream()
                .filter(h -> h.value().ingredient().test(input))
                .sorted(java.util.Comparator.comparingInt((RecipeHolder<LoomRecipe> h) -> h.value().inputCount())
                        .reversed().thenComparing(h -> h.id().toString()));
    }

    public float getShuttleOffset(float partial) {
        return -0.20F + 0.40F * shuttlePosition;
    }

    /** 0→1→0 pulse used by the moving beater/reed on each hand stroke. */
    public float getStrokeAnimation(float partial) {
        return isPacking() ? Mth.sin((float) Math.PI * getPackingProgress(partial)) : 0;
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
        tag.putBoolean("ShedB", shedB);
        tag.putBoolean("ProcessCancelled", processCancelled);
        tag.putFloat("ShuttlePosition", shuttlePosition);
        tag.putInt("PackingTicks", packingTicks);
        tag.putBoolean("Started", started);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        input = ItemStack.parseOptional(registries, tag.getCompound("Input"));
        output = ItemStack.parseOptional(registries, tag.getCompound("Output"));
        progress = tag.getInt("Progress");
        lastStrokeTick = tag.getLong("LastStroke");
        shuttleRight = tag.getBoolean("ShuttleRight");
        shedB = tag.getBoolean("ShedB");
        processCancelled = tag.getBoolean("ProcessCancelled");
        shuttlePosition = tag.contains("ShuttlePosition") ? Mth.clamp(tag.getFloat("ShuttlePosition"), 0, 1) : shuttleRight ? 1 : 0;
        packingTicks = Mth.clamp(tag.getInt("PackingTicks"), 0, 6);
        started = tag.getBoolean("Started") || progress > 0;
        operator = null;
        lastInputTick = Long.MIN_VALUE;
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
