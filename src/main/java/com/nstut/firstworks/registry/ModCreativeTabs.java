package com.nstut.firstworks.registry;

import com.nstut.firstworks.Firstworks;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Firstworks.MOD_ID);
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> FIRSTWORKS = TABS.register("firstworks",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.firstworks"))
                    .icon(() -> ModItems.BARREL.get().getDefaultInstance())
                    .displayItems((parameters, output) -> ModItems.ITEMS.getEntries().forEach(item -> output.accept(item.get())))
                    .build());

    public static void register(IEventBus bus) {
        TABS.register(bus);
    }

    private ModCreativeTabs() {}
}
