package com.nstut.firstworks.registry;

import com.nstut.firstworks.Firstworks;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.EntityType;

/** Public integration tags for Firstworks tool roles. */
public final class ModTags {
    public static final TagKey<Item> PRIMITIVE_KNIVES = TagKey.create(
            Registries.ITEM, Firstworks.id("primitive_knives"));
    public static final TagKey<Item> PRIMITIVE_BINDINGS = TagKey.create(
            Registries.ITEM, Firstworks.id("primitive_bindings"));
    public static final TagKey<Item> STRONG_BINDINGS = TagKey.create(
            Registries.ITEM, Firstworks.id("strong_bindings"));
    public static final TagKey<Block> CHARCOAL_SEALANTS = TagKey.create(
            Registries.BLOCK, Firstworks.id("charcoal_sealants"));
    public static final TagKey<Block> CHARCOAL_WOODS = TagKey.create(
            Registries.BLOCK, Firstworks.id("charcoal_woods"));
    public static final TagKey<Item> CHARCOAL_IGNITERS = TagKey.create(
            Registries.ITEM, Firstworks.id("charcoal_igniters"));
    public static final TagKey<EntityType<?>> DROPS_BONES = TagKey.create(
            Registries.ENTITY_TYPE, Firstworks.id("drops_bones"));
    public static final TagKey<EntityType<?>> NO_BONE_DROPS = TagKey.create(
            Registries.ENTITY_TYPE, Firstworks.id("no_bone_drops"));
    public static final TagKey<EntityType<?>> LEATHER_DROPS_AS_RAW_HIDE = TagKey.create(
            Registries.ENTITY_TYPE, Firstworks.id("leather_drops_as_raw_hide"));
    public static final TagKey<EntityType<?>> NO_RAW_HIDE_DROPS = TagKey.create(
            Registries.ENTITY_TYPE, Firstworks.id("no_raw_hide_drops"));
    public static final TagKey<Block> BARRELS_BLOCK = TagKey.create(
            Registries.BLOCK, Firstworks.id("barrels"));
    public static final TagKey<Item> BARRELS_ITEM = TagKey.create(
            Registries.ITEM, Firstworks.id("barrels"));
    public static final TagKey<Block> LOOMS_BLOCK = TagKey.create(
            Registries.BLOCK, Firstworks.id("looms"));
    public static final TagKey<Item> LOOMS_ITEM = TagKey.create(
            Registries.ITEM, Firstworks.id("looms"));
    public static final TagKey<Item> RAW_HIDES = TagKey.create(
            Registries.ITEM, Firstworks.id("raw_hides"));
    public static final TagKey<Block> OCHRE_SOURCES = TagKey.create(
            Registries.BLOCK, Firstworks.id("ochre_sources"));
    public static final TagKey<Block> PLANT_FIBRE_SOURCES = TagKey.create(
            Registries.BLOCK, Firstworks.id("plant_fibre_sources"));
    public static final TagKey<Block> DOUBLE_PLANT_FIBRE_SOURCES = TagKey.create(
            Registries.BLOCK, Firstworks.id("double_plant_fibre_sources"));

    private ModTags() {}
}
