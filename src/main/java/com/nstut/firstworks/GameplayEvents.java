package com.nstut.firstworks;

import com.nstut.firstworks.content.ColoredFleeceItem;
import com.nstut.firstworks.content.charcoal.CharcoalMoundData;
import com.nstut.firstworks.registry.ModItems;
import com.nstut.firstworks.registry.ModTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.tags.ItemTags;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.common.ItemAbilities;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import com.nstut.firstworks.content.TreeBarkItem;
import com.nstut.firstworks.registry.WoodTypeRegistry;

@EventBusSubscriber(modid = Firstworks.MOD_ID)
public final class GameplayEvents {
    @SubscribeEvent
    public static void igniteCharcoalMound(PlayerInteractEvent.RightClickBlock event) {
        ItemStack tool = event.getEntity().getItemInHand(event.getHand());
        if (!tool.is(ModTags.CHARCOAL_IGNITERS)) return;
        if (!event.getLevel().getBlockState(event.getPos()).is(ModTags.CHARCOAL_WOODS)) return;

        // Only claim the interaction when this log is a viable mound probe.
        // Ordinary trees must retain vanilla flint-and-steel/fire-starter behavior.
        if (!(event.getLevel() instanceof ServerLevel level) || event.getFace() == null
                || !CharcoalMoundData.canIgnite(level, event.getPos(), event.getFace())) return;

        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
        event.setCanceled(true);

        CharcoalMoundData.IgnitionResult result = CharcoalMoundData.get(level)
                .ignite(level, event.getPos(), event.getFace());
        if (!result.isSuccessful()) {
            if (result.message() != null) {
                event.getEntity().displayClientMessage(result.message(), true);
            }
            return;
        }
        if (!event.getEntity().hasInfiniteMaterials()) {
            EquipmentSlot slot = event.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND
                    ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
            tool.hurtAndBreak(1, event.getEntity(), slot);
        }
    }

    @SubscribeEvent
    public static void tickCharcoalMounds(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) CharcoalMoundData.get(level).tick(level);
    }

    @SubscribeEvent
    public static void onAddReloadListeners(net.neoforged.neoforge.event.AddReloadListenerEvent event) {
        event.addListener(com.nstut.firstworks.content.animal.AnimalMaterialManager.INSTANCE);
    }

    @SubscribeEvent
    public static void replaceAnimalLeather(LivingDropsEvent event) {
        if (!FirstworksConfig.REPLACE_ANIMAL_LEATHER.getAsBoolean()) return;
        if (event.getEntity().getType().is(ModTags.NO_RAW_HIDE_DROPS)) return;

        var profile = com.nstut.firstworks.content.animal.AnimalMaterialManager.getProfile(event.getEntity().getType());
        int looting = getLootingLevel(event);
        if (profile.isPresent() && profile.get().hide().isPresent()) {
            int count = profile.get().hide().get().roll(event.getEntity().getRandom(), looting);
            // Normalize drops: remove any existing leather or modded raw hides (e.g. naturalist:hide)
            event.getDrops().removeIf(drop -> drop.getItem().is(Items.LEATHER) || drop.getItem().is(ModTags.RAW_HIDES));
            if (count > 0) {
                event.getDrops().add(new ItemEntity(event.getEntity().level(), event.getEntity().getX(),
                        event.getEntity().getY(), event.getEntity().getZ(), new ItemStack(ModItems.RAW_HIDE.get(), count)));
            }
            return;
        }

        if (!event.getEntity().getType().is(ModTags.LEATHER_DROPS_AS_RAW_HIDE)) return;
        event.getDrops().forEach(drop -> {
            ItemStack stack = drop.getItem();
            if (stack.is(Items.LEATHER)) {
                drop.setItem(new ItemStack(ModItems.RAW_HIDE.get(), stack.getCount()));
            }
        });
    }

    @SubscribeEvent
    public static void addAnimalBones(LivingDropsEvent event) {
        if (!FirstworksConfig.ADD_ANIMAL_BONE_DROPS.getAsBoolean()) return;
        if (event.getEntity().getType().is(ModTags.NO_BONE_DROPS)) return;

        var profile = com.nstut.firstworks.content.animal.AnimalMaterialManager.getProfile(event.getEntity().getType());
        int looting = getLootingLevel(event);
        if (profile.isPresent() && profile.get().bones().isPresent()) {
            int count = profile.get().bones().get().roll(event.getEntity().getRandom(), looting);
            if (count > 0) {
                event.getDrops().add(new ItemEntity(event.getEntity().level(), event.getEntity().getX(),
                        event.getEntity().getY(), event.getEntity().getZ(), new ItemStack(Items.BONE, count)));
            }
            return;
        }

        if (!event.getEntity().getType().is(ModTags.DROPS_BONES)) return;
        int count = 1 + event.getEntity().getRandom().nextInt(2);
        if (looting > 0) {
            count += event.getEntity().getRandom().nextInt(looting + 1);
        }
        event.getDrops().add(new ItemEntity(event.getEntity().level(), event.getEntity().getX(),
                event.getEntity().getY(), event.getEntity().getZ(), new ItemStack(Items.BONE, count)));
    }

    private static int getLootingLevel(LivingDropsEvent event) {
        if (event.getEntity().level() instanceof ServerLevel serverLevel && event.getSource() != null
                && event.getSource().getEntity() instanceof net.minecraft.world.entity.LivingEntity attacker) {
            return serverLevel.registryAccess().lookup(net.minecraft.core.registries.Registries.ENCHANTMENT)
                    .flatMap(reg -> reg.get(net.minecraft.world.item.enchantment.Enchantments.LOOTING))
                    .map(looting -> net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantmentLevel(looting, attacker))
                    .orElse(0);
        }
        return 0;
    }

    @SubscribeEvent
    public static void replaceSheepDeathWool(LivingDropsEvent event) {
        if (!FirstworksConfig.ENABLE_TEXTILE_PROGRESSION.getAsBoolean()) return;
        if (!(event.getEntity() instanceof Sheep sheep)) return;
        event.getDrops().forEach(drop -> {
            if (!drop.getItem().is(ItemTags.WOOL)) return;
            int amount = 1 + sheep.getRandom().nextInt(2);
            drop.setItem(ColoredFleeceItem.create(ModItems.RAW_FLEECE.get(), sheep.getColor(), amount));
        });
    }

    @SubscribeEvent
    public static void shearFleece(PlayerInteractEvent.EntityInteract event) {
        if (!FirstworksConfig.ENABLE_TEXTILE_PROGRESSION.getAsBoolean()) return;
        if (!(event.getTarget() instanceof Sheep sheep)) return;
        ItemStack shears = event.getEntity().getItemInHand(event.getHand());
        if (!shears.is(Items.SHEARS) || !sheep.readyForShearing()) return;

        event.setCancellationResult(InteractionResult.sidedSuccess(sheep.level().isClientSide));
        event.setCanceled(true);
        if (sheep.level().isClientSide) return;

        sheep.level().playSound(null, sheep, SoundEvents.SHEEP_SHEAR, SoundSource.PLAYERS, 1.0F, 1.0F);
        sheep.setSheared(true);
        sheep.gameEvent(GameEvent.SHEAR, event.getEntity());
        int amount = 3 + sheep.getRandom().nextInt(3);
        sheep.spawnAtLocation(ColoredFleeceItem.create(ModItems.RAW_FLEECE.get(), sheep.getColor(), amount));
        EquipmentSlot slot = event.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND
                ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
        shears.hurtAndBreak(1, event.getEntity(), slot);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void knifeShearFleece(PlayerInteractEvent.EntityInteract event) {
        if (!FirstworksConfig.ENABLE_TEXTILE_PROGRESSION.getAsBoolean()) return;
        if (!(event.getTarget() instanceof Sheep sheep) || !sheep.readyForShearing()) return;
        ItemStack knife = event.getEntity().getItemInHand(event.getHand());
        if (!knife.is(ModTags.PRIMITIVE_KNIVES)) return;

        event.setCancellationResult(InteractionResult.sidedSuccess(sheep.level().isClientSide));
        event.setCanceled(true);
        if (sheep.level().isClientSide) return;

        sheep.level().playSound(null, sheep, SoundEvents.SHEEP_SHEAR, SoundSource.PLAYERS, 0.8F, 0.85F);
        sheep.setSheared(true);
        sheep.gameEvent(GameEvent.SHEAR, event.getEntity());
        sheep.spawnAtLocation(ColoredFleeceItem.create(ModItems.RAW_FLEECE.get(), sheep.getColor(), 1));
        EquipmentSlot slot = event.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND
                ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
        knife.hurtAndBreak(8, event.getEntity(), slot);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void collectBarkWhenStripping(BlockEvent.BlockToolModificationEvent event) {
        if (event.isSimulated() || event.getItemAbility() != ItemAbilities.AXE_STRIP) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        BlockState state = event.getState();
        Block block = state.getBlock();

        BlockState blockResult = block.getToolModifiedState(
                state,
                event.getContext(),
                ItemAbilities.AXE_STRIP,
                true
        );

        boolean modifiedByEvent = event.getFinalState() != state;

        if (blockResult == null && !modifiedByEvent) return;

        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);
        String woodType = WoodTypeRegistry.tryResolveWoodType(blockId);
        if (woodType == null) return;

        int barkCount = 1 + level.getRandom().nextInt(3);

        Block.popResource(
                level,
                event.getPos(),
                TreeBarkItem.create(ModItems.TREE_BARK.get(), woodType, barkCount)
        );
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void gatherPlantFibre(BlockEvent.BreakEvent event) {
        if (event.isCanceled() || !(event.getLevel() instanceof ServerLevel level)) return;
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
        double chance = FirstworksConfig.PLANT_FIBRE_HAND_CHANCE.get();
        if (!guaranteed && level.getRandom().nextDouble() >= chance) return;
        Block.popResource(level, event.getPos(), new ItemStack(ModItems.PLANT_FIBRE.get(), amount));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void gatherRawOchre(BlockEvent.BreakEvent event) {
        if (event.isCanceled() || !(event.getLevel() instanceof ServerLevel level)) return;
        if (event.getPlayer() == null || event.getPlayer().isCreative()) return;
        BlockState state = event.getState();
        if (!state.is(ModTags.OCHRE_SOURCES)) return;

        ItemStack tool = event.getPlayer().getMainHandItem();
        if (hasSilkTouch(level, tool)) return;

        boolean guaranteed = tool.is(ModTags.PRIMITIVE_KNIVES);
        double chance = FirstworksConfig.RAW_OCHRE_GATHER_CHANCE.get();
        if (guaranteed || level.getRandom().nextDouble() < chance) {
            Block.popResource(level, event.getPos(), new ItemStack(ModItems.RAW_OCHRE.get(), 1));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.isCanceled() || !(event.getLevel() instanceof ServerLevel level)) return;
        CharcoalMoundData.get(level).onBlockBroken(level, event.getPos());
    }

    private static boolean hasSilkTouch(ServerLevel level, ItemStack stack) {
        if (stack.isEmpty()) return false;
        return level.registryAccess().lookup(net.minecraft.core.registries.Registries.ENCHANTMENT)
                .flatMap(reg -> reg.get(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH))
                .map(st -> net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(st, stack) > 0)
                .orElse(false);
    }

    private GameplayEvents() {}
}
