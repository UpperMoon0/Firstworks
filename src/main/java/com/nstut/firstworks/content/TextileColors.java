package com.nstut.firstworks.content;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public final class TextileColors {
    public static Item wool(DyeColor color) {
        return switch (color) {
            case WHITE -> Items.WHITE_WOOL; case ORANGE -> Items.ORANGE_WOOL;
            case MAGENTA -> Items.MAGENTA_WOOL; case LIGHT_BLUE -> Items.LIGHT_BLUE_WOOL;
            case YELLOW -> Items.YELLOW_WOOL; case LIME -> Items.LIME_WOOL;
            case PINK -> Items.PINK_WOOL; case GRAY -> Items.GRAY_WOOL;
            case LIGHT_GRAY -> Items.LIGHT_GRAY_WOOL; case CYAN -> Items.CYAN_WOOL;
            case PURPLE -> Items.PURPLE_WOOL; case BLUE -> Items.BLUE_WOOL;
            case BROWN -> Items.BROWN_WOOL; case GREEN -> Items.GREEN_WOOL;
            case RED -> Items.RED_WOOL; case BLACK -> Items.BLACK_WOOL;
        };
    }

    public static Item bed(DyeColor color) {
        return switch (color) {
            case WHITE -> Items.WHITE_BED; case ORANGE -> Items.ORANGE_BED;
            case MAGENTA -> Items.MAGENTA_BED; case LIGHT_BLUE -> Items.LIGHT_BLUE_BED;
            case YELLOW -> Items.YELLOW_BED; case LIME -> Items.LIME_BED;
            case PINK -> Items.PINK_BED; case GRAY -> Items.GRAY_BED;
            case LIGHT_GRAY -> Items.LIGHT_GRAY_BED; case CYAN -> Items.CYAN_BED;
            case PURPLE -> Items.PURPLE_BED; case BLUE -> Items.BLUE_BED;
            case BROWN -> Items.BROWN_BED; case GREEN -> Items.GREEN_BED;
            case RED -> Items.RED_BED; case BLACK -> Items.BLACK_BED;
        };
    }

    private TextileColors() {}
}
