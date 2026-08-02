package com.nstut.firstworks.compat.kubejs;

import com.nstut.firstworks.content.barrel.BarrelRecipe;
import com.nstut.firstworks.content.barrel.BarrelBlockEntity;
import com.nstut.firstworks.content.loom.LoomBlockEntity;
import com.nstut.firstworks.content.loom.LoomRecipe;
import com.nstut.firstworks.content.SpinningRecipe;
import dev.latvian.mods.kubejs.script.ScriptType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
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

    public static boolean fireLoomStarting(ServerLevel level, LoomBlockEntity loom, ResourceLocation recipeId,
            LoomRecipe recipe, ItemStack input, ItemStack result) {
        return FirstworksKubeEvents.LOOM_WEAVING_STARTING.post(ScriptType.SERVER,
                new LoomWeavingKubeEvent(level, loom, recipeId, recipe, input, result)).interruptFalse();
    }

    public static void fireLoomCompleted(ServerLevel level, LoomBlockEntity loom, ResourceLocation recipeId,
            LoomRecipe recipe, ItemStack input, ItemStack result) {
        FirstworksKubeEvents.LOOM_WEAVING_COMPLETED.post(ScriptType.SERVER,
                new LoomWeavingKubeEvent(level, loom, recipeId, recipe, input, result));
    }

    public static boolean fireSpindleStarting(ServerLevel level, Player player, ResourceLocation recipeId,
            SpinningRecipe recipe, ItemStack input, ItemStack result) {
        return FirstworksKubeEvents.SPINDLE_SPINNING_STARTING.post(ScriptType.SERVER,
                new SpindleSpinningKubeEvent(level, player, recipeId, recipe, input, result)).interruptFalse();
    }

    public static void fireSpindleCompleted(ServerLevel level, Player player, ResourceLocation recipeId,
            SpinningRecipe recipe, ItemStack input, ItemStack result) {
        FirstworksKubeEvents.SPINDLE_SPINNING_COMPLETED.post(ScriptType.SERVER,
                new SpindleSpinningKubeEvent(level, player, recipeId, recipe, input, result));
    }

    private KubeJSCompat() {}
}
