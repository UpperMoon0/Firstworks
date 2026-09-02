package com.nstut.firstworks;

import com.nstut.firstworks.content.ColoredFleeceItem;
import com.nstut.firstworks.content.ResinScarBlock;
import com.nstut.firstworks.content.TreeBarkItem;
import com.nstut.firstworks.content.charcoal.CharcoalMoundData;
import com.nstut.firstworks.registry.ModBlocks;
import com.nstut.firstworks.registry.ModItems;
import com.nstut.firstworks.registry.ModTags;
import com.nstut.firstworks.registry.WoodTypeRegistry;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

@EventBusSubscriber(modid = Firstworks.MOD_ID)
public final class GameplayEvents {
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void tapResinTree(PlayerInteractEvent.RightClickBlock event) {
        Direction face = event.getFace();
        if (face == null || face.getAxis() == Direction.Axis.Y
                || !event.getLevel().getBlockState(event.getPos()).is(ModTags.RESIN_TREES)) {
            return;
        }

        ItemStack tool = event.getEntity().getItemInHand(event.getHand());
        if (!tool.is(ModTags.PRIMITIVE_KNIVES) && !tool.is(ModTags.RESIN_TAPPING_TOOLS)) {
            return;
        }

        var scarPos = event.getPos().relative(face);
        if (!event.getLevel().getBlockState(scarPos).canBeReplaced()) {
            return;
        }

        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
        event.setCanceled(true);
        if (event.getLevel().isClientSide) {
            return;
        }

        event.getLevel().setBlock(scarPos, ModBlocks.RESIN_SCAR.get().defaultBlockState()
                .setValue(ResinScarBlock.FACING, face.getOpposite()), Block.UPDATE_CLIENTS);
        event.getLevel().playSound(null, scarPos, SoundEvents.AXE_STRIP, SoundSource.BLOCKS, 0.55F, 1.25F);
        if (!event.getEntity().hasInfiniteMaterials()) {
            tool.hurtAndBreak(1, event.getEntity(), slotFor(event.getHand()));
        }
    }

    @SubscribeEvent
    public static void igniteCharcoalMound(PlayerInteractEvent.RightClickBlock event) {
        ItemStack tool = event.getEntity().getItemInHand(event.getHand());
        if (!tool.is(ModTags.CHARCOAL_IGNITERS)
                || !event.getLevel().getBlockState(event.getPos()).is(ModTags.CHARCOAL_WOODS)
                || !(event.getLevel() instanceof ServerLevel level)
                || event.getFace() == null) {
            return;
        }

        CharcoalMoundData moundData = CharcoalMoundData.get(level);
        CharcoalMoundData.IgnitionProbe probe = moundData.probe(level, event.getPos(), event.getFace());
        if (!probe.isMoundCandidate()) {
            return;
        }

        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
        event.setCanceled(true);
        if (!probe.isValid()) {
            if (probe.failureReason() != null) {
                event.getEntity().displayClientMessage(probe.failureReason(), true);
            }
            return;
        }

        CharcoalMoundData.IgnitionResult result = moundData.igniteFromProbe(level, probe);
        if (!result.isSuccessful()) {
            if (result.message() != null) {
                event.getEntity().displayClientMessage(result.message(), true);
            }
            return;
        }

        if (!event.getEntity().hasInfiniteMaterials()) {
            tool.hurtAndBreak(1, event.getEntity(), slotFor(event.getHand()));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (!event.isCanceled() && event.getLevel() instanceof ServerLevel level) {
            CharcoalMoundData.get(level).onBlockPlaced(level, event.getPos());
        }
    }

    @SubscribeEvent
    public static void tickCharcoalMounds(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) {
            CharcoalMoundData.get(level).tick(level);
        }
    }

    @SubscribeEvent
    public static void onAddReloadListeners(net.neoforged.neoforge.event.AddReloadListenerEvent event) {
        event.addListener(com.nstut.firstworks.content.animal.AnimalMaterialManager.INSTANCE);
    }

    @SubscribeEvent
    public static void replaceAnimalLeather(LivingDropsEvent event) {
        if (!FirstworksConfig.REPLACE_ANIMAL_LEATHER.getAsBoolean()
                || event.getEntity().getType().is(ModTags.NO_RAW_HIDE_DROPS)) {
            return;
        }

        var profile = com.nstut.firstworks.content.animal.AnimalMaterialManager
                .getProfile(event.getEntity().getType());
        int looting = getLootingLevel(event);
        if (profile.isPresent() && profile.get().hide().isPresent()) {
            int count = profile.get().hide().get().roll(event.getEntity().getRandom(), looting);
            event.getDrops().removeIf(drop -> drop.getItem().is(Items.LEATHER)
                    || drop.getItem().is(ModTags.RAW_HIDES));
            if (count > 0) {
                event.getDrops().add(new ItemEntity(event.getEntity().level(),
                        event.getEntity().getX(), event.getEntity().getY(), event.getEntity().getZ(),
                        new ItemStack(ModItems.RAW_HIDE.get(), count)));
            }
            return;
        }

        if (!event.getEntity().getType().is(ModTags.LEATHER_DROPS_AS_RAW_HIDE)) {
            return;
        }
        event.getDrops().forEach(drop -> {
            ItemStack stack = drop.getItem();
            if (stack.is(Items.LEATHER)) {
                drop.setItem(new ItemStack(ModItems.RAW_HIDE.get(), stack.getCount()));
            }
        });
    }

    @SubscribeEvent
    public static void addAnimalBones(LivingDropsEvent event) {
        if (!FirstworksConfig.ADD_ANIMAL_BONE_DROPS.getAsBoolean()
                || event.getEntity().getType().is(ModTags.NO_BONE_DROPS)) {
            return;
        }

        var profile = com.nstut.firstworks.content.animal.AnimalMaterialManager
                .getProfile(event.getEntity().getType());
        int looting = getLootingLevel(event);
        if (profile.isPresent() && profile.get().bones().isPresent()) {
            int count = profile.get().bones().get().roll(event.getEntity().getRandom(), looting);
            if (count > 0) {
                event.getDrops().add(new ItemEntity(event.getEntity().level(),
                        event.getEntity().getX(), event.getEntity().getY(), event.getEntity().getZ(),
                        new ItemStack(Items.BONE, count)));
            }
            return;
        }

        if (!event.getEntity().getType().is(ModTags.DROPS_BONES)) {
            return;
        }
        int count = 1 + event.getEntity().getRandom().nextInt(2);
        if (looting > 0) {
            count += event.getEntity().getRandom().nextInt(looting + 1);
        }
        event.getDrops().add(new ItemEntity(event.getEntity().level(),
                event.getEntity().getX(), event.getEntity().getY(), event.getEntity().getZ(),
                new ItemStack(Items.BONE, count)));
    }

    private static int getLootingLevel(LivingDropsEvent event) {
        if (event.getEntity().level() instanceof ServerLevel serverLevel
                && event.getSource() != null
                && event.getSource().getEntity() instanceof LivingEntity attacker) {
            return serverLevel.registryAccess()
                    .lookup(net.minecraft.core.registries.Registries.ENCHANTMENT)
                    .flatMap(registry -> registry.get(Enchantments.LOOTING))
                    .map(looting -> EnchantmentHelper.getEnchantmentLevel(looting, attacker))
                    .orElse(0);
        }
        return 0;
    }

    @SubscribeEvent
    public static void replaceSheepDeathWool(LivingDropsEvent event) {
        if (!FirstworksConfig.ENABLE_TEXTILE_PROGRESSION.getAsBoolean()
                || !(event.getEntity() instanceof Sheep sheep)) {
            return;
        }
        event.getDrops().forEach(drop -> {
            if (drop.getItem().is(ItemTags.WOOL)) {
                int amount = 1 + sheep.getRandom().nextInt(2);
                drop.setItem(ColoredFleeceItem.create(ModItems.RAW_FLEECE.get(), sheep.getColor(), amount));
            }
        });
    }

    @SubscribeEvent
    public static void shearFleece(PlayerInteractEvent.EntityInteract event) {
        if (!FirstworksConfig.ENABLE_TEXTILE_PROGRESSION.getAsBoolean()
                || !(event.getTarget() instanceof Sheep sheep)) {
            return;
        }

        ItemStack shears = event.getEntity().getItemInHand(event.getHand());
        if (!shears.is(Items.SHEARS) || !sheep.readyForShearing()) {
            return;
        }

        event.setCancellationResult(InteractionResult.sidedSuccess(sheep.level().isClientSide));
        event.setCanceled(true);
        if (sheep.level().isClientSide) {
            return;
        }

        sheep.level().playSound(null, sheep, SoundEvents.SHEEP_SHEAR, SoundSource.PLAYERS, 1.0F, 1.0F);
        sheep.setSheared(true);
        sheep.gameEvent(GameEvent.SHEAR, event.getEntity());
        sheep.spawnAtLocation(ColoredFleeceItem.create(ModItems.RAW_FLEECE.get(), sheep.getColor(),
                3 + sheep.getRandom().nextInt(3)));
        shears.hurtAndBreak(1, event.getEntity(), slotFor(event.getHand()));
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void knifeShearFleece(PlayerInteractEvent.EntityInteract event) {
        if (!FirstworksConfig.ENABLE_TEXTILE_PROGRESSION.getAsBoolean()
                || !(event.getTarget() instanceof Sheep sheep)
                || !sheep.readyForShearing()) {
            return;
        }

        ItemStack knife = event.getEntity().getItemInHand(event.getHand());
        if (!knife.is(ModTags.PRIMITIVE_KNIVES)) {
            return;
        }

        event.setCancellationResult(InteractionResult.sidedSuccess(sheep.level().isClientSide));
        event.setCanceled(true);
        if (sheep.level().isClientSide) {
            return;
        }

        sheep.level().playSound(null, sheep, SoundEvents.SHEEP_SHEAR, SoundSource.PLAYERS, 0.8F, 0.85F);
        sheep.setSheared(true);
        sheep.gameEvent(GameEvent.SHEAR, event.getEntity());
        sheep.spawnAtLocation(ColoredFleeceItem.create(ModItems.RAW_FLEECE.get(), sheep.getColor(), 1));
        knife.hurtAndBreak(8, event.getEntity(), slotFor(event.getHand()));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void collectBarkWhenStripping(BlockEvent.BlockToolModificationEvent event) {
        if (event.isSimulated()
                || event.getItemAbility() != ItemAbilities.AXE_STRIP
                || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        BlockState state = event.getState();
        Block block = state.getBlock();
        BlockState result = block.getToolModifiedState(state, event.getContext(), ItemAbilities.AXE_STRIP, true);
        if (result == null && event.getFinalState() == state) {
            return;
        }

        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
        String wood = WoodTypeRegistry.tryResolveWoodType(id);
        if (wood == null) {
            return;
        }
        Block.popResource(level, event.getPos(),
                TreeBarkItem.create(ModItems.TREE_BARK.get(), wood, 1 + level.getRandom().nextInt(3)));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void gatherPlantFibre(BlockEvent.BreakEvent event) {
        if (event.isCanceled() || !(event.getLevel() instanceof ServerLevel level)
                || event.getPlayer().isCreative()) {
            return;
        }

        BlockState state = event.getState();
        int amount;
        if (state.is(ModTags.DOUBLE_PLANT_FIBRE_SOURCES)) {
            amount = 2;
        } else if (state.is(ModTags.PLANT_FIBRE_SOURCES)) {
            amount = 1;
        } else {
            return;
        }

        ItemStack tool = event.getPlayer().getMainHandItem();
        boolean guaranteed = tool.is(ModTags.PRIMITIVE_KNIVES);
        if (!guaranteed && level.getRandom().nextDouble() >= FirstworksConfig.PLANT_FIBRE_HAND_CHANCE.get()) {
            return;
        }

        Block.popResource(level, event.getPos(), new ItemStack(ModItems.PLANT_FIBRE.get(), amount));
        if (guaranteed) {
            tool.hurtAndBreak(1, event.getPlayer(), EquipmentSlot.MAINHAND);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void gatherRawOchre(BlockEvent.BreakEvent event) {
        if (event.isCanceled() || !(event.getLevel() instanceof ServerLevel level)
                || event.getPlayer() == null || event.getPlayer().isCreative()) {
            return;
        }

        BlockState state = event.getState();
        if (!state.is(ModTags.OCHRE_SOURCES)) {
            return;
        }

        ItemStack tool = event.getPlayer().getMainHandItem();
        if (hasSilkTouch(level, tool)) {
            return;
        }

        boolean guaranteed = tool.is(ModTags.PRIMITIVE_KNIVES);
        if (guaranteed || level.getRandom().nextDouble() < FirstworksConfig.RAW_OCHRE_GATHER_CHANCE.get()) {
            Block.popResource(level, event.getPos(), new ItemStack(ModItems.RAW_OCHRE.get()));
            if (guaranteed) {
                tool.hurtAndBreak(1, event.getPlayer(), EquipmentSlot.MAINHAND);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!event.isCanceled() && event.getLevel() instanceof ServerLevel level) {
            CharcoalMoundData.get(level).onBlockBroken(level, event.getPos());
        }
    }

    private static boolean hasSilkTouch(ServerLevel level, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return level.registryAccess()
                .lookup(net.minecraft.core.registries.Registries.ENCHANTMENT)
                .flatMap(registry -> registry.get(Enchantments.SILK_TOUCH))
                .map(silkTouch -> EnchantmentHelper.getItemEnchantmentLevel(silkTouch, stack) > 0)
                .orElse(false);
    }

    private static EquipmentSlot slotFor(InteractionHand hand) {
        return hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
    }

    private GameplayEvents() {}
}
