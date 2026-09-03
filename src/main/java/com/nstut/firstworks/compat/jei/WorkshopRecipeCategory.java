package com.nstut.firstworks.compat.jei;

import com.nstut.firstworks.content.workshop.WorkshopRecipe;
import com.nstut.firstworks.registry.ModItems;
import com.nstut.firstworks.registry.ModTags;
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
import net.minecraft.world.item.crafting.Ingredient;

import java.util.Arrays;
import java.util.List;

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
    @Override public int getWidth() { return 170; }
    @Override public int getHeight() { return 104; }
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

        addProcessRequirements(builder, recipe.station());
    }

    private static void addProcessRequirements(IRecipeLayoutBuilder builder, String station) {
        switch (station) {
            case WorkshopRecipe.STONE_ANVIL -> builder.addSlot(RecipeIngredientRole.CATALYST, 35, 31)
                    .setStandardSlotBackground()
                    .addItemStacks(Arrays.stream(Ingredient.of(ModTags.HAMMERS).getItems()).toList());
            case WorkshopRecipe.KILN -> addFuelSlot(builder, 35);
            case WorkshopRecipe.CRUCIBLE_FURNACE -> {
                addFuelSlot(builder, 35);
                builder.addSlot(RecipeIngredientRole.CATALYST, 63, 31)
                        .setStandardSlotBackground()
                        .addItemStack(new ItemStack(ModItems.BELLOWS.get()));
            }
            default -> {
                // Pottery Wheel work is performed by empty-hand interaction and needs no extra item slot.
            }
        }
    }

    private static void addFuelSlot(IRecipeLayoutBuilder builder, int x) {
        builder.addSlot(RecipeIngredientRole.CATALYST, x, 31)
                .setStandardSlotBackground()
                .addItemStacks(List.of(new ItemStack(Items.COAL), new ItemStack(Items.CHARCOAL)));
    }

    @Override
    public void draw(WorkshopRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        arrow.draw(graphics, 91, 5);
        var font = Minecraft.getInstance().font;
        graphics.drawString(font,
                Component.translatable("jei.firstworks.workshop.station", stationName(recipe.station())),
                3, 55, 0xFF606060, false);
        boolean heated = WorkshopRecipe.KILN.equals(recipe.station())
                || WorkshopRecipe.CRUCIBLE_FURNACE.equals(recipe.station());
        graphics.drawString(font,
                Component.translatable(heated
                                ? "jei.firstworks.workshop.processing_ticks"
                                : "jei.firstworks.workshop.manual_actions",
                        recipe.work()),
                3, 67, 0xFF606060, false);

        int detailsY = 79;
        if (WorkshopRecipe.POTTERY_WHEEL.equals(recipe.station()) && recipe.inputCount() <= 3) {
            graphics.drawString(font, Component.translatable("jei.firstworks.workshop.pottery_batch"),
                    3, detailsY, 0xFF606060, false);
            detailsY += 12;
        }
        if (WorkshopRecipe.CRUCIBLE_FURNACE.equals(recipe.station())) {
            graphics.drawString(font, Component.translatable("jei.firstworks.workshop.air"),
                    3, detailsY, 0xFF606060, false);
            detailsY += 12;
        }
        if (recipe.hasCatalyst()) {
            graphics.drawString(font,
                    Component.translatable(recipe.consumeCatalyst()
                            ? "jei.firstworks.workshop.catalyst_consumed"
                            : "jei.firstworks.workshop.catalyst_reusable"),
                    3, detailsY, 0xFF606060, false);
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
