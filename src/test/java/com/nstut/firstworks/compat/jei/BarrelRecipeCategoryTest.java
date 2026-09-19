package com.nstut.firstworks.compat.jei;

import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BarrelRecipeCategoryTest {
    @Test
    void countedIngredientStacksApplyInputCountToEveryAlternative() {
        var stacks = BarrelRecipeCategory.countedIngredientStacks(
                Ingredient.of(Items.OAK_LOG, Items.BIRCH_LOG), 7);

        assertEquals(2, stacks.size());
        assertTrue(stacks.stream().allMatch(stack -> stack.getCount() == 7));
        assertTrue(stacks.stream().anyMatch(stack -> stack.is(Items.OAK_LOG)));
        assertTrue(stacks.stream().anyMatch(stack -> stack.is(Items.BIRCH_LOG)));
    }
}
