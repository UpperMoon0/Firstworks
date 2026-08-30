package com.nstut.firstworks.content;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;

/** A cutting tool with explicit Firstworks durability rules rather than sword block damage. */
public class KnifeItem extends Item {
    public KnifeItem(Tier tier, float attackDamage, float attackSpeed) {
        super(new Item.Properties()
                .durability(tier.getUses())
                .attributes(SwordItem.createAttributes(tier, attackDamage, attackSpeed)));
    }
}
