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
import net.minecraft.world.item.crafting.Ingredient;

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
    @Override public int getWidth() { return 170; }
    @Override public int getHeight() { return WorkshopRecipe.STONE_ANVIL.equals(station) ? 94 : WorkshopRecipe.CRUCIBLE_FURNACE.equals(station) ? 110 : 208; }
    @Override public IDrawable getIcon() { return icon; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, WorkshopRecipe recipe, IFocusGroup focuses) {
        boolean crucible = WorkshopRecipe.CRUCIBLE_FURNACE.equals(recipe.station());
        if (!crucible) builder.addSlot(RecipeIngredientRole.CATALYST, 3, 5)
                .setStandardSlotBackground()
                .addItemStack(stationStack(recipe.station()));
        builder.addSlot(RecipeIngredientRole.INPUT, 35, 5)
                .setStandardSlotBackground()
                .addItemStacks(Arrays.stream(recipe.ingredient().getItems())
                        .map(stack -> stack.copyWithCount(recipe.inputCount()))
                        .toList());
        recipe.catalyst().ifPresent(catalyst -> {
            var slot = builder.addSlot(recipe.consumeCatalyst() ? RecipeIngredientRole.INPUT : RecipeIngredientRole.CATALYST,
                            crucible ? 75 : 63, 5)
                    .setStandardSlotBackground()
                    .addItemStacks(Arrays.stream(catalyst.getItems())
                            .map(stack -> stack.copyWithCount(recipe.catalystCount())).toList());
            if (!recipe.consumeCatalyst()) markReusable(slot);
        });
        builder.addSlot(RecipeIngredientRole.OUTPUT, crucible ? 135 : 122, 5)
                .setStandardSlotBackground()
                .addItemStack(recipe.result());

        addProcessRequirements(builder, recipe.station());
    }

    private static void addProcessRequirements(IRecipeLayoutBuilder builder, String station) {
        switch (station) {
            case WorkshopRecipe.STONE_ANVIL -> builder.addSlot(RecipeIngredientRole.CATALYST, 35, 31)
                    .setStandardSlotBackground()
                    .addItemStacks(Arrays.stream(Ingredient.of(ModTags.HAMMERS).getItems()).toList());
            case WorkshopRecipe.CRUCIBLE_FURNACE -> {
                builder.addSlot(RecipeIngredientRole.INPUT, 35, 45).setStandardSlotBackground()
                        .setSlotName("fuel")
                        .addItemStacks(furnaceFuels())
                        .addRichTooltipCallback((slot, lines) -> lines.add(Component.translatable("jei.firstworks.workshop.fuel")));
                builder.addSlot(RecipeIngredientRole.CATALYST, 75, 45)
                        .setSlotName("bellows")
                        .setStandardSlotBackground()
                        .addItemStack(new ItemStack(ModItems.BELLOWS.get()))
                        .addRichTooltipCallback((slot, lines) -> lines.add(Component.translatable("jei.firstworks.workshop.bellows")));
                addIgniterSlot(builder, 135, 45);
            }
            default -> {
                // Pottery Wheel work is performed by empty-hand interaction and needs no extra item slot.
            }
        }
    }

    static void addIgniterSlot(IRecipeLayoutBuilder builder, int x, int y) {
        builder.addSlot(RecipeIngredientRole.CATALYST, x, y).setStandardSlotBackground().setSlotName("igniter")
                .addItemStacks(net.minecraft.core.registries.BuiltInRegistries.ITEM.stream().map(ItemStack::new)
                        .filter(com.nstut.firstworks.content.workshop.WorkshopIgnition::isIgniter).toList())
                .addRichTooltipCallback((slot, lines) -> lines.add(Component.translatable("jei.firstworks.workshop.igniter")));
    }

    static void drawSlotLabel(GuiGraphics graphics, String role, int centerX, int y) {
        var font = Minecraft.getInstance().font;
        var label = Component.translatable("jei.firstworks.workshop.label." + role);
        graphics.drawString(font, label, centerX - font.width(label) / 2, y, 0xFF606060, false);
    }

    static void drawFuelFlame(GuiGraphics graphics) {
        graphics.blitSprite(net.minecraft.resources.ResourceLocation.withDefaultNamespace("container/furnace/lit_progress"), 36, 26, 14, 14);
    }

    static void addProcessDisplay(mezz.jei.api.gui.widgets.IRecipeExtrasBuilder builder,
            java.util.function.Consumer<GuiGraphics> display) {
        // JEI draws extras after item slots. Flush at the boundaries so slot item rendering
        // cannot reorder the process labels and sprites against the recipe background.
        builder.addDrawable(new IDrawable() {
            @Override public int getWidth() { return 170; }
            @Override public int getHeight() { return 110; }
            @Override public void draw(GuiGraphics graphics, int x, int y) {
                graphics.flush();
                graphics.pose().pushPose();
                graphics.pose().translate(x, y, 0);
                display.accept(graphics);
                graphics.flush();
                graphics.pose().popPose();
            }
        }, 0, 0);
    }

    @Override
    public void createRecipeExtras(mezz.jei.api.gui.widgets.IRecipeExtrasBuilder builder,
            WorkshopRecipe recipe, IRecipeSlotsView slots, IFocusGroup focuses) {
        if (WorkshopRecipe.CRUCIBLE_FURNACE.equals(recipe.station())) {
            addProcessDisplay(builder, graphics -> drawProcess(recipe, graphics));
        }
    }

    private static void markReusable(mezz.jei.api.gui.builder.IRecipeSlotBuilder slot) {
        slot.setOverlay(new IDrawable() {
            @Override public int getWidth() { return 16; }
            @Override public int getHeight() { return 16; }
            @Override public void draw(GuiGraphics graphics, int x, int y) {
                graphics.pose().pushPose();
                graphics.pose().translate(x + 10, y - 2, 200);
                graphics.pose().scale(0.5F, 0.5F, 1);
                graphics.drawString(Minecraft.getInstance().font, "NC", 0, 0, 0xFFFFFFFF, true);
                graphics.pose().popPose();
            }
        }, 0, 0).addRichTooltipCallback((view, lines) ->
                lines.add(Component.translatable("jei.firstworks.workshop.not_consumed")));
    }

    static java.util.List<ItemStack> furnaceFuels() {
        return net.minecraft.core.registries.BuiltInRegistries.ITEM.stream()
                .map(ItemStack::new)
                .filter(stack -> stack.getBurnTime(net.minecraft.world.item.crafting.RecipeType.SMELTING) > 0)
                .toList();
    }

    @Override
    public void draw(WorkshopRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        if (!WorkshopRecipe.CRUCIBLE_FURNACE.equals(recipe.station())) drawProcess(recipe, graphics);
    }

    private void drawProcess(WorkshopRecipe recipe, GuiGraphics graphics) {
        boolean crucible = WorkshopRecipe.CRUCIBLE_FURNACE.equals(recipe.station());
        arrow.draw(graphics, crucible ? 105 : 91, 5);
        if (crucible) drawFuelFlame(graphics);
        var font = Minecraft.getInstance().font;
        if (crucible) {
            drawSlotLabel(graphics, "fuel", 43, 65);
            drawSlotLabel(graphics, "bellows", 83, 65);
            drawSlotLabel(graphics, "igniter", 143, 65);
        }
        if (WorkshopRecipe.STONE_ANVIL.equals(recipe.station())) {
            recipe.forge().ifPresent(forge -> {
                var sequence = Component.empty();
                for (String action : forge.actions()) {
                    if (!sequence.getSiblings().isEmpty()) sequence.append(" > ");
                    sequence.append(Component.translatable("action.firstworks." + action));
                }
                var lines = font.split(Component.translatable("hint.firstworks.anvil.sequence", sequence), 164);
                for (int i = 0; i < Math.min(3, lines.size()); i++) {
                    graphics.drawString(font, lines.get(i), 3, 55 + i * 10, 0xFF606060, false);
                }
                if (forge.heatTicks() > 0) graphics.drawString(font, Component.translatable("jei.firstworks.workshop.temperature",
                        com.nstut.firstworks.FirstworksClientConfig.HEAT_UNIT.get().format(forge.minimumTemperature())),
                        3, 85, 0xFF606060, false);
            });
            return;
        }
        if (!crucible) graphics.drawString(font,
                Component.translatable("jei.firstworks.workshop.station", stationName(recipe.station())),
                3, 55, 0xFF606060, false);
        boolean heated = WorkshopRecipe.CRUCIBLE_FURNACE.equals(recipe.station());
        graphics.drawString(font,
                Component.translatable(heated
                                ? "jei.firstworks.workshop.processing_seconds"
                                : "jei.firstworks.workshop.manual_actions",
                        heated ? seconds(recipe.requiredWork()) : recipe.requiredWork()),
                3, crucible ? 81 : 67, 0xFF606060, false);

        int detailsY = crucible ? 93 : 79;
        if (WorkshopRecipe.POTTERY_WHEEL.equals(recipe.station()) && recipe.inputCount() <= 3) {
            graphics.drawString(font, Component.translatable("jei.firstworks.workshop.pottery_batch"),
                    3, detailsY, 0xFF606060, false);
            detailsY += 12;
        }
        if (WorkshopRecipe.CRUCIBLE_FURNACE.equals(recipe.station())) {
            graphics.drawString(font, Component.translatable("jei.firstworks.workshop.temperature",
                            com.nstut.firstworks.FirstworksClientConfig.HEAT_UNIT.get().format(recipe.requiredTemperature())),
                    3, detailsY, 0xFF606060, false);
            detailsY += 12;
        }
    }

    public static String seconds(int ticks) {
        return ticks % 20 == 0 ? Integer.toString(ticks / 20) : String.format(java.util.Locale.ROOT, "%.2f", ticks / 20.0);
    }

    private static ItemStack stationStack(String station) {
        return switch (station) {
            case WorkshopRecipe.POTTERY_WHEEL -> new ItemStack(ModItems.POTTERY_WHEEL.get());
            case WorkshopRecipe.STONE_ANVIL -> new ItemStack(ModItems.STONE_ANVIL.get());
            case WorkshopRecipe.CRUCIBLE_FURNACE -> new ItemStack(ModItems.CRUCIBLE_FURNACE.get());
            default -> ItemStack.EMPTY;
        };
    }

    @Override public void getTooltip(mezz.jei.api.gui.builder.ITooltipBuilder tooltip, WorkshopRecipe recipe,
            IRecipeSlotsView slots, double mouseX, double mouseY) {
        if (mouseY < 55) return;
        recipe.forge().ifPresent(forge -> {
            for (int i = 0; i < forge.actions().size(); i += 4) {
                var line = Component.empty();
                for (int j = i; j < Math.min(i + 4, forge.actions().size()); j++) {
                    if (j > i) line.append(" > ");
                    line.append(Component.literal((j + 1) + ". ").append(Component.translatable("action.firstworks." + forge.actions().get(j))));
                }
                tooltip.add(line);
            }
        });
    }

    private static Component stationName(String station) {
        return switch (station) {
            case WorkshopRecipe.POTTERY_WHEEL -> Component.translatable("block.firstworks.pottery_wheel");
            case WorkshopRecipe.STONE_ANVIL -> Component.translatable("block.firstworks.stone_anvil");
            case WorkshopRecipe.CRUCIBLE_FURNACE -> Component.translatable("block.firstworks.crucible_furnace");
            default -> Component.literal(station);
        };
    }
}
