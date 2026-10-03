package com.nstut.firstworks.compat.jei;

import com.nstut.firstworks.content.MortarGrindingRecipe;
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

public final class MortarGrindingRecipeCategory implements IRecipeCategory<MortarGrindingRecipe> {
    private final IDrawable icon;
    private final IDrawable arrow;

    public MortarGrindingRecipeCategory(IGuiHelper guiHelper) {
        icon = guiHelper.createDrawableItemLike(ModItems.MORTAR_AND_PESTLE.get());
        arrow = guiHelper.getRecipeArrow();
    }
    @Override public mezz.jei.api.recipe.RecipeType<MortarGrindingRecipe> getRecipeType() {
        return FirstworksJeiPlugin.MORTAR_GRINDING;
    }
    @Override public Component getTitle() { return Component.translatable("jei.firstworks.mortar_grinding"); }
    @Override public int getWidth() { return 190; }
    @Override public int getHeight() { return 118; }
    @Override public IDrawable getIcon() { return icon; }
    @Override public void setRecipe(IRecipeLayoutBuilder builder, MortarGrindingRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.CATALYST, 4, 5).setStandardSlotBackground()
                .addItemLike(ModItems.MORTAR_AND_PESTLE.get());
        var inputs = Arrays.stream(recipe.ingredient().getItems())
                .map(stack -> stack.copyWithCount(recipe.inputCount())).toList();
        builder.addSlot(RecipeIngredientRole.INPUT, 27, 5).setStandardSlotBackground().addItemStacks(inputs);
        builder.addSlot(RecipeIngredientRole.OUTPUT, 108, 5).setStandardSlotBackground().addItemStack(recipe.result());
    }
    @Override public void draw(MortarGrindingRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics,
            double mouseX, double mouseY) {
        arrow.draw(graphics, 66, 5);
        var font = Minecraft.getInstance().font;
        int y = 30;
        for (var stage : recipe.stages()) {
            if (y >= 70) break;
            graphics.drawString(font, stageDescription(stage), 4, y, 0xFF606060, false);
            y += 10;
        }
        graphics.drawWordWrap(font, Component.translatable("hint.firstworks.mortar.controls"), 4, y + 4, 182, 0xFF606060);
    }

    private static Component stageDescription(com.nstut.firstworks.content.mortar.MortarStage stage) {
        return stage.action().equals("crush")
                ? Component.translatable("hint.firstworks.mortar.recipe_crush", stage.count())
                : Component.translatable("hint.firstworks.mortar.recipe_grind", String.format(java.util.Locale.ROOT, "%.2f", stage.duration() / 20.0F));
    }

    @Override public void getTooltip(mezz.jei.api.gui.builder.ITooltipBuilder tooltip, MortarGrindingRecipe recipe,
            IRecipeSlotsView slots, double mouseX, double mouseY) {
        if (mouseY < 30) return;
        for (var stage : recipe.stages()) tooltip.add(stageDescription(stage));
    }
}
