package com.nstut.firstworks.compat;

import com.nstut.firstworks.compat.kubejs.KubeJSCompat;
import com.nstut.firstworks.content.barrel.BarrelRecipe;
import com.nstut.firstworks.content.barrel.BarrelBlockEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.fluids.FluidStack;

public final class OptionalIntegrations {
    private static final boolean KUBE_JS_LOADED = ModList.get().isLoaded("kubejs");

    public static boolean fireBarrelProcessStarting(ServerLevel level, BarrelBlockEntity barrel,
            ResourceLocation recipeId, BarrelRecipe recipe, ItemStack input, FluidStack inputFluid,
            ItemStack result, FluidStack outputFluid) {
        return KUBE_JS_LOADED && KubeJSCompat.fireStarting(level, barrel, recipeId, recipe, input, inputFluid, result, outputFluid);
    }

    public static void fireBarrelProcessCompleted(ServerLevel level, BarrelBlockEntity barrel,
            ResourceLocation recipeId, BarrelRecipe recipe, ItemStack input, FluidStack inputFluid,
            ItemStack result, FluidStack outputFluid) {
        if (KUBE_JS_LOADED) {
            KubeJSCompat.fireCompleted(level, barrel, recipeId, recipe, input, inputFluid, result, outputFluid);
        }
    }

    private OptionalIntegrations() {}
}
