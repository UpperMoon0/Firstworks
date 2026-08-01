package com.nstut.firstworks.compat.kubejs;

import com.nstut.firstworks.content.barrel.BarrelRecipe;
import com.nstut.firstworks.content.barrel.BarrelBlockEntity;
import dev.latvian.mods.kubejs.script.ScriptType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

public final class KubeJSCompat {
    public static boolean fireStarting(ServerLevel level, BarrelBlockEntity barrel, ResourceLocation recipeId,
            BarrelRecipe recipe, ItemStack input, FluidStack inputFluid, ItemStack result, FluidStack outputFluid) {
        return FirstworksKubeEvents.BARREL_PROCESS_STARTING.post(ScriptType.SERVER,
                new BarrelProcessKubeEvent(level, barrel, recipeId, recipe, input, inputFluid, result, outputFluid))
                .interruptFalse();
    }

    public static void fireCompleted(ServerLevel level, BarrelBlockEntity barrel, ResourceLocation recipeId,
            BarrelRecipe recipe, ItemStack input, FluidStack inputFluid, ItemStack result, FluidStack outputFluid) {
        FirstworksKubeEvents.BARREL_PROCESS_COMPLETED.post(ScriptType.SERVER,
                new BarrelProcessKubeEvent(level, barrel, recipeId, recipe, input, inputFluid, result, outputFluid));
    }

    private KubeJSCompat() {}
}
