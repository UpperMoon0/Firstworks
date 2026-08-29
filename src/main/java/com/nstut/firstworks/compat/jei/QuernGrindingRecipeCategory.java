package com.nstut.firstworks.compat.jei;

import com.nstut.firstworks.content.quern.QuernGrindingRecipe;
import com.nstut.firstworks.registry.ModItems;
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
import java.util.Locale;

public final class QuernGrindingRecipeCategory implements IRecipeCategory<QuernGrindingRecipe> {
    private final IDrawable icon;
    private final IDrawable arrow;

    public QuernGrindingRecipeCategory(IGuiHelper guiHelper) {
        icon = guiHelper.createDrawableItemLike(ModItems.QUERN.get());
        arrow = guiHelper.getRecipeArrow();
    }

    @Override
    public mezz.jei.api.recipe.RecipeType<QuernGrindingRecipe> getRecipeType() {
        return FirstworksJeiPlugin.QUERN_GRINDING;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.firstworks.quern_grinding");
    }

    @Override
    public int getWidth() {
        return 150;
    }

    @Override
    public int getHeight() {
        return 50;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, QuernGrindingRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.CATALYST, 3, 5)
                .setStandardSlotBackground()
                .addItemLike(ModItems.QUERN.get());
        builder.addSlot(RecipeIngredientRole.INPUT, 35, 5)
                .setStandardSlotBackground()
                .addItemStacks(Arrays.stream(recipe.ingredient().getItems())
                        .map(stack -> stack.copyWithCount(recipe.inputCount()))
                        .toList());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 112, 5)
                .setStandardSlotBackground()
                .addItemStack(recipe.result());
    }

    @Override
    public void draw(QuernGrindingRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics,
            double mouseX, double mouseY) {
        arrow.draw(graphics, 73, 5);
        var font = Minecraft.getInstance().font;
        graphics.drawString(font,
                Component.translatable("jei.firstworks.quern.work", recipe.work()),
                3, 34, 0xFF606060, false);
    }
}
