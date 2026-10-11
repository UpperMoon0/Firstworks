package com.nstut.firstworks.gametest;

import com.nstut.firstworks.Firstworks;
import com.nstut.firstworks.GameplayEvents;
import com.nstut.firstworks.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import com.mojang.authlib.GameProfile;

@GameTestHolder(Firstworks.MOD_ID)
@PrefixGameTestTemplate(false)
public final class OchreGameTests {
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void actualBreakingReplacesLootAndProtectsCancelledCreativeAndSilk(GameTestHelper h) {
        var level = h.getLevel();
        var dropRule = level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DOBLOCKDROPS);
        boolean originalDrops = dropRule.get();
        dropRule.set(true, level.getServer());
        try {
            var p = new ServerPlayer(level.getServer(), level,
                    new GameProfile(UUID.randomUUID(), "ochre-test"), ClientInformation.createDefault()) {
                @Override public boolean isCreative() { return getAbilities().instabuild; }
            };
            var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
            new io.netty.channel.embedded.EmbeddedChannel(connection);
            p.connection = new net.minecraft.server.network.ServerGamePacketListenerImpl(level.getServer(), connection, p,
                    net.minecraft.server.network.CommonListenerCookie.createInitial(p.getGameProfile(), false));
            p.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
            GameType.SURVIVAL.updatePlayerAbilities(p.getAbilities());
            h.assertTrue(!p.isCreative(), "Fixture unexpectedly creative");
            var pos = h.absolutePos(new BlockPos(3, 2, 3));
            for (var block : List.of(Blocks.CLAY, Blocks.COARSE_DIRT, Blocks.RED_SAND, Blocks.TERRACOTTA,
                    Blocks.STONE)) { // Stone is added by the test datapack, exercising custom tool-gated tags.
                var knife = new ItemStack(ModItems.FLINT_KNIFE.get());
                p.setItemInHand(InteractionHand.MAIN_HAND, knife);
                h.assertTrue(knife.is(com.nstut.firstworks.registry.ModTags.PRIMITIVE_KNIVES), "Knife tag missing");
                h.assertTrue(block.defaultBlockState().is(com.nstut.firstworks.registry.ModTags.OCHRE_SOURCES),
                        "Source tag missing " + block);
                level.setBlock(pos, block.defaultBlockState(), 3);
                h.assertTrue(p.gameMode.destroyBlock(pos), "Break rejected");
                var drops = drops(h, pos);
                h.assertTrue(level.getBlockState(pos).isAir(), "Source not consumed");
                h.assertTrue(drops.size() == 1 && drops.getFirst().getItem().is(ModItems.RAW_OCHRE.get())
                        && drops.getFirst().getItem().getCount() == 1, "Ordinary source loot survived for " + block + ": " + drops.stream().map(e -> e.getItem().toString()).toList());
                h.assertTrue(knife.getDamageValue() == 1, "Knife did not lose exactly one durability");
                drops.forEach(ItemEntity::discard);
            }
            for (int i = 0; i < 12; i++) {
                level.setBlock(pos, Blocks.COARSE_DIRT.defaultBlockState(), 3);
                p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                p.gameMode.destroyBlock(pos);
                var drops = drops(h, pos);
                h.assertTrue(drops.size() == 1 && drops.getFirst().getItem().is(Items.COARSE_DIRT),
                        "Place/break farming added ochre or consumed ordinary drops");
                drops.forEach(ItemEntity::discard);
            }
            var knife = new ItemStack(ModItems.FLINT_KNIFE.get());
            knife.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                    .getOrThrow(Enchantments.SILK_TOUCH), 1);
            p.setItemInHand(InteractionHand.MAIN_HAND, knife);
            level.setBlock(pos, Blocks.CLAY.defaultBlockState(), 3);
            p.gameMode.destroyBlock(pos);
            var silkDrops = drops(h, pos);
            h.assertTrue(silkDrops.size() == 1 && silkDrops.getFirst().getItem().is(Items.CLAY)
                    && knife.getDamageValue() == 0, "Silk Touch no longer preserves the source");
            silkDrops.forEach(ItemEntity::discard);

            knife = new ItemStack(ModItems.FLINT_KNIFE.get());
            p.setItemInHand(InteractionHand.MAIN_HAND, knife);
            level.setBlock(pos, Blocks.CLAY.defaultBlockState(), 3);
            Consumer<BlockEvent.BreakEvent> cancel = e -> {
                if (e.getPos().equals(pos) && e.getPlayer() == p) e.setCanceled(true);
            };
            NeoForge.EVENT_BUS.addListener(cancel);
            try {
                h.assertTrue(!p.gameMode.destroyBlock(pos), "Canceled break succeeded");
                h.assertTrue(level.getBlockState(pos).is(Blocks.CLAY) && drops(h, pos).isEmpty()
                        && knife.getDamageValue() == 0, "Canceled break had side effects");
            } finally { NeoForge.EVENT_BUS.unregister(cancel); }
            var rule = level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DOBLOCKDROPS);
            boolean oldRule = rule.get();
            try {
                rule.set(false, level.getServer());
                p.gameMode.destroyBlock(pos);
                h.assertTrue(drops(h, pos).isEmpty() && knife.getDamageValue() == 0, "doTileDrops bypassed");
            } finally { rule.set(oldRule, level.getServer()); }
            level.setBlock(pos, Blocks.CLAY.defaultBlockState(), 3);
            p.gameMode.changeGameModeForPlayer(GameType.CREATIVE);
            p.gameMode.destroyBlock(pos);
            h.assertTrue(drops(h, pos).isEmpty() && knife.getDamageValue() == 0, "Creative ochre harvest");
            h.succeed();
        } finally { dropRule.set(originalDrops, level.getServer()); }
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void canceledDropEventAndUntaggedBlocksStayUntouched(GameTestHelper h) {
        var p = h.makeMockPlayer(GameType.SURVIVAL);
        var knife = new ItemStack(ModItems.FLINT_KNIFE.get());
        p.setItemInHand(InteractionHand.MAIN_HAND, knife);
        var pos = h.absolutePos(new BlockPos(3, 2, 3));
        var ordinary = new ItemEntity(h.getLevel(), pos.getX(), pos.getY(), pos.getZ(), new ItemStack(Items.CLAY_BALL, 4));
        var list = new ArrayList<ItemEntity>(List.of(ordinary));
        var event = new BlockDropsEvent(h.getLevel(), pos, Blocks.CLAY.defaultBlockState(), null, list, p, knife.copy());
        event.setCanceled(true);
        GameplayEvents.gatherRawOchre(event);
        h.assertTrue(list.size() == 1 && list.getFirst() == ordinary && knife.getDamageValue() == 0,
                "Canceled drop event was modified");
        event = new BlockDropsEvent(h.getLevel(), pos, Blocks.DIRT.defaultBlockState(), null, list, p, knife.copy());
        GameplayEvents.gatherRawOchre(event);
        h.assertTrue(list.getFirst() == ordinary && knife.getDamageValue() == 0, "Untagged block harvested");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void shippedPigmentRecipesAreLoadedAndProduceVanillaDye(GameTestHelper h) {
        var manager = h.getLevel().getRecipeManager();
        var mortar = (com.nstut.firstworks.content.MortarGrindingRecipe) manager
                .byKey(Firstworks.id("grind_ochre")).orElseThrow().value();
        var quern = (com.nstut.firstworks.content.quern.QuernGrindingRecipe) manager
                .byKey(Firstworks.id("quern_ochre")).orElseThrow().value();
        h.assertTrue(mortar.result().is(ModItems.GROUND_OCHRE.get()) && mortar.result().getCount() == 2,
                "Mortar pigment yield");
        h.assertTrue(quern.inputCount() == 4 && quern.result().getCount() == 8 && quern.work() == 60,
                "Quern batch advantage");
        var input = net.minecraft.world.item.crafting.CraftingInput.of(1, 1,
                List.of(new ItemStack(ModItems.GROUND_OCHRE.get())));
        var recipe = manager.getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, input, h.getLevel()).orElseThrow();
        h.assertTrue(recipe.id().equals(Firstworks.id("red_dye_from_ochre"))
                && recipe.value().assemble(input, h.getLevel().registryAccess()).is(Items.RED_DYE),
                "Ground ochre is not a usable vanilla red dye recipe");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void actualMortarAndQuernProcessingConsumeRawAndYieldPigment(GameTestHelper h) {
        var mortarPos = new BlockPos(3, 1, 3);
        var quernPos = new BlockPos(6, 1, 3);
        h.setBlock(mortarPos, com.nstut.firstworks.registry.ModBlocks.MORTAR_AND_PESTLE.get());
        h.setBlock(quernPos, com.nstut.firstworks.registry.ModBlocks.QUERN.get());
        com.nstut.firstworks.content.mortar.MortarBlockEntity mortar = h.getBlockEntity(mortarPos);
        com.nstut.firstworks.content.quern.QuernBlockEntity quern = h.getBlockEntity(quernPos);
        var p = h.makeMockPlayer(GameType.SURVIVAL);
        var absolute = h.absolutePos(mortarPos);
        p.moveTo(absolute.getX() + 0.5, absolute.getY() + 0.65, absolute.getZ() + 0.5, 0, 90);
        p.setXRot(90);
        var raw = new ItemStack(ModItems.RAW_OCHRE.get());
        h.assertTrue(mortar.insert(raw, false) && raw.isEmpty(), "Mortar did not consume loaded raw");
        h.assertTrue(mortar.operate(p, "crush"), "First crush failed");
        var batch = new ItemStack(ModItems.RAW_OCHRE.get(), 4);
        for (int i = 0; i < 4; i++) quern.insert(batch, false);
        for (int i = 0; i < 12; i++) h.assertTrue(quern.work(), "Quern crank failed");
        h.assertTrue(batch.isEmpty() && quern.getInput().isEmpty()
                && quern.getOutput().is(ModItems.GROUND_OCHRE.get())
                && quern.getOutput().getCount() == 8, "Quern consumption or yield failed");
        h.runAtTickTime(5, () -> {
            h.assertTrue(mortar.operate(p, "crush"), "Second crush failed");
            p.moveTo(absolute.getX() + 0.74, absolute.getY() + 0.65, absolute.getZ() + 0.5, 0, 90);
            p.setXRot(90);
        });
        for (int tick = 6; tick < 54; tick++) {
            h.runAtTickTime(tick, () -> h.assertTrue(mortar.operate(p, "grind"), "Held grinding failed"));
        }
        h.runAtTickTime(54, () -> {
            h.assertTrue(mortar.getInput().isEmpty() && mortar.getOutput().is(ModItems.GROUND_OCHRE.get())
                    && mortar.getOutput().getCount() == 2, "Mortar consumption or yield failed");
            h.succeed();
        });
    }

    private static List<ItemEntity> drops(GameTestHelper h, BlockPos pos) {
        return h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(0.5));
    }
}
