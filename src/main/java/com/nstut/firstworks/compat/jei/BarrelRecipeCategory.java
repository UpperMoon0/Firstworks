package com.nstut.firstworks.compat.jei;

import com.nstut.firstworks.content.barrel.BarrelRecipe;
import com.nstut.firstworks.registry.ModBlocks;
import com.nstut.firstworks.registry.ModItems;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.material.Fluid;

public final class BarrelRecipeCategory implements IRecipeCategory<BarrelRecipe> {
    private final IDrawable icon;
    private final IDrawable arrow;

    public BarrelRecipeCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemLike(ModBlocks.BARREL.get());
        this.arrow = guiHelper.getRecipeArrow();
    }

    @Override
    public mezz.jei.api.recipe.RecipeType<BarrelRecipe> getRecipeType() {
        return FirstworksJeiPlugin.BARREL_PROCESSING;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.firstworks.barrel_processing");
    }

    @Override public int getWidth() { return 132; }
    @Override public int getHeight() { return 58; }
    @Override public IDrawable getIcon() { return icon; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, BarrelRecipe recipe, IFocusGroup focuses) {
        boolean washingFleece = recipe.ingredient().test(new net.minecraft.world.item.ItemStack(ModItems.RAW_FLEECE.get()))
                && recipe.result().is(ModItems.CLEAN_WOOL.get());
        boolean isTreeBark = recipe.ingredient().test(new net.minecraft.world.item.ItemStack(ModItems.TREE_BARK.get()));
        IRecipeSlotBuilder inputItem = builder.addSlot(RecipeIngredientRole.INPUT, 4, 5)
                .setStandardSlotBackground();
        if (washingFleece) {
            inputItem.addItemStacks(FirstworksJeiPlugin.fleeceVariants(ModItems.RAW_FLEECE.get(), recipe.inputCount()));
        } else if (isTreeBark) {
            inputItem.addItemStacks(FirstworksJeiPlugin.treeBarkVariants(ModItems.TREE_BARK.get(), recipe.inputCount()));
        } else {
            inputItem.addIngredients(recipe.ingredient());
        }

        IRecipeSlotBuilder fluidSlot = builder.addSlot(RecipeIngredientRole.INPUT, 27, 5)
                .setStandardSlotBackground()
                .setFluidRenderer(Math.max(1000, recipe.fluidAmount()), true, 16, 16);

        recipe.fluid().target().ifLeft(loc -> {
            Fluid inputFluid = BuiltInRegistries.FLUID.get(loc);
            fluidSlot.addFluidStack(inputFluid, recipe.fluidAmount());
        }).ifRight(tag -> {
            var tagLookup = BuiltInRegistries.FLUID.getTag(tag);
            if (tagLookup.isPresent()) {
                tagLookup.get().stream()
                        .forEach(holder -> fluidSlot.addFluidStack(holder.value(), recipe.fluidAmount()));
            }
        });

        boolean hasOutputFluid = !recipe.outputFluid().equals(BarrelRecipe.NO_FLUID)
                && recipe.outputFluidAmount() > 0;
        if (hasOutputFluid) {
            Fluid outputFluid = BuiltInRegistries.FLUID.get(recipe.outputFluid());
            builder.addSlot(RecipeIngredientRole.OUTPUT, 85, 5)
                    .setStandardSlotBackground()
                    .setFluidRenderer(Math.max(1000, recipe.outputFluidAmount()), true, 16, 16)
                    .addFluidStack(outputFluid, recipe.outputFluidAmount());
        }
        if (!recipe.result().isEmpty()) {
            IRecipeSlotBuilder outputItem = builder.addSlot(RecipeIngredientRole.OUTPUT, hasOutputFluid ? 108 : 85, 5)
                    .setStandardSlotBackground();
            if (washingFleece) {
                outputItem.addItemStacks(FirstworksJeiPlugin.fleeceVariants(
                        ModItems.CLEAN_WOOL.get(), recipe.result().getCount()));
                builder.createFocusLink(inputItem, outputItem);
            } else {
                outputItem.addItemStack(recipe.result());
            }
        }
    }

    @Override
    public void draw(BarrelRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics,
            double mouseX, double mouseY) {
        arrow.draw(graphics, 55, 5);
        Component time = Component.translatable("jei.firstworks.duration", recipe.duration() / 20);
        graphics.drawString(Minecraft.getInstance().font, time, 4, 38, 0xFF606060, false);
    }
}
