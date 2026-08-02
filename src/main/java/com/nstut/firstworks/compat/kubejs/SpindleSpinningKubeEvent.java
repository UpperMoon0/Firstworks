package com.nstut.firstworks.compat.kubejs;

import com.nstut.firstworks.content.SpinningRecipe;
import dev.latvian.mods.kubejs.event.KubeEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class SpindleSpinningKubeEvent implements KubeEvent {
    private final ServerLevel level;
    private final Player player;
    private final ResourceLocation recipeId;
    private final SpinningRecipe recipe;
    private final ItemStack input;
    private final ItemStack result;

    public SpindleSpinningKubeEvent(ServerLevel level, Player player, ResourceLocation recipeId,
            SpinningRecipe recipe, ItemStack input, ItemStack result) {
        this.level = level;
        this.player = player;
        this.recipeId = recipeId;
        this.recipe = recipe;
        this.input = input.copy();
        this.result = result.copy();
    }

    public ServerLevel getLevel() { return level; }
    public Player getPlayer() { return player; }
    public ResourceLocation getRecipeId() { return recipeId; }
    public SpinningRecipe getRecipe() { return recipe; }
    public ItemStack getInput() { return input.copy(); }
    public ItemStack getResult() { return result.copy(); }
}
