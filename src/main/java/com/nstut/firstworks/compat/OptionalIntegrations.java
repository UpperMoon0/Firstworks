package com.nstut.firstworks.compat;

import com.nstut.firstworks.compat.kubejs.KubeJSCompat;
import com.nstut.firstworks.content.barrel.BarrelRecipe;
import com.nstut.firstworks.content.barrel.BarrelBlockEntity;
import com.nstut.firstworks.content.loom.LoomBlockEntity;
import com.nstut.firstworks.content.loom.LoomRecipe;
import com.nstut.firstworks.content.SpinningRecipe;
import com.nstut.firstworks.content.brick_mold.BrickMoldBlockEntity;
import com.nstut.firstworks.content.brick_mold.BrickMoldingRecipe;
import com.nstut.firstworks.content.mortar.MortarBlockEntity;
import com.nstut.firstworks.content.MortarGrindingRecipe;
import com.nstut.firstworks.content.quern.QuernBlockEntity;
import com.nstut.firstworks.content.quern.QuernGrindingRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
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

    public static boolean fireLoomWeavingStarting(ServerLevel level, LoomBlockEntity loom,
            ResourceLocation recipeId, LoomRecipe recipe, ItemStack input, ItemStack result) {
        return KUBE_JS_LOADED && KubeJSCompat.fireLoomStarting(level, loom, recipeId, recipe, input, result);
    }

    public static void fireLoomWeavingCompleted(ServerLevel level, LoomBlockEntity loom,
            ResourceLocation recipeId, LoomRecipe recipe, ItemStack input, ItemStack result) {
        if (KUBE_JS_LOADED) {
            KubeJSCompat.fireLoomCompleted(level, loom, recipeId, recipe, input, result);
        }
    }

    public static boolean fireSpindleSpinningStarting(ServerLevel level, Player player,
            ResourceLocation recipeId, SpinningRecipe recipe, ItemStack input, ItemStack result) {
        return KUBE_JS_LOADED && KubeJSCompat.fireSpindleStarting(level, player, recipeId, recipe, input, result);
    }

    public static void fireSpindleSpinningCompleted(ServerLevel level, Player player,
            ResourceLocation recipeId, SpinningRecipe recipe, ItemStack input, ItemStack result) {
        if (KUBE_JS_LOADED) {
            KubeJSCompat.fireSpindleCompleted(level, player, recipeId, recipe, input, result);
        }
    }

    public static boolean fireBrickMoldingStarting(ServerLevel level, BrickMoldBlockEntity mold,
            ResourceLocation recipeId, BrickMoldingRecipe recipe, ItemStack input, ItemStack result) {
        return KUBE_JS_LOADED && KubeJSCompat.fireBrickMoldingStarting(level, mold, recipeId, recipe, input, result);
    }

    public static void fireBrickMoldingCompleted(ServerLevel level, BrickMoldBlockEntity mold,
            ResourceLocation recipeId, BrickMoldingRecipe recipe, ItemStack input, ItemStack result) {
        if (KUBE_JS_LOADED) {
            KubeJSCompat.fireBrickMoldingCompleted(level, mold, recipeId, recipe, input, result);
        }
    }

    public static boolean fireMortarGrindingStarting(ServerLevel level, MortarBlockEntity mortar,
            ResourceLocation recipeId, MortarGrindingRecipe recipe, ItemStack input, ItemStack result) {
        return KUBE_JS_LOADED && KubeJSCompat.fireMortarGrindingStarting(level, mortar, recipeId, recipe, input, result);
    }

    public static void fireMortarGrindingCompleted(ServerLevel level, MortarBlockEntity mortar,
            ResourceLocation recipeId, MortarGrindingRecipe recipe, ItemStack input, ItemStack result) {
        if (KUBE_JS_LOADED) KubeJSCompat.fireMortarGrindingCompleted(level, mortar, recipeId, recipe, input, result);
    }
    public static boolean fireQuernGrindingStarting(ServerLevel level, QuernBlockEntity quern,
            ResourceLocation recipeId, QuernGrindingRecipe recipe, ItemStack input, ItemStack result) {
        return KUBE_JS_LOADED
                && KubeJSCompat.fireQuernGrindingStarting(level, quern, recipeId, recipe, input, result);
    }

    public static void fireQuernGrindingCompleted(ServerLevel level, QuernBlockEntity quern,
            ResourceLocation recipeId, QuernGrindingRecipe recipe, ItemStack input, ItemStack result) {
        if (KUBE_JS_LOADED) {
            KubeJSCompat.fireQuernGrindingCompleted(level, quern, recipeId, recipe, input, result);
        }
    }

    private OptionalIntegrations() {}
}
