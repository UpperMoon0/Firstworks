package com.nstut.firstworks.compat.jade;

import com.nstut.firstworks.content.barrel.BarrelBlock;
import com.nstut.firstworks.content.barrel.BarrelBlockEntity;
import com.nstut.firstworks.content.brick_mold.BrickMoldBlock;
import com.nstut.firstworks.content.brick_mold.BrickMoldBlockEntity;
import com.nstut.firstworks.content.charcoal.CharcoalPileBlock;
import com.nstut.firstworks.content.loom.LoomBlock;
import com.nstut.firstworks.content.loom.LoomBlockEntity;
import com.nstut.firstworks.content.mortar.MortarBlock;
import com.nstut.firstworks.content.mortar.MortarBlockEntity;
import net.minecraft.world.level.block.Block;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public final class FirstworksJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(BarrelProgressProvider.INSTANCE, BarrelBlockEntity.class);
        registration.registerBlockDataProvider(LoomProgressProvider.INSTANCE, LoomBlockEntity.class);
        registration.registerBlockDataProvider(BrickMoldProgressProvider.INSTANCE, BrickMoldBlockEntity.class);
        registration.registerBlockDataProvider(MortarProgressProvider.INSTANCE, MortarBlockEntity.class);
        registration.registerBlockDataProvider(CharcoalMoundProvider.INSTANCE, Block.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(BarrelProgressProvider.INSTANCE, BarrelBlock.class);
        registration.registerBlockComponent(LoomProgressProvider.INSTANCE, LoomBlock.class);
        registration.registerBlockComponent(BrickMoldProgressProvider.INSTANCE, BrickMoldBlock.class);
        registration.registerBlockComponent(MortarProgressProvider.INSTANCE, MortarBlock.class);
        registration.registerBlockComponent(CharcoalMoundProvider.INSTANCE, Block.class);
        registration.registerBlockComponent(CharcoalPileProvider.INSTANCE, CharcoalPileBlock.class);
    }
}
