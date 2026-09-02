package com.nstut.firstworks.compat.jei;

import com.nstut.firstworks.content.workshop.WorkshopRecipe;
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

import java.util.Arrays;

public final class WorkshopRecipeCategory implements IRecipeCategory<WorkshopRecipe> {
    private final RecipeType<WorkshopRecipe> recipeType;
    private final String station;
    private final IDrawable icon;
    private final IDrawable arrow;

    public WorkshopRecipeCategory(IGuiHelper guiHelper, RecipeType<WorkshopRecipe> recipeType, String station) {
        this.recipeType = recipeType;
        this.station = station;
        icon = guiHelper.createDrawableItemLike(stationStack(station).getItem());
        arrow = guiHelper.getRecipeArrow();
    }

    @Override public RecipeType<WorkshopRecipe> getRecipeType() { return recipeType; }
    @Override public Component getTitle() { return stationName(station); }
    @Override public int getWidth() { return 160; }
    @Override public int getHeight() { return 62; }
    @Override public IDrawable getIcon() { return icon; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, WorkshopRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.CATALYST, 3, 5)
                .setStandardSlotBackground()
                .addItemStack(stationStack(recipe.station()));
        builder.addSlot(RecipeIngredientRole.INPUT, 35, 5)
                .setStandardSlotBackground()
                .addItemStacks(Arrays.stream(recipe.ingredient().getItems())
                        .map(stack -> stack.copyWithCount(recipe.inputCount()))
                        .toList());
        recipe.catalyst().ifPresent(catalyst ->
                builder.addSlot(RecipeIngredientRole.CATALYST, 63, 5)
                        .setStandardSlotBackground()
                        .addItemStacks(Arrays.stream(catalyst.getItems())
                                .map(stack -> stack.copyWithCount(recipe.catalystCount()))
                                .toList()));
        builder.addSlot(RecipeIngredientRole.OUTPUT, 122, 5)
                .setStandardSlotBackground()
                .addItemStack(recipe.result());
    }

    @Override
    public void draw(WorkshopRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        arrow.draw(graphics, 91, 5);
        var font = Minecraft.getInstance().font;
        graphics.drawString(font,
                Component.translatable("jei.firstworks.workshop.station", stationName(recipe.station())),
                3, 34, 0xFF606060, false);
        graphics.drawString(font,
                Component.translatable("jei.firstworks.workshop.work", recipe.work()),
                3, 46, 0xFF606060, false);
        if (WorkshopRecipe.CRUCIBLE_FURNACE.equals(recipe.station())) {
            graphics.drawString(font, Component.translatable("jei.firstworks.workshop.air"),
                    83, 46, 0xFF606060, false);
        }
    }

    private static ItemStack stationStack(String station) {
        return switch (station) {
            case WorkshopRecipe.POTTERY_WHEEL -> new ItemStack(ModItems.POTTERY_WHEEL.get());
            case WorkshopRecipe.KILN -> new ItemStack(ModItems.KILN.get());
            case WorkshopRecipe.STONE_ANVIL -> new ItemStack(ModItems.STONE_ANVIL.get());
            case WorkshopRecipe.CRUCIBLE_FURNACE -> new ItemStack(ModItems.CRUCIBLE_FURNACE.get());
            default -> ItemStack.EMPTY;
        };
    }

    private static Component stationName(String station) {
        return switch (station) {
            case WorkshopRecipe.POTTERY_WHEEL -> Component.translatable("block.firstworks.pottery_wheel");
            case WorkshopRecipe.KILN -> Component.translatable("block.firstworks.kiln");
            case WorkshopRecipe.STONE_ANVIL -> Component.translatable("block.firstworks.stone_anvil");
            case WorkshopRecipe.CRUCIBLE_FURNACE -> Component.translatable("block.firstworks.crucible_furnace");
            default -> Component.literal(station);
        };
    }
}
