package com.nstut.firstworks.compat.jei;

import com.nstut.firstworks.content.loom.LoomRecipe;
import com.nstut.firstworks.registry.ModBlocks;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.Arrays;

public final class LoomRecipeCategory implements IRecipeCategory<LoomRecipe> {
    private final IDrawable icon;
    private final IDrawable arrow;

    public LoomRecipeCategory(IGuiHelper guiHelper) {
        icon = guiHelper.createDrawableItemLike(ModBlocks.LOOM.get());
        arrow = guiHelper.getRecipeArrow();
    }

    @Override public mezz.jei.api.recipe.RecipeType<LoomRecipe> getRecipeType() {
        return FirstworksJeiPlugin.LOOM_WEAVING;
    }
    @Override public Component getTitle() { return Component.translatable("jei.firstworks.loom_weaving"); }
    @Override public int getWidth() { return 112; }
    @Override public int getHeight() { return 48; }
    @Override public IDrawable getIcon() { return icon; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, LoomRecipe recipe, IFocusGroup focuses) {
        var inputs = Arrays.stream(recipe.ingredient().getItems())
                .map(stack -> stack.copyWithCount(recipe.inputCount()))
                .toList();
        builder.addSlot(RecipeIngredientRole.INPUT, 8, 5)
                .setStandardSlotBackground().addItemStacks(inputs);
        builder.addSlot(RecipeIngredientRole.OUTPUT, 86, 5)
                .setStandardSlotBackground().addItemStack(recipe.result());
    }

    @Override
    public void draw(LoomRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics,
            double mouseX, double mouseY) {
        arrow.draw(graphics, 43, 5);
        graphics.drawString(Minecraft.getInstance().font,
                Component.translatable("jei.firstworks.strokes", recipe.strokes()), 8, 34, 0xFF606060, false);
    }
}
