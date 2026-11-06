package com.nstut.firstworks.compat.jei;

import com.nstut.firstworks.content.workshop.CoolingRecipe;
import com.nstut.firstworks.FirstworksClientConfig;
import com.nstut.firstworks.registry.ModItems;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.*;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import java.util.Arrays;

public final class CoolingRecipeCategory implements IRecipeCategory<CoolingRecipe> {
    private final IDrawable icon;
    public CoolingRecipeCategory(IGuiHelper helper) { icon=helper.createDrawableItemLike(ModItems.PRIMITIVE_TONGS.get()); }
    @Override public RecipeType<CoolingRecipe> getRecipeType() { return WorkshopJeiPlugin.COOLING; }
    @Override public Component getTitle() { return Component.translatable("jei.firstworks.cooling"); }
    @Override public IDrawable getIcon() { return icon; }
    @Override public int getWidth() { return 180; }
    @Override public int getHeight() { return 80; }
    @Override public void setRecipe(IRecipeLayoutBuilder builder,CoolingRecipe recipe,IFocusGroup focus) {
        builder.addSlot(RecipeIngredientRole.INPUT,10,4).setStandardSlotBackground()
                .addItemStacks(Arrays.stream(recipe.ingredient().getItems()).map(s->s.copyWithCount(recipe.count())).toList());
        recipe.result().ifPresent(s->builder.addSlot(RecipeIngredientRole.OUTPUT,150,4).setStandardSlotBackground().addItemStack(s));
    }
    @Override public void createRecipeExtras(mezz.jei.api.gui.widgets.IRecipeExtrasBuilder builder,CoolingRecipe recipe,IRecipeSlotsView slots,IFocusGroup focus) {
        WorkshopRecipeCategory.addProcessDisplay(builder,graphics->drawInfo(recipe,graphics));
    }
    private void drawInfo(CoolingRecipe recipe,GuiGraphics graphics) {
        var font=Minecraft.getInstance().font;var unit=FirstworksClientConfig.HEAT_UNIT.get();
        graphics.drawString(font,recipe.medium(),10,30,0xFF606060,false);
        graphics.drawString(font,Component.translatable("jei.firstworks.cooling.entry",unit.format(recipe.minimumEntry())),10,44,0xFF606060,false);
        graphics.drawString(font,Component.translatable("jei.firstworks.cooling.finish",unit.format(recipe.completion())),10,58,0xFF606060,false);
    }
}
