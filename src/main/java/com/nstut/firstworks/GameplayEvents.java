package com.nstut.firstworks;

import com.nstut.firstworks.registry.ModItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps;

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
    public static void collectBarkWhenStripping(BlockEvent.BlockToolModificationEvent event) {
        if (event.isSimulated() || event.getItemAbility() != ItemAbilities.AXE_STRIP) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (event.getState().getBlock().builtInRegistryHolder().getData(NeoForgeDataMaps.STRIPPABLES) == null) return;
        int barkCount = 1 + level.getRandom().nextInt(3);
        Block.popResource(level, event.getPos(), new ItemStack(ModItems.TREE_BARK.get(), barkCount));
    }

    private GameplayEvents() {}
}
