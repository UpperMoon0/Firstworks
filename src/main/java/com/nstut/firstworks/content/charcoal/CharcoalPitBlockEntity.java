package com.nstut.firstworks.content.charcoal;

import com.nstut.firstworks.Firstworks;
import com.nstut.firstworks.registry.ModBlockEntities;
import com.nstut.firstworks.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;

public final class CharcoalPitBlockEntity extends BlockEntity {
    public static final int CAPACITY = 16;
    public static final int PROCESS_TICKS = 6_000;
    private static final TagKey<Item> FUELS = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(Firstworks.MOD_ID, "charcoal_pit_fuels"));

    private ItemStack stored = ItemStack.EMPTY;
    /** Absolute game time keeps carbonization advancing while this chunk is unloaded. */
    private long finishGameTime;
    /** Read only to migrate pits saved by the development build before absolute timing. */
    private int legacyProgress;
    private boolean burning;
    private final IItemHandler automation = new PitItemHandler();

    public CharcoalPitBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHARCOAL_PIT.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, CharcoalPitBlockEntity pit) {
        if (level.isClientSide || !pit.burning) return;
        if (!level.getBlockState(pos.above()).is(ModTags.CHARCOAL_SEALANTS)) {
            pit.burning = false;
            pit.finishGameTime = 0L;
            pit.legacyProgress = 0;
            pit.syncLit(false);
            return;
        }
        long now = level.getGameTime();
        if (pit.finishGameTime <= 0L) {
            pit.finishGameTime = now + Math.max(1, PROCESS_TICKS - pit.legacyProgress);
            pit.legacyProgress = 0;
            pit.setChanged();
        }
        if (now < pit.finishGameTime) {
            return;
        }
        int count = Math.max(1, pit.stored.getCount() * 3 / 4);
        pit.stored = new ItemStack(Items.CHARCOAL, count);
        pit.finishGameTime = 0L;
        pit.legacyProgress = 0;
        pit.burning = false;
        pit.syncLit(false);
    }

    public boolean canAccept(ItemStack stack) {
        return !burning && !stack.isEmpty() && stack.is(FUELS)
                && canMergeFuelStacks(stored, stack)
                && stored.getCount() < CAPACITY;
    }

    static boolean canMergeFuelStacks(ItemStack stored, ItemStack incoming) {
        return stored.isEmpty() || ItemStack.isSameItemSameComponents(stored, incoming);
    }

    public int insertLogs(ItemStack stack, boolean creative) {
        if (!canAccept(stack)) return 0;
        int inserted = Math.min(stack.getCount(), CAPACITY - stored.getCount());
        if (stored.isEmpty()) stored = stack.copyWithCount(inserted);
        else stored.grow(inserted);
        if (!creative) stack.shrink(inserted);
        setChanged();
        return inserted;
    }

    public boolean canIgnite() {
        return !burning && !stored.isEmpty() && stored.is(FUELS);
    }

    public boolean ignite() {
        if (!canIgnite() || level == null
                || !level.getBlockState(worldPosition.above()).is(ModTags.CHARCOAL_SEALANTS)) return false;
        burning = true;
        finishGameTime = level.getGameTime() + PROCESS_TICKS;
        legacyProgress = 0;
        syncLit(true);
        return true;
    }

    public boolean retrieve(Player player) {
        if (burning || stored.isEmpty()) return false;
        player.getInventory().placeItemBackInInventory(stored.copy());
        stored = ItemStack.EMPTY;
        finishGameTime = 0L;
        legacyProgress = 0;
        setChanged();
        return true;
    }

    public ItemStack getStoredStack() { return stored.copy(); }
    public int getProgress() {
        if (!burning || level == null || finishGameTime <= 0L) return legacyProgress;
        long elapsed = PROCESS_TICKS - Math.max(0L, finishGameTime - level.getGameTime());
        return (int) Math.clamp(elapsed, 0L, PROCESS_TICKS);
    }
    public boolean isBurning() { return burning; }
    public IItemHandler getItemHandler() { return automation; }

    private void syncLit(boolean lit) {
        setChanged();
        if (level != null && getBlockState().getValue(CharcoalPitBlock.LIT) != lit) {
            level.setBlock(worldPosition, getBlockState().setValue(CharcoalPitBlock.LIT, lit), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Stored", stored.saveOptional(registries));
        tag.putLong("FinishGameTime", finishGameTime);
        tag.putInt("Progress", getProgress());
        tag.putBoolean("Burning", burning);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        stored = ItemStack.parseOptional(registries, tag.getCompound("Stored"));
        finishGameTime = tag.getLong("FinishGameTime");
        legacyProgress = finishGameTime <= 0L ? tag.getInt("Progress") : 0;
        burning = tag.getBoolean("Burning");
    }

    private final class PitItemHandler implements IItemHandler {
        @Override public int getSlots() { return 1; }
        @Override public ItemStack getStackInSlot(int slot) { return slot == 0 ? stored.copy() : ItemStack.EMPTY; }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot != 0 || !canAccept(stack)) return stack;
            int inserted = Math.min(stack.getCount(), CAPACITY - stored.getCount());
            if (!simulate) {
                ItemStack input = stack.copy();
                insertLogs(input, false);
            }
            return stack.copyWithCount(stack.getCount() - inserted);
        }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot != 0 || burning || !stored.is(Items.CHARCOAL) || amount <= 0) return ItemStack.EMPTY;
            int extracted = Math.min(amount, stored.getCount());
            ItemStack result = stored.copyWithCount(extracted);
            if (!simulate) {
                stored.shrink(extracted);
                if (stored.isEmpty()) stored = ItemStack.EMPTY;
                setChanged();
            }
            return result;
        }
        @Override public int getSlotLimit(int slot) { return CAPACITY; }
        @Override public boolean isItemValid(int slot, ItemStack stack) { return slot == 0 && canAccept(stack); }
    }
}
