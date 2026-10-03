package com.nstut.firstworks.compat.jei;

import com.nstut.firstworks.registry.ModItems;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** A heating use, with no recipe output: the loaded stack itself becomes hot. */
public final class KilnHeatingCategory implements IRecipeCategory<ItemStack> {
    private final IDrawable icon;
    public KilnHeatingCategory(IGuiHelper helper) { icon = helper.createDrawableItemLike(ModItems.KILN.get()); }
    @Override public RecipeType<ItemStack> getRecipeType() { return WorkshopJeiPlugin.KILN_HEATING; }
    @Override public Component getTitle() { return Component.translatable("block.firstworks.kiln"); }
    @Override public IDrawable getIcon() { return icon; }
    @Override public int getWidth() { return 170; }
    @Override public int getHeight() { return 96; }
    @Override public void setRecipe(IRecipeLayoutBuilder builder, ItemStack item, IFocusGroup focus) {
        builder.addSlot(RecipeIngredientRole.INPUT, 35, 5).setStandardSlotBackground().addItemStack(item);
        builder.addSlot(RecipeIngredientRole.CATALYST, 3, 5).setStandardSlotBackground().addItemLike(ModItems.KILN.get());
        builder.addSlot(RecipeIngredientRole.CATALYST, 63, 5).setStandardSlotBackground()
                .addItemStacks(WorkshopRecipeCategory.furnaceFuels());
        builder.addSlot(RecipeIngredientRole.CATALYST, 91, 5).setStandardSlotBackground()
                .addItemLike(ModItems.FIRE_STARTER.get()).addItemLike(Items.FLINT_AND_STEEL).addItemLike(Items.FIRE_CHARGE);
    }
    @Override public void draw(ItemStack item, IRecipeSlotsView slots, GuiGraphics graphics, double x, double y) {
        graphics.drawWordWrap(Minecraft.getInstance().font, Component.translatable("jei.firstworks.kiln.heating"), 3, 32, getWidth() - 6, 0xFF606060);
    }
}
