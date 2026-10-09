package com.nstut.firstworks.content.barrel;

import com.mojang.serialization.MapCodec;
import com.nstut.firstworks.registry.ModBlockEntities;
import com.nstut.firstworks.registry.ModFluids;
import com.nstut.firstworks.FirstworksConfig;
import com.nstut.firstworks.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.biome.Biome.Precipitation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import net.neoforged.neoforge.fluids.FluidStack;

public class BarrelBlock extends BaseEntityBlock {
    public static final MapCodec<BarrelBlock> CODEC = simpleCodec(BarrelBlock::new);
    public static final BooleanProperty SEALED = BooleanProperty.create("sealed");
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    private static final VoxelShape OPEN_SHAPE = Shapes.or(
            box(1, 0, 1, 15, 2, 15),
            box(1, 2, 1, 15, 14, 3),
            box(1, 2, 13, 15, 14, 15),
            box(1, 2, 3, 3, 14, 13),
            box(13, 2, 3, 15, 14, 13)
    ).optimize();
    private static final VoxelShape SEALED_SHAPE = Shapes.or(
            OPEN_SHAPE,
            box(2, 14, 2, 14, 15, 14),
            box(1, 15, 1, 15, 16, 3),
            box(1, 15, 13, 15, 16, 15),
            box(1, 15, 3, 3, 16, 13),
            box(13, 15, 3, 15, 16, 13),
            box(3, 15, 7, 13, 16, 9),
            box(6, 16, 6, 10, 17, 10),
            box(5, 17, 5, 11, 18, 11)
    ).optimize();

    public BarrelBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(SEALED, false).setValue(POWERED, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(SEALED, POWERED);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (oldState.getBlock() != state.getBlock() && level instanceof ServerLevel serverLevel) {
            checkRedstonePulse(state, serverLevel, pos);
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
            BlockPos neighborPos, boolean movedByPiston) {
        if (level instanceof ServerLevel serverLevel) {
            checkRedstonePulse(state, serverLevel, pos);
        }
    }

    private void checkRedstonePulse(BlockState state, ServerLevel level, BlockPos pos) {
        boolean powered = level.hasNeighborSignal(pos);
        if (powered == state.getValue(POWERED)) return;

        BlockState updated = state.setValue(POWERED, powered);
        if (powered) {
            boolean sealed = !state.getValue(SEALED);
            updated = updated.setValue(SEALED, sealed);
            level.setBlock(pos, updated, 3);
            if (level.getBlockEntity(pos) instanceof BarrelBlockEntity barrel) {
                barrel.onLidChanged(sealed);
            }
            level.playSound(null, pos, sealed ? SoundEvents.WOODEN_TRAPDOOR_CLOSE : SoundEvents.WOODEN_TRAPDOOR_OPEN,
                    SoundSource.BLOCKS, 0.8F, 0.9F);
            return;
        }
        level.setBlock(pos, updated, 3);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(SEALED) ? SEALED_SHAPE : OPEN_SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(SEALED) ? SEALED_SHAPE : OPEN_SHAPE;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BarrelBlockEntity(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == ModBlockEntities.BARREL.get()
                ? (tickerLevel, pos, tickerState, entity) -> BarrelBlockEntity.tick(tickerLevel, pos, tickerState,
                        (BarrelBlockEntity) entity)
                : null;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof BarrelBlockEntity barrel)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (stack.isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (state.getValue(SEALED)) {
            transferFeedback(player, "sealed");
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        // A fluid-capable item may also be a datapack barrel recipe ingredient.
        // Explicit sneak-use inserts a matching ingredient; ordinary use transfers fluid.
        if (player.isShiftKeyDown() && barrel.canInsert(stack)) {
            if (!level.isClientSide && barrel.insertIngredient(stack, player.getAbilities().instabuild)) {
                level.playSound(null, pos, SoundEvents.COMPOSTER_FILL_SUCCESS, SoundSource.BLOCKS, 0.8F, 1.0F);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        PotionContents potion = stack.get(DataComponents.POTION_CONTENTS);
        if (stack.is(ModItems.WATER_CLAY_BUCKET.get())) {
            if (!level.isClientSide && barrel.addInputWater(1000)) {
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                    player.getInventory().placeItemBackInInventory(new ItemStack(ModItems.CLAY_BUCKET.get()));
                }
                level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
            } else if (!level.isClientSide) {
                transferFeedback(player, failedInputReason(barrel, new FluidStack(net.minecraft.world.level.material.Fluids.WATER, 1000)));
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        if (stack.is(ModItems.TANNIN_CLAY_BUCKET.get())) {
            if (!level.isClientSide && barrel.addInputFluid(new FluidStack(ModFluids.TANNIN_SOLUTION.get(), 1_000))) {
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                    player.getInventory().placeItemBackInInventory(new ItemStack(ModItems.CLAY_BUCKET.get()));
                }
                level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
            } else if (!level.isClientSide) {
                transferFeedback(player, failedInputReason(barrel, new FluidStack(ModFluids.TANNIN_SOLUTION.get(), 1000)));
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        if (stack.is(ModItems.CLAY_BUCKET.get())) {
            if (!level.isClientSide) {
                ItemStack filledBucket = barrel.drainClayBucket();
                if (!filledBucket.isEmpty()) {
                    if (!player.getAbilities().instabuild) stack.shrink(1);
                    player.getInventory().placeItemBackInInventory(filledBucket);
                    level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
                } else {
                    transferFeedback(player, barrel.getTotalFluidAmount() == 0 ? "empty" : "refused");
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        if (stack.is(Items.POTION) && potion != null && potion.is(Potions.WATER) && !potion.hasEffects()) {
            if (!level.isClientSide && barrel.addInputWater(250)) {
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                    player.getInventory().placeItemBackInInventory(new ItemStack(Items.GLASS_BOTTLE));
                }
                level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
            } else if (!level.isClientSide) {
                transferFeedback(player, failedInputReason(barrel, new FluidStack(net.minecraft.world.level.material.Fluids.WATER, 250)));
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        // Clay buckets above retain their deliberate water/tannin-only behavior.
        // Manual access uses both stores from every face; automation keeps its sided handlers.
        if (!(stack.getItem() instanceof net.minecraft.world.item.MobBucketItem)
                && net.neoforged.neoforge.fluids.FluidUtil.getFluidHandler(stack).isPresent()) {
            if (!level.isClientSide) {
                var handler = barrel.getAutomationFluidHandler();
                boolean transferred;
                if (player.getAbilities().instabuild) {
                    // FluidUtil's creative shortcut drains without returning a container.
                    // Fill one copy deliberately and return it, retaining the held creative stack.
                    var result = net.neoforged.neoforge.fluids.FluidUtil.tryFillContainer(
                            stack, handler, Integer.MAX_VALUE, player, true);
                    transferred = result.isSuccess();
                    if (transferred) {
                        player.getInventory().placeItemBackInInventory(result.getResult());
                    } else {
                        // Creative pouring retains its source container; never use the
                        // fill-and-stow shortcut, including for partially filled tanks.
                        transferred = net.neoforged.neoforge.fluids.FluidUtil.tryEmptyContainer(
                                stack, handler, Integer.MAX_VALUE, player, true).isSuccess();
                    }
                } else {
                    transferred = net.neoforged.neoforge.fluids.FluidUtil.interactWithFluidHandler(
                            player, hand, handler);
                }
                if (!transferred) transferFeedback(player, failedTransferReason(barrel, stack));
            }
            // Consume rejected transfers too, so a bucket cannot place fluid into/around the barrel.
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        if (barrel.canInsert(stack)) {
            if (!level.isClientSide && barrel.insertIngredient(stack, player.getAbilities().instabuild)) {
                level.playSound(null, pos, SoundEvents.COMPOSTER_FILL_SUCCESS, SoundSource.BLOCKS, 0.8F, 1.0F);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    private static boolean isSupportedOffhandFluidContainer(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.is(ModItems.CLAY_BUCKET.get()) || stack.is(ModItems.WATER_CLAY_BUCKET.get())
                || stack.is(ModItems.TANNIN_CLAY_BUCKET.get())) return true;
        PotionContents potion = stack.get(DataComponents.POTION_CONTENTS);
        if (stack.is(Items.POTION) && potion != null && potion.is(Potions.WATER) && !potion.hasEffects()) {
            return true;
        }
        return !(stack.getItem() instanceof net.minecraft.world.item.MobBucketItem)
                && net.neoforged.neoforge.fluids.FluidUtil.getFluidHandler(stack).isPresent();
    }

    public static String failedTransferReason(BarrelBlockEntity barrel, ItemStack stack) {
        var contained = net.neoforged.neoforge.fluids.FluidUtil.getFluidContained(stack);
        if (contained.isPresent()) {
            return failedInputReason(barrel, contained.get());
        } else if (barrel.getTotalFluidAmount() == 0) {
            return "empty";
        }
        return "refused";
    }

    private static String failedInputReason(BarrelBlockEntity barrel, FluidStack fluid) {
        var input = barrel.getInputTank().getFluid();
        if (!input.isEmpty() && !FluidStack.isSameFluidSameComponents(input, fluid)) return "incompatible";
        if (BarrelBlockEntity.CAPACITY - barrel.getTotalFluidAmount() < fluid.getAmount()) return "capacity";
        return "refused";
    }

    private static void transferFeedback(Player player, String reason) {
        if (player.level().isClientSide) return;
        var data = player.getPersistentData();
        long now = player.level().getGameTime();
        String key = "FirstworksBarrelFeedback";
        if (data.contains(key) && now >= data.getLong(key) && now - data.getLong(key) < 20) return;
        data.putLong(key, now);
        player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                "message.firstworks.barrel." + reason), true);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof BarrelBlockEntity barrel)) {
            return InteractionResult.PASS;
        }
        // Vanilla attempts main-hand block fallback before the offhand item.
        // Do not collect output or toggle the lid ahead of an offhand container.
        if (isSupportedOffhandFluidContainer(player.getOffhandItem())) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide && player.isShiftKeyDown() && barrel.retrieveInput(player)) {
            return InteractionResult.SUCCESS;
        }
        if (!level.isClientSide && barrel.takeOutput(player)) {
            return InteractionResult.SUCCESS;
        }
        if (!level.isClientSide) {
            boolean sealed = !state.getValue(SEALED);
            level.setBlock(pos, state.setValue(SEALED, sealed), 3);
            barrel.onLidChanged(sealed);
            level.playSound(null, pos, sealed ? SoundEvents.WOODEN_TRAPDOOR_CLOSE : SoundEvents.WOODEN_TRAPDOOR_OPEN,
                    SoundSource.BLOCKS, 0.8F, 0.9F);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (state.getBlock() != newState.getBlock() && level.getBlockEntity(pos) instanceof BarrelBlockEntity barrel) {
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, barrel.getIngredient());
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, barrel.getOutput());
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    @Override
    public void handlePrecipitation(BlockState state, Level level, BlockPos pos, Precipitation rainfall) {
        super.handlePrecipitation(state, level, pos, rainfall);
        if (!FirstworksConfig.RAIN_FILLS_BARRELS.get()) return;
        if (state.getValue(SEALED)) return;
        if (rainfall != Precipitation.RAIN) return;
        if (level.getBlockEntity(pos) instanceof BarrelBlockEntity barrel) {
            barrel.addRainWater();
        }
    }
}
