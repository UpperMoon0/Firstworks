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
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
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
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;
import java.util.Optional;
import java.util.UUID;

public final class MortarBlockEntity extends BlockEntity {
    private ItemStack input = ItemStack.EMPTY;
    private ItemStack output = ItemStack.EMPTY;
    private boolean grinding;
    private int stageIndex;
    private int stageProgress;
    private boolean started;
    private boolean cancelled;
    private ResourceLocation recipeId;
    private long lastWorkTick = Long.MIN_VALUE;
    private long lastCrushTick = Long.MIN_VALUE;
    private boolean legacyProgressPending;
    private long legacyFinishGameTime = Long.MIN_VALUE;
    private UUID operator;
    private final IItemHandler itemHandler = new MortarItemHandler();

    public MortarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MORTAR.get(), pos, state);
    }

    /** Ticking can only stop work. Progress is earned by validated manual input. */
    public static void tick(Level level, BlockPos pos, BlockState state, MortarBlockEntity mortar) {
        if (!level.isClientSide) mortar.migrateLegacyProgress();
        if (level.isClientSide || !mortar.grinding) return;
        Player player = mortar.operator == null ? null : level.getPlayerByUUID(mortar.operator);
        if (level.getGameTime() - mortar.lastWorkTick > 1 || player == null || !mortar.canOperate(player, "grind")) {
            mortar.grinding = false;
            mortar.operator = null;
            mortar.setChangedAndSync();
        }
    }

    private void migrateLegacyProgress() {
        if (!legacyProgressPending || !(level instanceof ServerLevel)) return;
        legacyProgressPending = false;
        grinding = false;
        operator = null;
        cancelled = false;

        var active = findRecipe(input);
        if (active.isEmpty() || output.isEmpty() == false) {
            legacyFinishGameTime = Long.MIN_VALUE;
            setChangedAndSync();
            return;
        }

        var holder = active.get();
        var stages = holder.value().stages();
        int legacyDuration = Math.max(1, holder.value().duration());
        long remaining = legacyFinishGameTime == Long.MIN_VALUE
                ? legacyDuration
                : Math.max(0L, Math.min((long) legacyDuration, legacyFinishGameTime - level.getGameTime()));
        long completed = Math.max(0L, legacyDuration - remaining);
        int totalWork = stages.stream().mapToInt(MortarStage::work).sum();
        int migratedWork = totalWork <= 1 ? 0
                : (int) Math.min(totalWork - 1L, completed * totalWork / legacyDuration);

        stageIndex = 0;
        while (stageIndex < stages.size() - 1 && migratedWork >= stages.get(stageIndex).work()) {
            migratedWork -= stages.get(stageIndex).work();
            stageIndex++;
        }
        stageProgress = Math.max(0, migratedWork);
        started = true;
        recipeId = holder.id();
        lastWorkTick = Long.MIN_VALUE;
        lastCrushTick = Long.MIN_VALUE;
        legacyFinishGameTime = Long.MIN_VALUE;
        setChangedAndSync();
    }

    public boolean canOperate(Player player, String action) {
        if (player.isSpectator() || !player.isAlive() || player.isShiftKeyDown()
                || !player.getMainHandItem().isEmpty() || player.level() != level
                || !player.mayBuild() || !level.mayInteract(player, worldPosition)) return false;
        return player.pick(player.blockInteractionRange(), 1.0F, false) instanceof BlockHitResult hit
                && hit.getBlockPos().equals(worldPosition) && MortarBlock.actionAt(worldPosition, hit).equals(action);
    }

    public boolean operate(Player player, String action) {
        if (!(level instanceof ServerLevel server) || !canOperate(player, action)
                || !output.isEmpty() || lastWorkTick == level.getGameTime()) return false;
        var active = findRecipe(input);
        if (active.isEmpty()) return false;
        var holder = active.get();
        if (!holder.id().equals(recipeId)) {
            resetProcessing();
            recipeId = holder.id();
        }
        var stages = holder.value().stages();
        if (stageIndex >= stages.size()) resetProcessing();
        MortarStage stage = stages.get(stageIndex);
        if (!stage.action().equals(action) || cancelled) return false;
        if (action.equals("crush") && lastCrushTick != Long.MIN_VALUE && level.getGameTime() - lastCrushTick < 4) return false;
        if (!started) {
            if (OptionalIntegrations.fireMortarGrindingStarting(server, this, holder.id(), holder.value(), input.copy(), holder.value().result())) {
                cancelled = true;
                setChangedAndSync();
                return false;
            }
            started = true;
        }
        lastWorkTick = level.getGameTime();
        boolean wasGrinding = grinding;
        grinding = action.equals("grind");
        operator = grinding ? player.getUUID() : null;
        if (!grinding) lastCrushTick = lastWorkTick;
        stageProgress++;
        if (!grinding || lastWorkTick % 10 == 0) {
            level.playSound(null, worldPosition, grinding ? SoundEvents.GRINDSTONE_USE : SoundEvents.STONE_HIT,
                    SoundSource.BLOCKS, 0.35F, grinding ? 0.78F : 1.05F);
            server.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, input.copyWithCount(1)),
                    worldPosition.getX() + 0.5, worldPosition.getY() + 0.25, worldPosition.getZ() + 0.5,
                    grinding ? 1 : 4, 0.08, 0.02, 0.08, 0.006);
        }
        if (stageProgress >= stage.work()) {
            stageIndex++;
            stageProgress = 0;
            grinding = false;
            operator = null;
            if (stageIndex >= stages.size()) {
                ItemStack consumed = input.copyWithCount(holder.value().inputCount());
                input.shrink(holder.value().inputCount());
                if (input.isEmpty()) input = ItemStack.EMPTY;
                output = holder.value().result().copy();
                resetProcessing();
                OptionalIntegrations.fireMortarGrindingCompleted(server, this, holder.id(), holder.value(), consumed, output.copy());
            }
            setChangedAndSync();
        } else if (!grinding || !wasGrinding || stageProgress % 5 == 0) setChangedAndSync();
        else setChanged();
        return true;
    }

    public void stopGrinding(Player player) {
        if (player.getUUID().equals(operator)) {
            grinding = false;
            operator = null;
            setChangedAndSync();
        }
    }

    /** Kept for script compatibility; an unowned start must never run a hand tool autonomously. */
    @Deprecated public boolean startGrinding() { return false; }

    private void resetProcessing() {
        stageIndex = 0;
        stageProgress = 0;
        started = false;
        cancelled = false;
        grinding = false;
        operator = null;
        recipeId = null;
    }

    private Optional<RecipeHolder<MortarGrindingRecipe>> findRecipe(ItemStack stack) {
        if (stack.isEmpty() || level == null) return Optional.empty();
        return matchingRecipes(stack).filter(h -> h.value().matches(new SingleRecipeInput(stack), level)).findFirst();
    }

    public Optional<RecipeHolder<MortarGrindingRecipe>> findRecipeForIngredient(ItemStack stack) {
        if (stack.isEmpty() || level == null) return Optional.empty();
        return findRecipe(stack).or(() -> matchingRecipes(stack).min(java.util.Comparator
                .comparingInt((RecipeHolder<MortarGrindingRecipe> h) -> h.value().inputCount())
                .thenComparing(h -> h.id().toString())));
    }

    private java.util.stream.Stream<RecipeHolder<MortarGrindingRecipe>> matchingRecipes(ItemStack stack) {
        return level.getRecipeManager().getAllRecipesFor(ModRecipes.MORTAR_GRINDING_TYPE.get()).stream()
                .filter(h -> h.value().ingredient().test(stack))
                .sorted(java.util.Comparator.comparingInt((RecipeHolder<MortarGrindingRecipe> h) -> h.value().inputCount())
                        .reversed().thenComparing(h -> h.id().toString()));
    }

    private int inputCapacity(ItemStack stack) {
        if (level == null || stack.isEmpty()) return 64;
        return Math.min(stack.getMaxStackSize(), matchingRecipes(stack)
                .mapToInt(h -> h.value().inputCount()).max().orElse(0));
    }

    public boolean canInsert(ItemStack stack) {
        if (started || grinding || stack.isEmpty() || !output.isEmpty()) return false;
        var recipe = findRecipeForIngredient(stack);
        if (recipe.isEmpty()) return false;
        if (!input.isEmpty() && !ItemStack.isSameItemSameComponents(input, stack)) return false;
        return input.getCount() < inputCapacity(stack);
    }

    public boolean insert(ItemStack held, boolean creative) {
        if (!canInsert(held)) return false;
        if (input.isEmpty()) input = held.copyWithCount(1);
        else input.grow(1);
        if (!creative) held.shrink(1);
        cancelled = false;
        setChangedAndSync();
        return true;
    }

    public boolean takeOutput(Player player) {
        if (output.isEmpty()) return false;
        player.getInventory().placeItemBackInInventory(output.copy());
        output = ItemStack.EMPTY;
        setChangedAndSync();
        return true;
    }

    public boolean takeInput(Player player) {
        if (input.isEmpty()) return false;
        player.getInventory().placeItemBackInInventory(input.copy());
        input = ItemStack.EMPTY;
        resetProcessing();
        setChangedAndSync();
        return true;
    }

    public Component hint(String action) {
        if (!output.isEmpty()) return Component.translatable("hint.firstworks.collect");
        if (input.isEmpty()) return Component.translatable("jade.firstworks.mortar.empty");
        var recipe = getActiveRecipe();
        if (recipe.isEmpty()) return Component.translatable("hint.firstworks.unsupported");
        if (input.getCount() < recipe.get().value().inputCount()) return Component.translatable("hint.firstworks.input_count", input.getCount(), recipe.get().value().inputCount());
        if (cancelled) return Component.translatable("hint.firstworks.cancelled");
        String required = getStage().map(MortarStage::action).orElse("grind");
        if (!required.equals(action)) return Component.translatable("hint.firstworks.mortar.requires", Component.translatable("action.firstworks." + required));
        return Component.translatable("hint.firstworks.mortar." + action);
    }

    public Optional<MortarStage> getStage() {
        return getActiveRecipe().map(h -> h.value().stages().get(Math.min(stageIndex, h.value().stages().size() - 1)));
    }
    public int getStageIndex() { return stageIndex; }
    public int getStageProgress() { return stageProgress; }
    public boolean isProcessCancelled() { return cancelled; }
    public float getGrindingProgress(float partialTick) { return isGrinding() && level != null ? level.getGameTime() + partialTick : 0; }
    public float getCrushPulse(float partialTick) {
        if (level == null || lastCrushTick == Long.MIN_VALUE) return 0;
        float elapsed = (level.getGameTime() - lastCrushTick) + partialTick;
        return elapsed >= 0 && elapsed < 5 ? (float)Math.sin(Math.PI * elapsed / 5) : 0;
    }
    public ItemStack getInput() { return input; }
    public ItemStack getOutput() { return output; }
    public boolean isGrinding() { return grinding && level != null && level.getGameTime() - lastWorkTick <= 6; }
    @Deprecated public long getFinishGameTime() {
        return legacyProgressPending && legacyFinishGameTime != Long.MIN_VALUE ? legacyFinishGameTime : 0L;
    }
    public Optional<RecipeHolder<MortarGrindingRecipe>> getActiveRecipe() { return findRecipeForIngredient(input); }
    public IItemHandler getItemHandler(@Nullable Direction side) { return itemHandler; }

    private void setChangedAndSync() {
        setChanged();
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Input", input.saveOptional(registries));
        tag.put("Output", output.saveOptional(registries));
        tag.putInt("Stage", stageIndex);
        tag.putInt("StageProgress", stageProgress);
        tag.putBoolean("Started", started);
        tag.putBoolean("Cancelled", cancelled);
        tag.putBoolean("Grinding", grinding);
        tag.putLong("LastWork", lastWorkTick);
        tag.putLong("LastCrush", lastCrushTick);
        if (legacyProgressPending) {
            tag.putBoolean("LegacyProgressPending", true);
            tag.putLong("LegacyFinishGameTime", legacyFinishGameTime);
        }
        if (recipeId != null) tag.putString("Recipe", recipeId.toString());
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        input = ItemStack.parseOptional(registries, tag.getCompound("Input"));
        output = ItemStack.parseOptional(registries, tag.getCompound("Output"));
        boolean legacyFormat = tag.getBoolean("LegacyProgressPending")
                || (!tag.contains("Stage") && tag.contains("FinishGameTime") && tag.getBoolean("Grinding"));
        legacyProgressPending = legacyFormat;
        legacyFinishGameTime = tag.getBoolean("LegacyProgressPending")
                ? tag.getLong("LegacyFinishGameTime")
                : legacyFormat ? tag.getLong("FinishGameTime") : Long.MIN_VALUE;
        stageIndex = legacyFormat ? 0 : Math.max(0, tag.getInt("Stage"));
        stageProgress = legacyFormat ? 0 : Math.max(0, tag.getInt("StageProgress"));
        started = !legacyFormat && tag.getBoolean("Started");
        cancelled = !legacyFormat && tag.getBoolean("Cancelled");
        recipeId = legacyFormat ? null : ResourceLocation.tryParse(tag.getString("Recipe"));
        grinding = !legacyFormat && level != null && level.isClientSide && tag.getBoolean("Grinding");
        operator = null;
        lastWorkTick = legacyFormat ? Long.MIN_VALUE
                : tag.contains("LastWork") ? tag.getLong("LastWork") : Long.MIN_VALUE;
        lastCrushTick = legacyFormat ? Long.MIN_VALUE
                : tag.contains("LastCrush") ? tag.getLong("LastCrush") : Long.MIN_VALUE;
        if (legacyProgressPending && level != null && !level.isClientSide) migrateLegacyProgress();
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this, BlockEntity::getUpdateTag); }

    private final class MortarItemHandler implements IItemHandler {
        @Override public int getSlots() { return 2; }
        @Override public ItemStack getStackInSlot(int slot) {
            return slot == 0 ? input.copy() : slot == 1 ? output.copy() : ItemStack.EMPTY;
        }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot != 0 || !canInsert(stack)) return stack;
            int required = inputCapacity(stack);
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
            return slot == 0 ? inputCapacity(input) : 64;
        }
        @Override public boolean isItemValid(int slot, ItemStack stack) { return slot == 0 && canInsert(stack); }
    }
}
