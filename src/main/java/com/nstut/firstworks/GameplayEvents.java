package com.nstut.firstworks;

import com.nstut.firstworks.content.ColoredFleeceItem;
import com.nstut.firstworks.registry.ModItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Sheep;
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
import net.neoforged.neoforge.common.ItemAbilities;
import net.minecraft.core.registries.BuiltInRegistries;
import com.nstut.firstworks.content.TreeBarkItem;

@EventBusSubscriber(modid = Firstworks.MOD_ID)
public final class GameplayEvents {
    @SubscribeEvent
    public static void replaceAnimalLeather(LivingDropsEvent event) {
        if (!FirstworksConfig.REPLACE_ANIMAL_LEATHER.getAsBoolean()) return;
        if (!(event.getEntity() instanceof Animal)) return;
        event.getDrops().forEach(drop -> {
            ItemStack stack = drop.getItem();
            if (stack.is(Items.LEATHER)) {
                drop.setItem(new ItemStack(ModItems.RAW_HIDE.get(), stack.getCount()));
            }
        });
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

        String woodType = getWoodTypeForBlock(block);
        int barkCount = 1 + level.getRandom().nextInt(3);

        Block.popResource(
                level,
                event.getPos(),
                TreeBarkItem.create(ModItems.TREE_BARK.get(), woodType, barkCount)
        );
    }

    private static String getWoodTypeForBlock(Block block) {
        return com.nstut.firstworks.registry.WoodTypeRegistry.resolveWoodType(
                BuiltInRegistries.BLOCK.getKey(block));
    }

    @SubscribeEvent
    public static void gatherPlantFibre(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        Block block = event.getState().getBlock();
        int amount;
        if (block == Blocks.TALL_GRASS || block == Blocks.LARGE_FERN) {
            amount = 2;
        } else if (block == Blocks.FERN || block == Blocks.SHORT_GRASS) {
            amount = 1;
        } else {
            return;
        }

        ItemStack tool = event.getPlayer().getMainHandItem();
        boolean guaranteed = tool.is(ItemTags.SWORDS);
        if (!guaranteed && level.getRandom().nextFloat() >= 0.30F) return;
        Block.popResource(level, event.getPos(), new ItemStack(ModItems.PLANT_FIBRE.get(), amount));
    }

    private GameplayEvents() {}
}
