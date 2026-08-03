package com.nstut.firstworks.compat.jei;

import com.nstut.firstworks.content.brick_mold.BrickMoldingRecipe;
import com.nstut.firstworks.registry.ModBlocks;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.Arrays;

public final class BrickMoldingRecipeCategory implements IRecipeCategory<BrickMoldingRecipe> {
    private final IDrawable icon;
    private final IDrawable arrow;

    public BrickMoldingRecipeCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemLike(ModBlocks.BRICK_MOLD.get());
        this.arrow = guiHelper.getRecipeArrow();
    }

    @Override public mezz.jei.api.recipe.RecipeType<BrickMoldingRecipe> getRecipeType() {
        return FirstworksJeiPlugin.BRICK_MOLDING;
    }
    @Override public Component getTitle() { return Component.translatable("jei.firstworks.brick_molding"); }
    @Override public int getWidth() { return 120; }
    @Override public int getHeight() { return 48; }
    @Override public IDrawable getIcon() { return icon; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, BrickMoldingRecipe recipe, IFocusGroup focuses) {
        var inputs = Arrays.stream(recipe.ingredient().getItems())
                .map(stack -> stack.copyWithCount(recipe.inputCount()))
                .toList();
        builder.addSlot(RecipeIngredientRole.INPUT, 12, 10)
                .setStandardSlotBackground().addItemStacks(inputs);
        builder.addSlot(RecipeIngredientRole.OUTPUT, 90, 10)
                .setStandardSlotBackground().addItemStack(recipe.result());
    }

    @Override
    public void draw(BrickMoldingRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics,
            double mouseX, double mouseY) {
        arrow.draw(graphics, 47, 10);
        graphics.drawString(Minecraft.getInstance().font,
                Component.translatable("jei.firstworks.brick_molding.presses", Math.max(1, recipe.presses())),
                8, 36, 0xFF606060, false);
    }
}
