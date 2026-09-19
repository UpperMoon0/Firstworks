package com.nstut.firstworks.gametest;

import com.nstut.firstworks.Firstworks;
import com.nstut.firstworks.compat.jei.JeiIngredientStacks;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Firstworks.MOD_ID)
@PrefixGameTestTemplate(false)
public final class JeiIngredientGameTests {
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void tagAlternativesKeepConfiguredInputCount(GameTestHelper helper) {
        var stacks = JeiIngredientStacks.withCount(Ingredient.of(ItemTags.LOGS), 7);

        helper.assertTrue(!stacks.isEmpty(), "Vanilla log tag resolved to no JEI alternatives");
        helper.assertTrue(stacks.stream().allMatch(stack -> stack.getCount() == 7),
                "A tag alternative lost the configured barrel input_count");
        helper.assertTrue(stacks.stream().allMatch(stack -> stack.is(ItemTags.LOGS)),
                "JEI count expansion changed the ingredient alternatives");
        helper.succeed();
    }
}
