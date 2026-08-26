package com.nstut.firstworks.compat.kubejs;

import com.nstut.firstworks.Firstworks;
import dev.latvian.mods.kubejs.event.EventGroupRegistry;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.registry.BuilderTypeRegistry;
import dev.latvian.mods.kubejs.script.BindingRegistry;
import net.minecraft.core.registries.Registries;

public final class FirstworksKubeJSPlugin implements KubeJSPlugin {
    @Override
    public void registerBuilderTypes(BuilderTypeRegistry registry) {
        registry.of(Registries.BLOCK, blocks -> {
            blocks.add(Firstworks.id("loom"), FirstworksLoomBuilder.class, FirstworksLoomBuilder::new);
            blocks.add(Firstworks.id("barrel"), FirstworksBarrelBuilder.class, FirstworksBarrelBuilder::new);
        });
    }

    @Override
    public void registerEvents(EventGroupRegistry registry) {
        registry.register(FirstworksKubeEvents.GROUP);
    }

    @Override
    public void registerBindings(BindingRegistry bindings) {
        if (bindings.type().isStartup()) {
            bindings.add("Firstworks", FirstworksKubeJS.INSTANCE);
        }
    }
}
