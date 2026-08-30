package com.nstut.firstworks.content;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TieredItem;

/**
 * A cutting tool with explicit Firstworks durability rules rather than sword block damage.
 *
 * <p>Extends {@link TieredItem} (not {@link SwordItem}) so the knife keeps tier durability,
 * repair material, and enchantability from the {@link Tier}, while ordinary block breaking
 * applies no knife durability (no {@code Tool} component is installed). Combat still costs
 * exactly one durability point per successful hit via {@link #postHurtEnemy}.
 */
public class KnifeItem extends TieredItem {
    public KnifeItem(Tier tier, float attackDamage, float attackSpeed) {
        super(tier, new Item.Properties()
                .attributes(SwordItem.createAttributes(tier, attackDamage, attackSpeed)));
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        return true;
    }

    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        stack.hurtAndBreak(1, attacker, EquipmentSlot.MAINHAND);
    }
}
