package com.nstut.firstworks.client;

import com.nstut.firstworks.Firstworks;
import com.nstut.firstworks.content.mortar.MortarBlock;
import com.nstut.firstworks.content.mortar.MortarBlockEntity;
import com.nstut.firstworks.content.mortar.MortarInputPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = Firstworks.MOD_ID, value = Dist.CLIENT)
public final class MortarInput {
    private static BlockPos previous;
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        BlockPos current = null;
        if (mc.player != null && mc.level != null && mc.screen == null && !mc.isPaused()
                && mc.options.keyUse.isDown() && mc.player.getMainHandItem().isEmpty() && !mc.player.isShiftKeyDown()
                && mc.hitResult instanceof BlockHitResult hit
                && mc.level.getBlockEntity(hit.getBlockPos()) instanceof MortarBlockEntity
                && MortarBlock.actionAt(hit.getBlockPos(), hit).equals("grind")) current = hit.getBlockPos();
        if (previous != null && !previous.equals(current) && mc.getConnection() != null)
            PacketDistributor.sendToServer(new MortarInputPayload(previous, false));
        if (current != null) PacketDistributor.sendToServer(new MortarInputPayload(current, true));
        previous = current;
    }
    private MortarInput() {}
}
