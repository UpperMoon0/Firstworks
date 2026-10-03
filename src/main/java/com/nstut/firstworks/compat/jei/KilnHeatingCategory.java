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

/** A heating use, with no recipe output: the loaded stack itself becomes hot. */
public final class KilnHeatingCategory implements IRecipeCategory<ItemStack> {
    private final IDrawable icon;
    public KilnHeatingCategory(IGuiHelper helper) { icon = helper.createDrawableItemLike(ModItems.KILN.get()); }
    @Override public RecipeType<ItemStack> getRecipeType() { return WorkshopJeiPlugin.KILN_HEATING; }
    @Override public Component getTitle() { return Component.translatable("block.firstworks.kiln"); }
    @Override public IDrawable getIcon() { return icon; }
    @Override public int getWidth() { return 170; }
    @Override public int getHeight() { return 110; }
    @Override public void setRecipe(IRecipeLayoutBuilder builder, ItemStack item, IFocusGroup focus) {
        builder.addSlot(RecipeIngredientRole.INPUT, 35, 5).setStandardSlotBackground().addItemStack(item.copyWithCount(1));
        builder.addSlot(RecipeIngredientRole.INPUT, 35, 45).setStandardSlotBackground()
                .setSlotName("fuel")
                .addItemStacks(WorkshopRecipeCategory.furnaceFuels())
                .addRichTooltipCallback((slot, lines) -> lines.add(Component.translatable("jei.firstworks.workshop.fuel")));
        WorkshopRecipeCategory.addIgniterSlot(builder, 91, 45);
    }
    @Override public void draw(ItemStack item, IRecipeSlotsView slots, GuiGraphics graphics, double x, double y) {
        graphics.blitSprite(net.minecraft.resources.ResourceLocation.withDefaultNamespace("container/furnace/lit_progress"), 36, 26, 14, 14);
        var unit = com.nstut.firstworks.FirstworksClientConfig.HEAT_UNIT.get();
        var font = Minecraft.getInstance().font;
        WorkshopRecipeCategory.drawSlotLabel(graphics, "fuel", 43, 65);
        WorkshopRecipeCategory.drawSlotLabel(graphics, "igniter", 99, 65);
        graphics.drawString(font, Component.translatable("jei.firstworks.kiln.maximum",
                unit.format(com.nstut.firstworks.content.workshop.HeatTemperature.maximum(item))), 3, 81, 0xFF606060, false);
        graphics.drawString(font, Component.translatable("jei.firstworks.kiln.heat_time",
                unit.format(com.nstut.firstworks.content.workshop.ThermalModel.AMBIENT),
                WorkshopRecipeCategory.seconds(com.nstut.firstworks.FirstworksConfig.KILN_HEATING_TICKS.get())), 3, 93, 0xFF606060, false);
    }
}
