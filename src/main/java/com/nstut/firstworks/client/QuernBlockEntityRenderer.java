package com.nstut.firstworks.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.nstut.firstworks.content.quern.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import com.mojang.math.Axis;

public final class QuernBlockEntityRenderer implements BlockEntityRenderer<QuernBlockEntity> {
    public QuernBlockEntityRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public void render(QuernBlockEntity q,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        pose.pushPose(); pose.translate(.5,.42,.5); pose.mulPose(Axis.YP.rotationDegrees(q.getRotation(partial)));
        pose.pushPose(); pose.scale(.72F,.22F,.72F);
        Minecraft.getInstance().getItemRenderer().renderStatic(new ItemStack(Blocks.STONE),ItemDisplayContext.FIXED,light,OverlayTexture.NO_OVERLAY,pose,buffers,q.getLevel(),1);
        pose.popPose();
        if(q.getBlockState().getBlock() instanceof QuernBlock block && block.isRotary()){
            pose.pushPose(); pose.translate(.28,.33,0); pose.mulPose(Axis.ZP.rotationDegrees(-18)); pose.scale(.55F,.55F,.55F);
            Minecraft.getInstance().getItemRenderer().renderStatic(new ItemStack(Items.STICK),ItemDisplayContext.FIXED,light,OverlayTexture.NO_OVERLAY,pose,buffers,q.getLevel(),2); pose.popPose();
        }
        if(!q.getInput().isEmpty()||!q.getOutput().isEmpty()){
            var stack=q.getOutput().isEmpty()?q.getInput():q.getOutput();pose.pushPose();pose.translate(0,.12,0);pose.scale(.45F,.45F,.45F);
            Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED,light,OverlayTexture.NO_OVERLAY,pose,buffers,q.getLevel(),0);pose.popPose();
        }
        pose.popPose();
    }
}
