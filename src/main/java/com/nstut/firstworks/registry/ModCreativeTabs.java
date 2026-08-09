package com.nstut.firstworks.registry;

import com.nstut.firstworks.content.ColoredFleeceItem;
import com.nstut.firstworks.content.TreeBarkItem;
import com.nstut.firstworks.Firstworks;
import com.nstut.firstworks.content.barrel.BarrelBlock;
import com.nstut.firstworks.content.loom.LoomBlock;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.HashSet;
import java.util.List;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Firstworks.MOD_ID);
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> FIRSTWORKS = TABS.register("firstworks",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.firstworks"))
                    .icon(() -> ModItems.BARREL.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        var firstworksItems = new HashSet<Item>();
                        ModItems.ITEMS.getEntries().forEach(itemHolder -> {
                            Item item = itemHolder.get();
                            firstworksItems.add(item);
                            if (item == ModItems.TREE_BARK.get()) {
                                List.of("oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "bamboo", "crimson", "warped")
                                        .forEach(wood -> output.accept(TreeBarkItem.create(item, wood, 1)));
                            } else if (item == ModItems.RAW_FLEECE.get() || item == ModItems.CLEAN_WOOL.get()) {
                                for (DyeColor color : DyeColor.values()) {
                                    output.accept(ColoredFleeceItem.create(item, color, 1));
                                }
                            } else {
                                output.accept(item);
                            }
                        });
                        BuiltInRegistries.ITEM.stream()
                                .filter(item -> !firstworksItems.contains(item))
                                .filter(item -> item instanceof BlockItem blockItem
                                        && (blockItem.getBlock() instanceof BarrelBlock || blockItem.getBlock() instanceof LoomBlock))
                                .forEach(output::accept);
                    })
                    .build());

    public static void register(IEventBus bus) {
        TABS.register(bus);
    }

    private ModCreativeTabs() {}
}
